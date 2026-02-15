package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.extractor.ToolArgumentsExtractor;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import com.dango.aicodegenerate.model.message.ToolRequestMessage;
import com.dango.aicodegenerate.service.AiCodeModifierService;
import com.dango.dangoaicodeapp.ai.AiCodeModifierServiceFactory;
import com.dango.dangoaicodeapp.model.entity.ElementInfo;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码修改节点
 * 使用修改专用提示词进行增量修改
 * 
 * 工具调用策略：
 * - 目录读取工具：获取项目结构
 * - 文件读取工具：读取指定文件内容
 * - 文件修改工具：修改现有文件的部分内容
 * - 文件写入工具：创建新文件或完全重写文件
 * - 文件删除工具：删除不需要的文件
 * - 图片搜索工具：搜索替换图片素材
 *
 * @author dango
 */
@Slf4j
@Component
public class CodeModifierNode {

    private static final String NODE_NAME = "代码修改";

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析修改需求...\n");

            try {
                // 检查是否有 SQL 执行失败
                if (context.isDatabaseEnabled() && context.hasSqlExecutionFailure()) {
                    log.warn("检测到 SQL 执行失败，跳过代码修改");
                    context.emitNodeMessage(NODE_NAME, "检测到数据库操作失败，跳过代码修改\n");
                    context.setErrorMessage("数据库操作失败，无法进行代码修改");
                    context.emitNodeComplete(NODE_NAME);
                    context.setCurrentStep(NODE_NAME);
                    return WorkflowContext.saveContext(context);
                }

                // 恢复监控上下文到当前线程（用于跨线程传递监控信息）
                context.restoreMonitorContext();

                // 构建修改请求（包含项目结构、元素信息和用户修改要求）
                String modifyRequest = buildModifyRequest(context);
                
                log.info("修改请求构建完成:\n{}", modifyRequest);
                context.emitNodeMessage(NODE_NAME, "修改请求已构建，正在调用 AI 服务...\n");

                Long appId = context.getAppId();
                CodeGenTypeEnum generationType = context.getGenerationType();
                
                // 如果没有设置代码生成类型，尝试从现有项目推断
                if (generationType == null) {
                    generationType = inferGenerationType(appId);
                    context.setGenerationType(generationType);
                }

                // 获取修改专用 AI 服务（配置了文件操作工具）
                AiCodeModifierServiceFactory modifierServiceFactory = SpringContextUtil.getBean(AiCodeModifierServiceFactory.class);
                AiCodeModifierService modifierService = modifierServiceFactory.getModifierService(appId, generationType);
                
                // 根据代码生成类型选择对应的修改方法
                TokenStream tokenStream = switch (generationType) {
                    case HTML -> modifierService.modifyHtmlCodeStream(appId, modifyRequest);
                    case MULTI_FILE -> modifierService.modifyMultiFileCodeStream(appId, modifyRequest);
                    case VUE_PROJECT -> modifierService.modifyVueProjectCodeStream(appId, modifyRequest);
                };
                
                // 使用 CountDownLatch 等待流式生成完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                // 为每个工具调用维护一个 extractor
                Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();

                // 订阅 TokenStream，实时输出到前端
                // 注意：必须注册所有回调，包括工具调用相关的回调，否则会导致 NPE
                tokenStream
                        .onPartialResponse(partialResponse -> {
                            // 实时输出代码片段到前端（包装为 JSON 格式）
                            AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                            context.emit(JSONUtil.toJsonStr(aiResponseMessage));
                        })
                        .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                            String toolId = toolExecutionRequest.id();
                            String toolName = toolExecutionRequest.name();
                            String delta = toolExecutionRequest.arguments();

                            ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                                toolId,
                                id -> new ToolArgumentsExtractor(id, toolName)
                            );

                            List<StreamMessage> messages = extractor.process(delta);
                            for (StreamMessage msg : messages) {
                                context.emit(JSONUtil.toJsonStr(msg));
                            }
                        })
                        .onToolExecuted(toolExecution -> {
                            // 处理工具执行结果
                            ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                            context.emit(JSONUtil.toJsonStr(toolExecutedMessage));
                        })
                        .onCompleteResponse(response -> {
                            log.info("代码修改完成");
                            latch.countDown();
                        })
                        .onError(error -> {
                            log.error("代码修改失败: {}", error.getMessage(), error);
                            errorRef.set(error);
                            latch.countDown();
                        })
                        .start();
                
                // 等待流式生成完成
                latch.await();
                
                // 检查是否有错误
                if (errorRef.get() != null) {
                    throw new RuntimeException(errorRef.get());
                }
                
                // 构建生成的代码目录路径
                String generatedCodeDir = buildGeneratedCodeDir(generationType, appId);
                context.setGeneratedCodeDir(generatedCodeDir);

                context.emitNodeMessage(NODE_NAME, "\n代码修改完成\n");

            } catch (Exception e) {
                log.error("代码修改失败: {}", e.getMessage(), e);
                context.setErrorMessage("代码修改失败: " + e.getMessage());
                context.emitNodeError(NODE_NAME, e.getMessage());
            } finally {
                // 清除当前线程的监控上下文
                context.clearMonitorContext();
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
    
    /**
     * 根据 appId 推断代码生成类型
     * 检查现有项目目录来确定类型
     */
    private static CodeGenTypeEnum inferGenerationType(Long appId) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        
        // 按优先级检查各种类型的目录
        if (new File(baseDir, "vue_project_" + appId).exists()) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        if (new File(baseDir, "multi_file_" + appId).exists()) {
            return CodeGenTypeEnum.MULTI_FILE;
        }
        if (new File(baseDir, "html_" + appId).exists()) {
            return CodeGenTypeEnum.HTML;
        }
        
        // 默认返回 HTML 类型
        return CodeGenTypeEnum.HTML;
    }
    
    /**
     * 构建生成的代码目录路径
     */
    private static String buildGeneratedCodeDir(CodeGenTypeEnum generationType, Long appId) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        String dirName = generationType.getValue() + "_" + appId;
        return baseDir + File.separator + dirName;
    }

    /**
     * 构建修改请求
     * 包含：修改指导（优先） + 项目结构 + 数据库信息 + 元素信息 + 用户修改要求
     *
     * 注意：不传递完整代码，由 AI 自行决定读取哪些文件
     *
     * @param context 工作流上下文
     * @return 修改请求字符串
     */
    public static String buildModifyRequest(WorkflowContext context) {
        StringBuilder request = new StringBuilder();

        // 1. 优先添加修改指导（如果有）- 放在最前面
        List<com.dango.aicodegenerate.model.FileModificationGuide> guides = context.getFileModificationGuides();
        if (guides != null && !guides.isEmpty()) {
            // 先输出修改清单到前端
            context.emitNodeMessage(NODE_NAME,
                String.format("\n📋 修改清单（共 %d 个文件）：\n", guides.size()));

            for (int i = 0; i < guides.size(); i++) {
                com.dango.aicodegenerate.model.FileModificationGuide guide = guides.get(i);
                context.emitNodeMessage(NODE_NAME,
                    String.format("  %d. %s (%s)\n", i+1, guide.getPath(), guide.getType()));
            }

            context.emitNodeMessage(NODE_NAME, "\n开始执行修改...\n\n");

            // 构建修改指导部分（给 AI 看的）
            request.append("🚨 修改指导（必须严格按照以下顺序执行）\n\n");
            request.append("**重要**：ModificationPlanner 已分析项目并制定了详细的修改计划。\n");
            request.append(String.format("你需要修改 %d 个文件，请在修改每个文件前输出进度信息：\n", guides.size()));
            request.append("📝 正在修改 [序号]/").append(guides.size()).append(": [文件路径]\n\n");

            for (int i = 0; i < guides.size(); i++) {
                com.dango.aicodegenerate.model.FileModificationGuide guide = guides.get(i);
                request.append(String.format("### 文件 %d/%d: %s (%s)\n",
                    i + 1, guides.size(), guide.getPath(), guide.getType()));

                if (StrUtil.isNotBlank(guide.getReason())) {
                    request.append("**修改原因**: ").append(guide.getReason()).append("\n\n");
                }

                request.append("**操作步骤**:\n");
                List<String> operations = guide.getOperations();
                if (operations != null && !operations.isEmpty()) {
                    for (String operation : operations) {
                        request.append("- ").append(operation).append("\n");
                    }
                } else {
                    request.append("- （无具体操作步骤）\n");
                }
                request.append("\n");
            }

            request.append("---\n\n");
        }

        // 2. 添加项目结构（而非完整代码）
        String projectStructure = context.getProjectStructure();
        if (StrUtil.isNotBlank(projectStructure)) {
            request.append("## 项目结构\n```\n")
                   .append(projectStructure)
                   .append("```\n\n");
        }

        // 3. 添加数据库信息（如果启用了数据库）
        if (context.isDatabaseEnabled()) {
            request.append("## 数据库信息\n");
            request.append("Schema: app_").append(context.getAppId()).append("\n\n");

            // 优先使用最新的 Schema（SQL 执行后的）
            String databaseSchema = context.getLatestDatabaseSchema();
            if (StrUtil.isBlank(databaseSchema)) {
                databaseSchema = context.getDatabaseSchema();
            }

            if (StrUtil.isNotBlank(databaseSchema)) {
                request.append("### 表结构\n```\n")
                       .append(databaseSchema)
                       .append("```\n\n");
            }

            // 添加 Supabase 客户端配置说明
            request.append("### Supabase 客户端配置\n")
                   .append("**重要**：Supabase 客户端已由系统自动配置，位于 `src/integrations/supabase/client.js`\n\n")
                   .append("**你只能使用这个客户端，绝对不能修改它**\n\n")
                   .append("**正确的使用方式**：\n")
                   .append("```javascript\n")
                   .append("// 1. 导入客户端\n")
                   .append("import { supabase } from '@/integrations/supabase/client'\n\n")
                   .append("// 2. 使用客户端进行数据库操作\n")
                   .append("// 查询\n")
                   .append("const { data, error } = await supabase.from('表名').select('*')\n\n")
                   .append("// 插入\n")
                   .append("const { data, error } = await supabase.from('表名').insert({ 字段: 值 })\n\n")
                   .append("// 更新\n")
                   .append("const { data, error } = await supabase.from('表名').update({ 字段: 新值 }).eq('id', id)\n\n")
                   .append("// 删除\n")
                   .append("const { error } = await supabase.from('表名').delete().eq('id', id)\n")
                   .append("```\n\n");
        }

        // 4. 添加元素信息（如果有）
        ElementInfo elementInfo = context.getElementInfo();
        if (elementInfo != null) {
            request.append("## 选中元素信息\n")
                   .append(formatElementInfo(elementInfo))
                   .append("\n");
        }

        // 5. 添加用户修改要求
        String modifyRequirement = buildModifyRequirement(context);
        if (StrUtil.isNotBlank(modifyRequirement)) {
            request.append("## 修改要求\n")
                   .append(modifyRequirement)
                   .append("\n\n");
        }

        // 6. 添加工具使用提示
        request.append("## 操作指南\n")
               .append("1. 如果有修改指导，请严格按照指导中的步骤执行\n")
               .append("2. 使用【文件读取工具】查看需要修改的文件内容\n")
               .append("3. 根据修改要求，使用对应的工具进行修改：\n")
               .append("   - 【文件修改工具】：修改现有文件的部分内容\n")
               .append("   - 【文件写入工具】：创建新文件或完全重写文件\n")
               .append("   - 【文件删除工具】：删除不需要的文件\n")
               .append("   - 【图片搜索工具】：搜索替换图片素材\n");

        // 7. 添加重要约束
        request.append("\n## 重要约束\n");

        if (context.isDatabaseEnabled()) {
            request.append("### 数据库客户端保护\n")
                   .append("- **绝对禁止修改** `src/integrations/supabase/` 目录下的任何文件\n")
                   .append("- **绝对禁止读取** `src/integrations/supabase/client.js` 文件（不需要查看，直接使用即可）\n")
                   .append("- **绝对禁止创建** 新的 Supabase 客户端配置文件\n")
                   .append("- **只能使用** 已有的客户端：`import { supabase } from '@/integrations/supabase/client'`\n")
                   .append("- 原因：客户端配置包含正确的 URL、Key 和 Schema，由系统自动生成和管理\n\n");
        }

        request.append("### 其他约束\n")
               .append("- **禁止创建** `.sql` 文件（数据库操作已由系统完成）\n")
               .append("- **禁止创建** `.md` 文件（不需要文档）\n")
               .append("- **禁止创建** 测试页面或调试页面（如 DatabaseTest.vue）\n")
               .append("- **只修改业务代码**，让应用使用数据库存储数据，用户不应感知到数据库的存在\n");

        return request.toString();
    }

    /**
     * 构建修改要求
     * 针对数据库初始化场景，重写修改要求
     *
     * @param context 工作流上下文
     * @return 修改要求字符串
     */
    private static String buildModifyRequirement(WorkflowContext context) {
        // 如果是数据库初始化场景（有成功执行的 SQL），重写修改要求
        if (context.isDatabaseEnabled() && context.hasSqlExecutionSuccess()) {
            return "数据库表已创建完成，请修改前端代码，将原有的本地存储（localStorage/内存）改为使用 Supabase 数据库存储。\n"
                    + "具体要求：\n"
                    + "1. 找到应用中存储数据的地方（如文章、用户信息等）\n"
                    + "2. 将本地存储改为调用 Supabase API 进行增删改查\n"
                    + "3. 保持原有的用户界面和交互逻辑不变\n"
                    + "4. 用户不应感知到数据库的存在，只是数据能够持久化保存";
        }

        // 否则使用原始的修改要求
        return context.getOriginalPrompt();
    }

    /**
     * 格式化元素信息
     * 将 ElementInfo 对象转换为易读的字符串格式
     *
     * @param elementInfo 元素信息对象
     * @return 格式化后的字符串
     */
    public static String formatElementInfo(ElementInfo elementInfo) {
        if (elementInfo == null) {
            return "";
        }

        StringBuilder info = new StringBuilder();

        // 标签名（转小写以符合 HTML 规范）
        if (StrUtil.isNotBlank(elementInfo.getTagName())) {
            info.append("- 标签: ").append(elementInfo.getTagName().toLowerCase()).append("\n");
        }

        // CSS 选择器
        if (StrUtil.isNotBlank(elementInfo.getSelector())) {
            info.append("- 选择器: ").append(elementInfo.getSelector()).append("\n");
        }

        // 元素 ID
        if (StrUtil.isNotBlank(elementInfo.getId())) {
            info.append("- ID: ").append(elementInfo.getId()).append("\n");
        }

        // 元素类名
        if (StrUtil.isNotBlank(elementInfo.getClassName())) {
            info.append("- 类名: ").append(elementInfo.getClassName()).append("\n");
        }

        // 文本内容（截断过长的内容）
        if (StrUtil.isNotBlank(elementInfo.getTextContent())) {
            String textContent = elementInfo.getTextContent().trim();
            if (textContent.length() > 100) {
                textContent = textContent.substring(0, 100) + "...";
            }
            info.append("- 当前内容: ").append(textContent).append("\n");
        }

        // 页面路径
        if (StrUtil.isNotBlank(elementInfo.getPagePath())) {
            info.append("- 页面路径: ").append(elementInfo.getPagePath()).append("\n");
        }

        return info.toString();
    }
}

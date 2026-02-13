package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
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
                
                // 订阅 TokenStream，实时输出到前端
                // 注意：必须注册所有回调，包括工具调用相关的回调，否则会导致 NPE
                tokenStream
                        .onPartialResponse(partialResponse -> {
                            // 实时输出代码片段到前端（包装为 JSON 格式）
                            AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                            context.emit(JSONUtil.toJsonStr(aiResponseMessage));
                        })
                        .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                            // 处理工具调用请求（流式输出工具调用信息）
                            ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                            context.emit(JSONUtil.toJsonStr(toolRequestMessage));
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
     * 包含：项目结构 + 数据库信息 + 元素信息 + 用户修改要求
     *
     * 注意：不传递完整代码，由 AI 自行决定读取哪些文件
     *
     * @param context 工作流上下文
     * @return 修改请求字符串
     */
    public static String buildModifyRequest(WorkflowContext context) {
        StringBuilder request = new StringBuilder();

        // 添加项目结构（而非完整代码）
        String projectStructure = context.getProjectStructure();
        if (StrUtil.isNotBlank(projectStructure)) {
            request.append("## 项目结构\n```\n")
                   .append(projectStructure)
                   .append("```\n\n");
        }

        // 添加数据库信息（如果启用了数据库）
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

            // 添加 SQL 执行结果（如果有）
            if (context.getExecutionResults() != null && !context.getExecutionResults().isEmpty()) {
                request.append("### 已执行的 SQL 操作\n");
                context.getExecutionResults().forEach(result -> {
                    String status = result.isSuccess() ? "✓" : "✗";
                    request.append("- ").append(status).append(" ")
                           .append(result.getSql().trim().split("\n")[0]);  // 只显示第一行
                    if (!result.isSuccess() && StrUtil.isNotBlank(result.getError())) {
                        request.append(" (错误: ").append(result.getError()).append(")");
                    }
                    request.append("\n");
                });
                request.append("\n");
            }

            // 添加 Supabase 客户端使用规范
            request.append("### Supabase 客户端使用规范\n")
                   .append("- 客户端配置文件: `src/integrations/supabase/client.js`\n")
                   .append("- 导入方式: `import { supabase } from '@/integrations/supabase/client'`\n")
                   .append("- 查询示例: `const { data, error } = await supabase.from('表名').select('*')`\n")
                   .append("- 插入示例: `const { data, error } = await supabase.from('表名').insert({ ... })`\n")
                   .append("- 更新示例: `const { data, error } = await supabase.from('表名').update({ ... }).eq('id', id)`\n")
                   .append("- 删除示例: `const { error } = await supabase.from('表名').delete().eq('id', id)`\n\n");
        }

        // 添加元素信息
        ElementInfo elementInfo = context.getElementInfo();
        if (elementInfo != null) {
            request.append("## 选中元素信息\n")
                   .append(formatElementInfo(elementInfo))
                   .append("\n");
        }

        // 添加用户修改要求
        String originalPrompt = context.getOriginalPrompt();
        if (StrUtil.isNotBlank(originalPrompt)) {
            request.append("## 修改要求\n")
                   .append(originalPrompt)
                   .append("\n\n");
        }

        // 添加工具使用提示
        request.append("## 操作指南\n")
               .append("1. 使用【文件读取工具】查看需要修改的文件内容\n")
               .append("2. 根据修改要求，使用对应的工具进行修改：\n")
               .append("   - 【文件修改工具】：修改现有文件的部分内容\n")
               .append("   - 【文件写入工具】：创建新文件或完全重写文件\n")
               .append("   - 【文件删除工具】：删除不需要的文件\n")
               .append("   - 【图片搜索工具】：搜索替换图片素材\n");

        return request.toString();
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

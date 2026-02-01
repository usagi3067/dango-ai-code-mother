package com.dango.dangoaicodeapp.workflow.node;

import com.dango.dangoaicodeapp.core.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点
 * 对 Vue 项目执行 npm install 和 npm run build
 */
@Slf4j
@Component
public class ProjectBuilderNode {

    private static final String NODE_NAME = "项目构建";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);

            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum generationType = context.getGenerationType();
            String buildResultDir;

            // Vue 项目类型：使用 VueProjectBuilder 进行构建
            if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
                context.emitNodeMessage(NODE_NAME, "检测到 Vue 项目，开始执行构建...\n");
                context.emitNodeMessage(NODE_NAME, "执行 npm install...\n");
                
                try {
                    VueProjectBuilder vueProjectBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    // 执行 Vue 项目构建（npm install + npm run build）
                    boolean buildSuccess = vueProjectBuilder.buildProject(generatedCodeDir);
                    if (buildSuccess) {
                        // 构建成功，返回 dist 目录路径
                        buildResultDir = generatedCodeDir + File.separator + "dist";
                        log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                        context.emitNodeMessage(NODE_NAME, "npm run build 执行成功\n");
                        context.emitNodeMessage(NODE_NAME, 
                                String.format("构建产物目录: %s\n", buildResultDir));
                    } else {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                    }
                } catch (BusinessException e) {
                    context.emitNodeError(NODE_NAME, e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                    context.setErrorMessage("Vue 项目构建异常: " + e.getMessage());
                    context.emitNodeError(NODE_NAME, e.getMessage());
                    buildResultDir = generatedCodeDir; // 异常时返回原路径
                }
            } else {
                // HTML 和 MULTI_FILE 代码生成时已经保存了，直接使用生成的代码目录
                context.emitNodeMessage(NODE_NAME, 
                        String.format("%s 类型无需额外构建，跳过构建步骤\n", generationType.getText()));
                buildResultDir = generatedCodeDir;
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}

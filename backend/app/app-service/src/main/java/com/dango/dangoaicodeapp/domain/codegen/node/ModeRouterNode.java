package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.model.enums.OperationModeEnum;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 模式路由节点
 * 工作流入口，根据请求内容判断操作模式（创建/修改）
 *
 * 判断逻辑：
 * 1. 如果 context 中有 elementInfo，则为修改模式
 * 2. 如果应用没有历史代码，则为创建模式
 * 3. 否则默认为创建模式
 *
 * @author dango
 */
@Slf4j
@Component
public class ModeRouterNode {

    private static final String NODE_NAME = "模式路由";

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析操作模式...\n");

            // 判断操作模式（elementInfo 已由 Controller 层解析并设置到 context）
            OperationModeEnum mode = determineOperationMode(context);
            context.setOperationMode(mode);

            log.info("操作模式判断完成: {} ({})", mode.getValue(), mode.getText());
            context.emitNodeMessage(NODE_NAME,
                    String.format("已确定操作模式: %s (%s)\n", mode.getText(), mode.getValue()));

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 判断操作模式
     *
     * 判断逻辑：
     * 1. 如果 context 中有 elementInfo，则为修改模式
     * 2. 如果应用有历史代码，则为修改模式
     * 3. 如果应用没有历史代码，则为创建模式
     *
     * @param context 工作流上下文
     * @return 操作模式枚举
     */
    public static OperationModeEnum determineOperationMode(WorkflowContext context) {
        // 1. 检查是否有元素信息（由 Controller 层设置）
        if (context.getElementInfo() != null) {
            log.info("检测到 elementInfo，使用修改模式");
            return OperationModeEnum.MODIFY;
        }

        // 2. 检查是否有历史代码：有则修改，无则创建
        if (hasExistingCode(context.getAppId())) {
            log.info("检测到历史代码，使用修改模式");
            return OperationModeEnum.MODIFY;
        }

        log.info("未检测到历史代码，使用创建模式");
        return OperationModeEnum.CREATE;
    }

    /**
     * 检查应用是否有现有代码
     * 检查 vue_project_appId 目录是否存在
     *
     * @param appId 应用 ID
     * @return 是否存在现有代码
     */
    public static boolean hasExistingCode(Long appId) {
        if (appId == null || appId <= 0) {
            return false;
        }

        String baseDir = System.getProperty("user.dir") + "/tmp/code_output";
        Path codePath = Path.of(baseDir, "vue_project_" + appId);
        if (Files.exists(codePath)) {
            log.debug("找到现有代码目录: {}", codePath);
            return true;
        }

        return false;
    }

    /**
     * 路由条件边
     * 返回下一个节点的名称
     *
     * @param state 消息状态
     * @return 下一个节点名称（"create" 或 "modify"）
     */
    public static String routeToNextNode(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        OperationModeEnum mode = context.getOperationMode();

        if (mode == null) {
            log.warn("操作模式为空，默认路由到创建模式");
            return "create";
        }

        return switch (mode) {
            case CREATE -> "create";
            case MODIFY -> "modify";
            case FIX -> "create"; // FIX 模式暂时路由到创建模式
        };
    }
}

package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.OperationModeEnum;
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
 * 1. 如果 context 中有 elementInfo 或历史代码，则为已有代码模式（后续由意图识别分流）
 * 2. 如果应用没有历史代码，则为创建模式
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
     * 1. 如果 context 中有 elementInfo，则为已有代码模式
     * 2. 如果应用有历史代码，则为已有代码模式
     * 3. 如果应用没有历史代码，则为创建模式
     *
     * @param context 工作流上下文
     * @return 操作模式枚举
     */
    public static OperationModeEnum determineOperationMode(WorkflowContext context) {
        // 1. 检查是否有元素信息（由 Controller 层设置）
        if (context.getElementInfo() != null) {
            log.info("检测到 elementInfo，使用已有代码模式");
            return OperationModeEnum.EXISTING_CODE;
        }

        // 2. 检查是否有历史代码：有则进入已有代码子图（意图识别后再分流修改/问答）
        if (hasExistingCode(context.getAppId(), context.getGenerationType())) {
            log.info("检测到历史代码，使用已有代码模式");
            return OperationModeEnum.EXISTING_CODE;
        }

        log.info("未检测到历史代码，使用创建模式");
        return OperationModeEnum.CREATE;
    }

    /**
     * 检查应用是否有现有代码
     * 根据 generationType 检查对应的项目目录是否存在
     *
     * @param appId 应用 ID
     * @param generationType 代码生成类型
     * @return 是否存在现有代码
     */
    public static boolean hasExistingCode(Long appId, CodeGenTypeEnum generationType) {
        if (appId == null || appId <= 0) {
            return false;
        }

        String baseDir = System.getProperty("user.dir") + "/tmp/code_output";
        String dirName = (generationType != null ? generationType.getValue() : "vue_project") + "_" + appId;
        Path codePath = Path.of(baseDir, dirName);
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
     * 路由逻辑：
     * - CREATE 模式：根据 generationType 进一步区分
     *   - LEETCODE_PROJECT → "leetcode_create"
     *   - INTERVIEW_PROJECT → "interview_create"
     *   - 其他 → "create"
     * - EXISTING_CODE 模式 → "existing_code"
     * - MODIFY 模式 → "modify"
     *
     * @param state 消息状态
     * @return 下一个节点名称
     */
    public static String routeToNextNode(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        OperationModeEnum mode = context.getOperationMode();

        if (mode == null) {
            log.warn("操作模式为空，默认路由到创建模式");
            return "create";
        }

        return switch (mode) {
            case CREATE -> {
                if (context.getGenerationType() == CodeGenTypeEnum.LEETCODE_PROJECT) {
                    log.info("力扣题解类型，路由到力扣创建子图");
                    yield "leetcode_create";
                }
                if (context.getGenerationType() == CodeGenTypeEnum.INTERVIEW_PROJECT) {
                    log.info("面试题解类型，路由到面试创建子图");
                    yield "interview_create";
                }
                yield "create";
            }
            case EXISTING_CODE -> {
                log.info("已有代码，路由到已有代码子图（意图识别）");
                yield "existing_code";
            }
            case MODIFY -> "modify";
            case FIX -> "create"; // FIX 模式暂时路由到创建模式
            case QA -> "existing_code"; // QA 模式也路由到已有代码子图
        };
    }
}

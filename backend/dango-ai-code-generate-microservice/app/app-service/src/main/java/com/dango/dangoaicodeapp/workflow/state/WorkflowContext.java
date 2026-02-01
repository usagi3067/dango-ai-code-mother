package com.dango.dangoaicodeapp.workflow.state;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.FluxSink;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流上下文 - 存储所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * WorkflowContext 在 MessagesState 中的存储 key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 全局 FluxSink 注册表，使用 appId 作为 key
     * 解决 langgraph4j 状态传递时 transient 字段丢失的问题
     */
    private static final ConcurrentHashMap<String, FluxSink<String>> SINK_REGISTRY = new ConcurrentHashMap<>();

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 应用 ID（默认值为 0L）
     */
    private Long appId = 0L;

    /**
     * 工作流执行 ID（用于关联 FluxSink）
     */
    private String workflowExecutionId;

    /**
     * 图片资源字符串
     */
    private String imageListStr;

    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;

    /**
     * 图片收集计划（用于并发收集）
     */
    private ImageCollectionPlan imageCollectionPlan;

    /**
     * 并发图片收集的中间结果字段 - 内容图片
     */
    private List<ImageResource> contentImages;

    /**
     * 并发图片收集的中间结果字段 - 插画图片
     */
    private List<ImageResource> illustrations;

    /**
     * 并发图片收集的中间结果字段 - 架构图
     */
    private List<ImageResource> diagrams;

    /**
     * 并发图片收集的中间结果字段 - Logo
     */
    private List<ImageResource> logos;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum generationType;

    /**
     * 生成的代码目录
     */
    private String generatedCodeDir;

    /**
     * 构建成功的目录
     */
    private String buildResultDir;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 质量检查结果
     */
    private QualityResult qualityResult;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }

    // ========== FluxSink 注册表操作 ==========

    /**
     * 注册 FluxSink 到全局注册表
     */
    public static void registerSink(String executionId, FluxSink<String> sink) {
        SINK_REGISTRY.put(executionId, sink);
    }

    /**
     * 从全局注册表移除 FluxSink
     */
    public static void unregisterSink(String executionId) {
        SINK_REGISTRY.remove(executionId);
    }

    /**
     * 从全局注册表获取 FluxSink
     */
    private FluxSink<String> getSink() {
        if (workflowExecutionId != null) {
            return SINK_REGISTRY.get(workflowExecutionId);
        }
        return null;
    }

    // ========== 流式输出方法 ==========

    /**
     * 发送消息到流（如果 sink 存在）
     */
    public void emit(String message) {
        FluxSink<String> sink = getSink();
        if (sink != null) {
            sink.next(message);
        }
    }

    /**
     * 发送带前缀的节点消息
     */
    public void emitNodeMessage(String nodeName, String message) {
        emit(String.format("[%s] %s", nodeName, message));
    }

    /**
     * 发送节点开始消息
     */
    public void emitNodeStart(String nodeName) {
        emitNodeMessage(nodeName, "开始执行...\n");
    }

    /**
     * 发送节点完成消息
     */
    public void emitNodeComplete(String nodeName) {
        emitNodeMessage(nodeName, "执行完成\n");
    }

    /**
     * 发送节点错误消息
     */
    public void emitNodeError(String nodeName, String error) {
        emitNodeMessage(nodeName, "执行失败: " + error + "\n");
    }
}

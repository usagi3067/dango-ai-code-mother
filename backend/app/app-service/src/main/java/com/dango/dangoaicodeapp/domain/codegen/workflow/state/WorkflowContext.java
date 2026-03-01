package com.dango.dangoaicodeapp.domain.codegen.workflow.state;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.FileModificationGuide;
import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.aicodegenerate.model.ModificationPlanResult;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.aicodegenerate.model.SqlStatementItem;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.OperationModeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 流式输出端口（由基础设施层在启动时注入）。
     */
    private static volatile WorkflowStreamPort workflowStreamPort;

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

    // ========== 创建/修改分离相关字段 ==========

    /**
     * 最大修复重试次数
     */
    public static final int MAX_FIX_RETRY_COUNT = 3;

    /**
     * 操作模式（CREATE、MODIFY、FIX）
     */
    private OperationModeEnum operationMode;

    /**
     * 意图类型（MODIFY 或 QA，由 IntentClassifierNode 设置）
     */
    private String intentType;

    /**
     * 项目目录结构（用于修改模式）
     */
    private String projectStructure;

    /**
     * 选中的元素信息（用于修改模式）
     */
    private ElementInfo elementInfo;

    /**
     * 修复重试次数
     */
    @Builder.Default
    private int fixRetryCount = 0;

    // ========== 数据库相关字段 ==========

    /**
     * 是否启用数据库
     */
    @Builder.Default
    private boolean databaseEnabled = false;

    /**
     * 初始数据库表结构（进入工作流前获取）
     */
    private String databaseSchema;

    /**
     * DatabaseOperator 输出：SQL 执行结果
     */
    private List<SqlExecutionResult> executionResults;

    /**
     * DatabaseOperator 输出：执行后的最新表结构
     */
    private String latestDatabaseSchema;

    // ========== ModificationPlanner 相关字段 ==========

    /**
     * ModificationPlanner 输出：完整的修改规划
     */
    private ModificationPlanResult modificationPlan;

    // ========== 数据库操作辅助方法 ==========

    /**
     * 检查是否有 SQL 执行失败
     *
     * @return true 如果有任何 SQL 执行失败
     */
    public boolean hasSqlExecutionFailure() {
        if (executionResults == null || executionResults.isEmpty()) {
            return false;
        }
        return executionResults.stream().anyMatch(result -> !result.isSuccess());
    }

    /**
     * 检查是否有成功执行的 SQL（即数据库有变更）
     *
     * @return true 如果有任何 SQL 执行成功
     */
    public boolean hasSqlExecutionSuccess() {
        if (executionResults == null || executionResults.isEmpty()) {
            return false;
        }
        return executionResults.stream().anyMatch(SqlExecutionResult::isSuccess);
    }

    // ========== ModificationPlanner 辅助方法 ==========

    /**
     * 获取规划的 SQL 语句列表（由 ModificationPlanner 产出）。
     * 如果规划为空则返回空列表。
     */
    public List<SqlStatement> getPlannedSqlStatements() {
        if (modificationPlan == null || modificationPlan.getSqlStatements() == null) {
            return Collections.emptyList();
        }
        return convertToSqlStatements(modificationPlan.getSqlStatements());
    }

    /**
     * 将 SqlStatementItem 转换为 SqlStatement
     */
    private List<SqlStatement> convertToSqlStatements(List<SqlStatementItem> items) {
        return items.stream()
            .map(item -> SqlStatement.builder()
                .type(item.getType())
                .sql(item.getSql())
                .description(item.getDescription())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 获取文件修改指导列表
     * 从 modificationPlan 中提取，如果为空则返回空列表
     */
    public List<FileModificationGuide> getFileModificationGuides() {
        if (modificationPlan == null || modificationPlan.getFilesToModify() == null) {
            return Collections.emptyList();
        }
        return modificationPlan.getFilesToModify();
    }

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

    // ========== 运行时端口注入 ==========

    /**
     * 注入流式输出端口（应用启动时调用一次）。
     */
    public static void configureWorkflowStreamPort(WorkflowStreamPort streamPort) {
        workflowStreamPort = streamPort;
    }

    // ========== 流式输出方法 ==========

    /**
     * 发送消息到流（如果 sink 存在）
     * 注意：此方法直接发送原始消息，用于转发已经是 JSON 格式的消息
     */
    public void emit(String message) {
        if (workflowStreamPort != null && workflowExecutionId != null) {
            workflowStreamPort.emit(workflowExecutionId, message);
        }
    }

    /**
     * 发送 AI 回复内容到流，自动包装为 JSON 格式
     * msgType 默认为 null，即 content 类型，用于发送 AI 实际回复内容
     */
    public void emitText(String text) {
        AiResponseMessage message = new AiResponseMessage(text);
        emit(JSONUtil.toJsonStr(message));
    }

    /**
     * 发送日志类型消息到流，自动包装为 JSON 格式
     * msgType 为 "log"，用于发送工作流节点状态等日志信息
     */
    public void emitLog(String text) {
        AiResponseMessage message = new AiResponseMessage(text);
        message.setMsgType("log");
        emit(JSONUtil.toJsonStr(message));
    }

    /**
     * 发送带前缀的节点消息（自动包装为 JSON 格式，log 类型）
     */
    public void emitNodeMessage(String nodeName, String message) {
        emitLog(String.format("[%s] %s", nodeName, message));
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

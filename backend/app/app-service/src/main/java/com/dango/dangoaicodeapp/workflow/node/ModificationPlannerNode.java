package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.FileModificationGuide;
import com.dango.aicodegenerate.model.ModificationPlanResult;
import com.dango.aicodegenerate.model.SqlStatementItem;
import com.dango.aicodegenerate.service.AiModificationPlannerService;
import com.dango.dangoaicodeapp.ai.AiModificationPlannerServiceFactory;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 修改规划节点
 * 分析用户需求，输出 SQL 操作计划和代码修改指导
 *
 * 职责：
 * - 接收项目结构、数据库 Schema（如果启用）、用户需求
 * - 调用 AI 分析并制定完整的修改规划
 * - 输出 ModificationPlanResult（包含 SQL 计划和代码修改计划）
 *
 * @author dango
 */
@Slf4j
@Component
public class ModificationPlannerNode {

    private static final String NODE_NAME = "修改规划";

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析需求并制定修改计划...\n");

            try {
                // 恢复监控上下文
                context.restoreMonitorContext();

                // 构建规划请求
                String planningRequest = buildPlanningRequest(context);
                log.info("修改规划请求:\n{}", planningRequest);

                // 获取 AI 规划服务
                AiModificationPlannerServiceFactory plannerFactory =
                    SpringContextUtil.getBean(AiModificationPlannerServiceFactory.class);
                AiModificationPlannerService plannerService =
                    plannerFactory.createPlannerService(context.getAppId());

                // 调用 AI 规划（结构化输出）
                ModificationPlanResult planResult = plannerService.plan(
                    context.getAppId(), planningRequest);
                log.info("修改规划结果: {}", planResult);

                // 保存到 context
                context.setModificationPlan(planResult);

                // 输出规划摘要
                outputPlanSummary(context, planResult);

            } catch (Exception e) {
                log.error("修改规划失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            } finally {
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
     * 构建规划请求
     * 参考设计文档 4.3 节
     */
    private static String buildPlanningRequest(WorkflowContext context) {
        StringBuilder request = new StringBuilder();

        // 数据库信息（如果启用）
        if (context.isDatabaseEnabled()) {
            request.append("## 数据库信息\n");
            request.append("Schema: app_").append(context.getAppId()).append("\n");

            String databaseSchema = context.getDatabaseSchema();
            if (StrUtil.isNotBlank(databaseSchema)) {
                request.append("现有表结构:\n").append(databaseSchema).append("\n\n");
            } else {
                request.append("现有表结构: 无（空数据库）\n\n");
            }

            // 受保护的文件说明（仅当数据库启用时）
            request.append("## 受保护的文件\n");
            request.append("以下文件由系统自动生成和管理，请勿修改：\n");
            request.append("- src/integrations/supabase/client.js (Supabase 客户端配置)\n");
            request.append("- package.json 中的 @supabase/supabase-js 依赖\n\n");
        }

        // 项目结构
        String projectStructure = context.getProjectStructure();
        if (StrUtil.isNotBlank(projectStructure)) {
            request.append("## 项目结构\n```\n")
                   .append(projectStructure)
                   .append("```\n\n");
        }

        // 用户需求
        String originalPrompt = context.getOriginalPrompt();
        if (StrUtil.isNotBlank(originalPrompt)) {
            request.append("## 用户需求\n")
                   .append(originalPrompt)
                   .append("\n\n");
        }

        return request.toString();
    }

    /**
     * 输出规划摘要
     * 参考设计文档 4.4 节
     */
    private static void outputPlanSummary(WorkflowContext context, ModificationPlanResult planResult) {
        if (planResult == null) {
            context.emitNodeMessage(NODE_NAME, "规划完成：无修改计划\n");
            return;
        }

        // 输出分析说明
        if (StrUtil.isNotBlank(planResult.getAnalysis())) {
            context.emitNodeMessage(NODE_NAME, "分析说明：" + planResult.getAnalysis() + "\n");
        }

        // 输出修改策略
        if (StrUtil.isNotBlank(planResult.getStrategy())) {
            context.emitNodeMessage(NODE_NAME, "修改策略：" + planResult.getStrategy() + "\n");
        }

        // 输出 SQL 计划摘要
        List<SqlStatementItem> sqlStatements = planResult.getSqlStatements();
        if (sqlStatements != null && !sqlStatements.isEmpty()) {
            context.emitNodeMessage(NODE_NAME,
                String.format("\nSQL 操作计划（共 %d 条）：\n", sqlStatements.size()));

            // 统计 SQL 类型（添加 null 检查）
            long ddlCount = sqlStatements.stream()
                .filter(stmt -> stmt != null && "DDL".equalsIgnoreCase(stmt.getType()))
                .count();
            long dmlCount = sqlStatements.stream()
                .filter(stmt -> stmt != null && "DML".equalsIgnoreCase(stmt.getType()))
                .count();

            if (ddlCount > 0) {
                context.emitNodeMessage(NODE_NAME, String.format("  - DDL 操作：%d 条\n", ddlCount));
            }
            if (dmlCount > 0) {
                context.emitNodeMessage(NODE_NAME, String.format("  - DML 操作：%d 条\n", dmlCount));
            }

            // 输出每条 SQL 的描述（添加 null 检查）
            for (SqlStatementItem stmt : sqlStatements) {
                if (stmt != null) {
                    String type = stmt.getType() != null ? stmt.getType() : "UNKNOWN";
                    String description = stmt.getDescription() != null ? stmt.getDescription() : "无描述";
                    context.emitNodeMessage(NODE_NAME,
                        String.format("  - [%s] %s\n", type, description));
                }
            }
        } else {
            context.emitNodeMessage(NODE_NAME, "\nSQL 操作计划：无需数据库操作\n");
        }

        // 输出代码修改计划摘要
        List<FileModificationGuide> filesToModify = planResult.getFilesToModify();
        if (filesToModify != null && !filesToModify.isEmpty()) {
            context.emitNodeMessage(NODE_NAME,
                String.format("\n代码修改计划（共 %d 个文件）：\n", filesToModify.size()));

            // 统计文件修改类型（添加 null 检查）
            long modifyCount = filesToModify.stream()
                .filter(file -> file != null && "MODIFY".equalsIgnoreCase(file.getType()))
                .count();
            long createCount = filesToModify.stream()
                .filter(file -> file != null && "CREATE".equalsIgnoreCase(file.getType()))
                .count();
            long deleteCount = filesToModify.stream()
                .filter(file -> file != null && "DELETE".equalsIgnoreCase(file.getType()))
                .count();

            if (modifyCount > 0) {
                context.emitNodeMessage(NODE_NAME, String.format("  - 修改文件：%d 个\n", modifyCount));
            }
            if (createCount > 0) {
                context.emitNodeMessage(NODE_NAME, String.format("  - 创建文件：%d 个\n", createCount));
            }
            if (deleteCount > 0) {
                context.emitNodeMessage(NODE_NAME, String.format("  - 删除文件：%d 个\n", deleteCount));
            }

            // 输出每个文件的修改说明（添加 null 检查）
            for (FileModificationGuide file : filesToModify) {
                if (file != null) {
                    String type = file.getType() != null ? file.getType() : "UNKNOWN";
                    String path = file.getPath() != null ? file.getPath() : "未知路径";
                    String reason = file.getReason() != null ? file.getReason() : "无说明";
                    context.emitNodeMessage(NODE_NAME,
                        String.format("  - [%s] %s: %s\n", type, path, reason));
                }
            }
        } else {
            context.emitNodeMessage(NODE_NAME, "\n代码修改计划：无需修改代码\n");
        }
    }
}

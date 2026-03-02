# AI 代码生成模块 - DDD 架构审阅

> 审阅日期: 2026-03-01
> 审阅范围: `app-service` 模块中 AI 代码生成全链路
> 入口点: `AppController` → `CodeGenApplicationServiceImpl` → `CodeGenWorkflow`

---

## 1. 执行摘要

### 模块概述

AI 代码生成是本系统的**核心业务能力**，用户通过自然语言描述生成完整的 Web 应用代码。该模块采用 **LangGraph4j 工作流引擎** 编排多个 AI 节点，支持创建、修改、修复三种操作模式，覆盖 Vue 工程、LeetCode 题解、面试题解三种代码生成类型。

### 架构评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 分层清晰度 | ⭐⭐⭐⭐ | 四层分离清晰，但 Facade 命名有误导 |
| 领域模型质量 | ⭐⭐⭐ | 充血模型方向正确，但核心领域逻辑分散 |
| 依赖方向 | ⭐⭐⭐⭐ | 基本遵循依赖倒置，少数违反 |
| 职责单一性 | ⭐⭐⭐ | 部分类职责过重 |
| 可测试性 | ⭐⭐ | 工作流难以单元测试，强依赖外部基础设施 |
| 总评 | ⭐⭐⭐☆ | 架构设计意识良好，细节有改进空间 |

### 关键发现

**优势**:
1. 工作流编排设计精良，子图分离清晰，条件边路由逻辑合理
2. 充血模型意识到位（App、ChatHistory 实体包含业务方法）
3. Repository 接口定义在领域层，实现在基础设施层，符合 DDD 依赖倒置
4. 工厂模式 + 策略模式组合实现多类型代码生成路由

**风险**:
1. `CodeGenApplicationServiceImpl` 职责过重，承担了编排、消息处理、Redis 操作、截图触发等多重职责
2. `WorkflowContext` 是一个巨大的上帝对象（God Object），承载了所有状态
3. `AiCodeGeneratorFacade` 不是真正的 Facade，实际是领域服务
4. 工作流每次请求都重新编译（`createWorkflow()`），无缓存

---

## 2. 数据流分析

### 完整请求链路

```
[前端 SSE 请求]
    │
    ▼
AppController.chatGenCode()
    │ 解析 ChatGenCodeRequest → appId, message, elementInfo
    ▼
CodeGenApplicationServiceImpl.startBackgroundGeneration()
    │ 1. 参数校验 + 权限校验
    │ 2. 保存用户消息到 ChatHistory
    │ 3. 预插入 generating 状态的 AI 消息
    │ 4. CAS 防重复启动（GenTaskService）
    │ 5. 查询数据库 Schema（如果启用）
    ▼
new CodeGenWorkflow().executeWorkflowWithFlux()
    │ 创建 WorkflowContext，注册 FluxSink 到全局注册表
    │ 在 TracedVirtualThread 中执行
    ▼
ModeRouterNode（模式路由）
    │ 判断 CREATE / EXISTING_CODE 模式
    │
    ├──[CREATE] → 创建模式子图
    │   ImagePlanNode → [并发图片收集] → ImageAggregatorNode
    │   → PromptEnhancerNode → CodeGeneratorNode
    │
    ├──[LEETCODE_CREATE] → 力扣创建子图
    │   LeetCodeAnimationAdvisorNode → LeetCodePromptEnhancerNode → CodeGeneratorNode
    │
    ├──[INTERVIEW_CREATE] → 面试创建子图
    │   InterviewAnimationAdvisorNode → InterviewPromptEnhancerNode → CodeGeneratorNode
    │
    └──[EXISTING_CODE] → 已有代码子图
        CodeReaderNode → IntentClassifierNode
        │
        ├──[MODIFY] → ModificationPlannerNode → [条件] → DatabaseOperatorNode → CodeModifierNode
        │                                         └──[skip_sql] → CodeModifierNode
        └──[QA] → QANode → END (跳过构建检查)
    │
    ▼ (除 QA 外)
构建检查修复子图
    BuildCheckNode ←→ CodeFixerNode（循环修复，最多 3 次）
    │
    ▼
StreamHandlerExecutor.doExecute()
    │ JsonMessageStreamHandler 解析 JSON 消息类型
    │ 格式化工具调用/执行的展示消息
    ▼
Redis Stream（写入）
    │ redisStreamService.addToStream()
    ▼
CodeGenApplicationServiceImpl.consumeGenerationStream()
    │ 从 Redis Stream 读取，推送 SSE
    ▼
[前端渲染]
```

### 关键数据转换点

| 阶段 | 输入 | 输出 | 转换逻辑 |
|------|------|------|---------|
| 请求入口 | `ChatGenCodeRequest` | `appId, message, ElementInfo` | Controller 解析 |
| 应用层编排 | 原始参数 | `WorkflowContext` | 构建工作流上下文 |
| 模式路由 | `WorkflowContext` | `OperationModeEnum` | 检查 elementInfo + 历史代码目录 |
| AI 生成 | `userMessage` | `TokenStream` → `Flux<String>` | LangChain4j 流式调用 |
| 流处理 | `Flux<String>` (原始 JSON) | `Flux<String>` (格式化 JSON) | `JsonMessageStreamHandler` |
| 持久化 | `Flux<String>` | Redis Stream + ChatHistory | 订阅写入 |

---

## 3. 分层分析

### 3.1 接口层 (interfaces)

**文件**: `AppController.java`

**职责**: HTTP 接口暴露、参数校验、DTO 转换

**评估**: ✅ 良好
- Controller 只做参数转换和调用应用层服务
- SSE 端点设计合理，使用 `Flux<ServerSentEvent>` 返回流式数据
- `chatGenCode` 和 `streamGenCode` 分离了启动任务和消费流两个职责

**问题**: 无明显问题

---

### 3.2 应用层 (application)

**文件**: `CodeGenApplicationServiceImpl.java`

**职责**: 用例编排、事务协调、基础设施调用

**评估**: ⚠️ 需要改进

**问题 1: 职责过重 (严重)**

`startBackgroundGeneration()` 方法承担了过多职责：
- 参数校验 + 权限校验
- 聊天历史保存
- CAS 防重复（GenTaskService）
- 数据库 Schema 查询（跨服务 Dubbo 调用）
- 工作流创建和执行
- Redis Stream 写入
- 聊天历史更新
- 截图触发

> **DDD 建议**: 将 Redis Stream 写入和消费抽取为独立的**基础设施端口**；将截图触发抽取为**领域事件**处理。

**问题 2: 每次请求 new 工作流实例**

```java
Flux<String> codeStream = new CodeGenWorkflow().executeWorkflowWithFlux(...);
```

工作流每次请求都重新创建，且 `createWorkflow()` 内部重新编译图。工作流的图结构是固定的，应该被缓存。

> **建议**: `CodeGenWorkflow` 应为 Spring Bean，图编译结果缓存为单例。

**问题 3: 应用层直接操作 Redis**

`consumeGenerationStream()` 直接轮询 Redis Stream，包含低层级的 while 循环和 Thread.sleep，这是基础设施层的关注点。

> **建议**: 抽取为 `MessageStreamPort` 接口，Redis 实现放在 infrastructure 层。

---

### 3.3 领域层 (domain)

#### 3.3.1 工作流核心 (domain/codegen/workflow)

**文件**: `CodeGenWorkflow.java`, `WorkflowContext.java`

**评估**: ⚠️ 设计精良但存在结构问题

**优点**:
- 子图分离清晰，LangGraph4j 的使用方式专业
- 条件边路由逻辑（模式判断、SQL 判断、修复循环）设计合理
- 并发图片收集体现了性能意识

**问题 1: WorkflowContext 上帝对象 (严重)**

`WorkflowContext` 有 **25+ 个字段**，承载了：
- 基本信息（appId, prompt, executionId）
- 图片相关（imageList, imageCollectionPlan, contentImages, illustrations, diagrams, logos）
- 数据库相关（databaseEnabled, databaseSchema, sqlStatements, executionResults, latestDatabaseSchema）
- 修改规划相关（modificationPlan）
- 构建检查相关（qualityResult, fixRetryCount）
- 监控（monitorContext）
- 全局 FluxSink 注册表（静态 ConcurrentHashMap）

> **DDD 建议**: 按子图职责拆分为多个上下文对象：`ImageCollectionContext`、`DatabaseContext`、`BuildContext` 等。通过组合模式在 `WorkflowContext` 中引用。

**问题 2: 静态全局注册表**

```java
private static final ConcurrentHashMap<String, FluxSink<String>> SINK_REGISTRY = new ConcurrentHashMap<>();
```

WorkflowContext 作为领域对象，持有静态全局状态是反模式。这引入了隐式的全局依赖，且在多实例部署时有隐患。

> **建议**: 将 SinkRegistry 提升为独立的基础设施组件，通过依赖注入使用。

#### 3.3.2 Facade 层 (domain/codegen/service)

**文件**: `AiCodeGeneratorFacade.java`

**评估**: ⚠️ 命名与职责不匹配

`AiCodeGeneratorFacade` 标注了 `@Service`，直接处理 `TokenStream` 到 `Flux<String>` 的转换，包含大量流处理逻辑（partialResponse、partialToolCall、toolExecuted 等回调）。这实际是一个**领域服务**，不是 Facade。

在当前工作流架构下，这个类的职责与 `CodeGeneratorNode` 存在重叠。工作流 Node 通过 `WorkflowContext.emit()` 输出消息，而 Facade 也有自己的流处理逻辑。

> **建议**:
> - 如果保留工作流架构，Facade 应退化为工厂方法容器
> - 如果保留 Facade，应把 TokenStream 处理逻辑统一到 Facade 中

#### 3.3.3 AI 服务工厂 (domain/codegen/ai/factory)

**文件**: `AiCodeGeneratorServiceFactory.java` 等 13 个工厂类

**评估**: ⚠️ 可优化

**优点**:
- Caffeine 缓存策略合理（时间 + 访问双维度过期）
- 动态配置变更监听（`@EventListener(EnvironmentChangeEvent.class)`）

**问题 1: 工厂类代码重复度高**

`AiCodeGeneratorServiceFactory`、`AiCodeModifierServiceFactory`、`AiCodeFixerServiceFactory` 三个工厂的核心逻辑高度相似：
- 构建 ChatMemory
- 加载聊天历史
- 通过 `AiServices.builder()` 创建实例
- Caffeine 缓存管理

> **建议**: 提取通用的 `AbstractAiServiceFactory<T>` 基类，子类只需定义服务接口类型和特定配置。

**问题 2: 工具注入不一致**

`AiCodeGeneratorServiceFactory` 通过 `ToolManager.getAllTools()` 注入工具，而 `AiCodeModifierServiceFactory` 直接注入 9 个具体 Tool Bean。两种方式混用。

> **建议**: 统一使用 `ToolManager` 管理工具注入。

**问题 3: 工厂类位置争议**

工厂类位于 `domain/codegen/ai/factory`，但它们直接依赖 LangChain4j SDK（`AiServices.builder()`）和 Spring（`@Component`）。这些是基础设施层的关注点。

> **DDD 建议**: 将 AI 服务创建逻辑视为**基础设施层**的适配器，领域层只定义服务接口。

#### 3.3.4 工具体系 (domain/codegen/tools)

**文件**: `BaseTool.java`, `ToolManager.java`

**评估**: ✅ 设计合理

- `BaseTool` 定义清晰的抽象（`getToolName()`, `generateToolRequestResponse()`, `generateToolExecutedResult()`）
- `ToolManager` 使用 `@PostConstruct` 自动注册所有工具实例
- 工具与 AI 服务的集成通过 LangChain4j 的 `@Tool` 注解实现

#### 3.3.5 领域实体 (domain/app/entity)

**文件**: `App.java`, `ChatHistory.java`

**评估**: ✅ 良好

充血模型设计：
- `App.checkOwnership()` — 所有权校验
- `App.markDeployed()` — 状态变更
- `App.enableDatabase()` — 含前置校验的业务操作
- `App.createNew()` — 工厂方法
- `ChatHistory.createUserMessage()` / `createAiMessage()` — 工厂方法

**小问题**: `App` 实体同时使用了 MyBatis-Flex 的 `@Table` 注解和 DDD 充血模型方法。虽然实用，但领域实体直接耦合了持久化框架。

---

### 3.4 基础设施层 (infrastructure)

**文件**: `AppRepositoryImpl.java`, `ChatHistoryRepositoryImpl.java`, `RedisStreamService`, `GenTaskService`

**评估**: ✅ 整体良好

- Repository 实现正确放在基础设施层
- Redis 相关服务（`RedisStreamService`、`GenTaskService`）提供了良好的抽象

**问题**: `RedisStreamService` 和 `GenTaskService` 被应用层直接使用，但没有在领域层定义对应的端口接口。

---

## 4. 模型质量评估

### 4.1 聚合边界

当前的聚合边界定义：

| 聚合根 | 包含 | 评估 |
|--------|------|------|
| `App` | 应用元数据、部署信息 | ✅ 边界清晰 |
| `ChatHistory` | 单条消息 | ⚠️ 应归属于 App 聚合 |
| `WorkflowContext` | 工作流全部状态 | ❌ 不是聚合，是过程对象 |

**问题**: `ChatHistory` 作为独立聚合存在，但业务上它强依赖 `App`（通过 `appId` 关联）。从 DDD 角度，聊天历史应该是 App 聚合的一部分，或者至少通过 App 聚合根来管理生命周期。

### 4.2 值对象

| 值对象 | 不可变性 | 评估 |
|--------|---------|------|
| `ElementInfo` | ⚠️ Lombok @Data 可变 | 应为 @Value 或 record |
| `CodeGenTypeEnum` | ✅ 枚举不可变 | 良好 |
| `OperationModeEnum` | ✅ 枚举不可变 | 良好 |

### 4.3 领域事件

当前**未使用领域事件**。以下场景适合引入：

| 事件 | 触发点 | 消费者 |
|------|--------|--------|
| `CodeGenerationCompleted` | 工作流完成 | 截图服务、聊天历史更新 |
| `CodeGenerationFailed` | 工作流失败 | 错误记录、状态更新 |
| `AppDeployed` | 部署完成 | 通知、统计 |

---

## 5. 设计决策评审

### 5.1 工作流引擎选择 — LangGraph4j

**决策**: 使用 LangGraph4j 编排 AI 代码生成流程

**评估**: ✅ **优秀决策**

- 子图能力天然支持多模式分离（创建/修改/修复）
- 条件边支持复杂的路由逻辑（意图分类、SQL 判断、修复循环）
- 并发节点支持图片收集的并行化
- 图可视化（Mermaid）便于调试

### 5.2 Redis Stream 消息桥接

**决策**: 后台生成写入 Redis Stream，前端 SSE 从 Redis Stream 消费

**评估**: ✅ **合理决策**

- 解耦了生成过程和 SSE 连接的生命周期
- 支持断线重连（通过 `afterId` 参数）
- 支持多实例部署

**风险**: Redis Stream 无持久化保证，服务重启会丢失未消费消息。

### 5.3 Caffeine 缓存 AI 服务实例

**决策**: 使用 Caffeine 缓存 `CodeGeneratorService` 等 AI 服务实例

**评估**: ⚠️ **存在风险**

- 缓存 key 为 `appId_codeGenType`，同一应用的多次请求共享 ChatMemory
- `maxMessages(50)` 限制了上下文窗口
- 缓存过期后重建服务会丢失 ChatMemory（依赖 Redis ChatMemoryStore 恢复）

### 5.4 文件系统作为代码存储

**决策**: 生成的代码保存在本地文件系统 (`tmp/code_output/`)

**评估**: ⚠️ **可扩展性受限**

- 模式路由通过 `Files.exists()` 检查目录判断是否有历史代码
- 不支持多实例部署（文件系统不共享）
- 缺少代码版本管理

---

## 6. 问题清单与解决方案

### P0 - 严重

#### 6.1 WorkflowContext 上帝对象

**问题**: 25+ 字段，承载了图片、数据库、构建、监控等所有子领域的状态

**影响**: 任何子图的修改都可能影响整个上下文，违反开闭原则

**解决方案**:
```java
// 按子图职责拆分
public class WorkflowContext {
    private String originalPrompt;
    private Long appId;
    private OperationModeEnum operationMode;

    // 组合子上下文
    private ImageCollectionContext imageContext;
    private DatabaseContext databaseContext;
    private BuildContext buildContext;
    private ModificationContext modificationContext;
}
```

#### 6.2 静态全局 SinkRegistry

**问题**: `WorkflowContext` 中的 `SINK_REGISTRY` 是静态 `ConcurrentHashMap`，领域对象不应持有全局状态

**影响**: 测试困难、多实例部署时状态不一致风险

**解决方案**: 提取为 `WorkflowSinkRegistry` 组件，通过 DI 注入到需要的地方。

---

### P1 - 重要

#### 6.3 CodeGenApplicationServiceImpl 职责过重

**问题**: 单个方法承担了 7+ 职责

**解决方案**: 使用领域事件解耦后续操作（截图触发、状态更新）；将 Redis 操作抽取为端口接口。

#### 6.4 工厂类代码重复

**问题**: 3 个核心 AI 服务工厂（Generator/Modifier/Fixer）代码相似度 > 70%

**解决方案**: 提取 `AbstractAiServiceFactory<T extends AiServiceInterface>` 基类。

#### 6.5 AiCodeGeneratorFacade 命名误导

**问题**: 标注 `@Service` 的类命名为 Facade，且与工作流 Node 存在职责重叠

**解决方案**: 重命名为 `AiCodeGeneratorDomainService`，明确其领域服务身份。

---

### P2 - 改进

#### 6.6 工作流每次请求重新编译

**问题**: `new CodeGenWorkflow()` + `createWorkflow()` 每次请求都重新编译图

**解决方案**: `CodeGenWorkflow` 作为 Spring Bean，图编译结果缓存。

#### 6.7 ElementInfo 可变性

**问题**: `ElementInfo` 作为值对象使用 `@Data`（可变）

**解决方案**: 改为 Java `record` 或 Lombok `@Value`。

#### 6.8 工具注入方式不一致

**问题**: Generator 用 `ToolManager.getAllTools()`，Modifier 直接注入 9 个具体 Bean

**解决方案**: 统一通过 `ToolManager` 管理，支持按场景过滤工具集。

---

## 7. 测试缺口与集成点

### 7.1 测试缺口

| 层级 | 测试类型 | 当前状态 | 优先级 |
|------|---------|---------|--------|
| 领域实体 | 单元测试 | ❌ 无 | P1 |
| WorkflowContext | 单元测试 | ❌ 无 | P1 |
| ModeRouterNode | 单元测试 | ❌ 无 | P1 |
| 工作流编排 | 集成测试 | ❌ 无 | P0 |
| AI 服务工厂 | 单元测试 | ❌ 无 | P2 |
| JsonMessageStreamHandler | 单元测试 | ❌ 无 | P1 |
| 端到端（Controller → SSE） | E2E 测试 | ❌ 无 | P2 |

**建议优先编写的测试**:

1. **ModeRouterNode 单元测试**: 验证模式判断逻辑（有/无 elementInfo、有/无历史代码目录）
2. **工作流编排集成测试**: 使用 Mock AI 服务验证各子图的路由逻辑
3. **JsonMessageStreamHandler 单元测试**: 验证不同消息类型的解析和格式化

### 7.2 集成点

| 集成点 | 协议 | 方向 | 影响 |
|--------|------|------|------|
| Nacos | HTTP | 出 | 服务注册/配置 |
| Redis (Stream) | Redis Protocol | 双向 | 消息桥接，断线重连 |
| Redis (ChatMemoryStore) | Redis Protocol | 双向 | AI 对话记忆持久化 |
| Supabase Service | Dubbo RPC | 出 | 数据库 Schema 查询/SQL 执行 |
| Screenshot Service | Dubbo RPC | 出 | 截图生成 |
| AI Model Provider | HTTP(S) | 出 | LLM API 调用（流式） |
| 文件系统 | 本地 I/O | 双向 | 代码读写、项目构建 |
| SkyWalking | gRPC | 出 | 分布式追踪 |

---

## 附录: 模块文件清单

### 核心链路文件 (按调用顺序)

1. `interfaces/controller/AppController.java` — HTTP 入口
2. `application/service/impl/CodeGenApplicationServiceImpl.java` — 应用层编排
3. `domain/codegen/workflow/CodeGenWorkflow.java` — 工作流引擎
4. `domain/codegen/workflow/state/WorkflowContext.java` — 工作流状态
5. `domain/codegen/node/ModeRouterNode.java` — 模式路由
6. `domain/codegen/node/CodeGeneratorNode.java` — 代码生成节点
7. `domain/codegen/node/CodeModifierNode.java` — 代码修改节点
8. `domain/codegen/node/CodeFixerNode.java` — 代码修复节点
9. `domain/codegen/node/BuildCheckNode.java` — 构建检查节点
10. `domain/codegen/service/AiCodeGeneratorFacade.java` — AI 代码生成领域服务
11. `domain/codegen/ai/factory/AiCodeGeneratorServiceFactory.java` — AI 服务工厂
12. `domain/codegen/handler/JsonMessageStreamHandler.java` — 流消息处理
13. `domain/codegen/tools/ToolManager.java` — 工具管理器

### 领域模型文件

14. `domain/app/entity/App.java` — 应用聚合根
15. `domain/app/entity/ChatHistory.java` — 聊天历史实体
16. `domain/app/valueobject/CodeGenTypeEnum.java` — 代码生成类型
17. `domain/app/valueobject/OperationModeEnum.java` — 操作模式
18. `domain/app/valueobject/ElementInfo.java` — 元素信息值对象
19. `domain/app/repository/AppRepository.java` — 应用仓储接口
20. `domain/app/repository/ChatHistoryRepository.java` — 聊天历史仓储接口

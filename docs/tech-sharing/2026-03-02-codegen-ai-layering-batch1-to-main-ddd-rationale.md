# 从 `codex/codegen-ai-layering-batch1` 到 `main`：CodeGen 分层重构的 DDD 处理说明

## 1. 范围与证据

本说明聚焦分支差异：`codex/codegen-ai-layering-batch1..main`。

### 1.1 证据命令

```bash
git log --oneline --reverse codex/codegen-ai-layering-batch1..main
git diff --shortstat codex/codegen-ai-layering-batch1..main
git diff --name-status codex/codegen-ai-layering-batch1..main
```

### 1.2 变更规模

- 17 个提交
- 109 个文件变更
- 2565 行新增 / 2410 行删除

> 这不是“局部修补”，而是一次跨应用层 / 领域层 / 基础设施层 / 测试层的结构性重构。

---

## 2. 这批改造在 DDD 上到底解决了什么

如果只看代码量，很容易把这次变更理解成“接口改名 + 文件搬家”。

从 DDD 视角，这批改造真正解决的是 4 类问题：

1. **领域层隐式耦合**
   - 以前节点对运行时设施（流式输出、AI 调用、文件系统）有直接或隐式依赖。
   - 改造后统一经 Port 进入，领域层表达“业务动作”，不携带设施实现。

2. **应用层职责漂移**
   - 以前应用服务既编排用例又处理大量一致性细节。
   - 改造后把“任务启动一致性”等规则下沉到领域服务，应用层回到“脚本化编排”。

3. **工作流入参与节点装配分散**
   - 以前存在重载扩散、动态查找、依赖图不透明。
   - 改造后采用 `RunWorkflowCommand` + `CodeGenWorkflowFactory`，依赖显式、装配集中。

4. **测试与构建稳定性弱**
   - 旧白盒测试依赖已移除 API，外部集成测试默认参与构建。
   - 改造后分层处理：纯单测保留、失配测试重建/占位、外部依赖测试默认不阻塞日常构建。

---

## 3. 分层改造地图（按 DDD 层次）

## 3.1 应用层：把流程写成“用例脚本”

- 关键文件：`backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/application/service/impl/CodeGenApplicationServiceImpl.java:74`
- 关键变化：
  - `startBackgroundGeneration` 保持顺序清晰：校验 -> 保存用户消息 -> 启动会话 -> 构建流 -> 订阅收口。
  - 新增 `CodeGenWorkflowExecutor` 承接运行时上下文与 sink 生命周期：
    `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/application/service/impl/CodeGenWorkflowExecutor.java:28`

**DDD 理由**：应用服务负责“用例编排”，不直接承载领域规则和设施细节。

---

## 3.2 领域服务：把一致性规则收口为领域能力

- 关键文件：
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/service/GenerationSessionDomainService.java:1`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/service/GenerationSessionDomainServiceImpl.java:33`

- 关键规则：
  - `startSession` 采用“先预占任务，再创建占位消息，再绑定 chatHistoryId，失败补偿”的原子语义。
  - `completeSession / failSession / appendChunk` 统一入口，避免状态散落。

**DDD 理由**：
- 这是领域不变量（同一 `(appId,userId)` 单活任务、消息与任务绑定关系），不应散在应用流程分支里。

---

## 3.3 领域端口：Gateway -> Port，语义从“技术连接器”转为“领域契约”

- 目录变化集中在：
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/port/`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/infrastructure/ai/codegen/gateway/`

- 代表性改名：
  - `IntentClassificationGateway` -> `IntentClassificationPort`
  - `ImageCollectionGateway` -> `ImageCollectionPort`
  - `CodeGenerationGateway` -> `CodeGenerationPort`

**DDD 理由**：
- `Port` 是领域对外能力边界，强调“领域需要什么”，而不是“怎么连外部系统”。
- 对应实现放在基础设施层，符合依赖倒置。

---

## 3.4 工作流编排：命令对象 + 工厂装配 + 显式依赖图

- 关键文件：
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/workflow/command/RunWorkflowCommand.java:12`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/workflow/CodeGenWorkflowFactory.java:97`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/workflow/CodeGenWorkflow.java:39`

- 关键变化：
  - 用 `RunWorkflowCommand` 统一入口参数，替代重载扩散。
  - 用 `CodeGenWorkflowFactory` 集中组装节点 action，避免 Service Locator/动态查找。

**DDD 理由**：
- 领域流程（workflow）应可读、可推理，依赖图必须显式。

---

## 3.5 节点输出链路：统一下沉到消息 Port

- 关键文件：
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/node/CodeGeneratorNode.java:71`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/node/CodeModifierNode.java:89`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/node/QANode.java:51`
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/infrastructure/workflow/WorkflowMessagePortImpl.java:20`

- 改造方向：
  - 节点不再直接依赖上下文“暗门”输出，统一走 `WorkflowMessagePort.emitRaw(...)`。

**DDD 理由**：
- 节点负责业务决策与状态推进；输出协议是基础设施责任。

---

## 3.6 线程池与生命周期：由 Spring Bean 托管而非请求级构建

- 关键文件：
  - `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/infrastructure/config/CodeGenWorkflowConfig.java:21`

- 关键变化：
  - `codeGenWorkflowParallelExecutor` 由容器单例托管，带 `destroyMethod = "shutdown"`。
  - 避免请求级工作流重复创建线程池造成资源抖动。

**DDD 理由**：
- 这是设施生命周期管理问题，应由 infrastructure/config 负责，不属于领域行为。

---

## 3.7 接口层：启动/状态/恢复三接口分离

- 关键文件：`backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/interfaces/controller/AppController.java:257`

- 拆分结果：
  - `POST /chat/gen/code`：只负责启动
  - `GET /chat/gen/status`：只查任务状态
  - `GET /chat/gen/resume`：只做流恢复消费

**DDD 理由**：
- 接口语义与用例边界对齐，避免一个端点承担多个职责。

---

## 4. 为什么“这么处理”而不是别的处理

### 4.1 为什么不是继续用 Service Locator

- Service Locator 把依赖藏在运行时，编译期看不出依赖图。
- 构造器注入 + 工厂装配能让节点依赖显式，单测替身简单。

### 4.2 为什么不是把一致性继续留在应用层 if/else

- 规则分散后很难保证“所有路径都补偿”。
- 聚合为 `GenerationSessionDomainService` 后，启动/完成/失败路径统一。

### 4.3 为什么不是全部直接操作 Redis/文件系统

- 直接调用设施 API 会把领域逻辑和技术细节绑死。
- Port 让领域层稳定，适配器可替换（Redis -> 其他实现成本更低）。

### 4.4 为什么要保留部分 `@Disabled` 测试

- 那些测试依赖外部运行环境，默认参与会让 CI/本地构建随机失败。
- 短期先保障默认流水线稳定，长期再做专门环境或契约测试重建。

---

## 5. 你可以直接用的“文档写法”（指导模板）

如果你要写“这次 DDD 改造说明”，建议按下面 8 段：

1. **背景与问题**：旧结构哪里违背分层（给 2-3 个具体例子）。
2. **目标与边界**：这次要解决什么，不解决什么。
3. **分层方案**：Application / Domain / Infrastructure 各自职责。
4. **关键改动清单**：按模块列文件与改动前后。
5. **为什么这么做（DDD 理由）**：每个改动都给“原则 -> 结果”。
6. **踩坑与修复**：静态上下文、测试失配、外部依赖波动。
7. **验证证据**：命令、统计、构建结果。
8. **后续计划**：哪些 `@Disabled` 测试要回补、哪些 Port 仍可继续收敛。

可直接引用本次命令证据：

```bash
git log --oneline --reverse codex/codegen-ai-layering-batch1..main
git diff --shortstat codex/codegen-ai-layering-batch1..main
mvn -f backend/pom.xml -pl app/app-service -am test -DskipTests=false
```

---

## 6. 结论（一句话版）

这批从 `codex/codegen-ai-layering-batch1` 到 `main` 的改造，本质是把 CodeGen 从“可运行但耦合重”推进到“分层清晰、依赖显式、规则可维护、构建可重复”的 DDD 结构。

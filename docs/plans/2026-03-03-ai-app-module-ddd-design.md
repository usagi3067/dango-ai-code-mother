# AI 模块和 App 模块基于 DDD 的职责划分设计

## 1. 设计目标

基于 DDD（领域驱动设计）原则，明确 ai 模块和 app 模块的职责边界：
- **ai 模块**：纯技术基础设施层，提供 AI 能力的技术封装
- **app 模块**：业务领域层，包含代码生成的业务逻辑

## 2. 核心设计原则

### 2.1 模块定位

**ai 模块（基础设施层）**
- 定位：通用的 AI 技术能力提供者
- 职责：封装 AI 框架（LangChain4j）的技术细节
- 特点：不包含任何业务逻辑，可被多个业务模块复用

**app 模块（业务领域层）**
- 定位：代码生成平台的核心业务领域
- 职责：实现代码生成的业务逻辑和工作流编排
- 特点：依赖 ai 模块的技术能力，实现具体业务场景

### 2.2 依赖关系

```
app 模块 ──依赖──> ai 模块 ──依赖──> LangChain4j
user 模块 ──依赖──> ai 模块 ──依赖──> LangChain4j
```

- ai 模块不依赖任何业务模块
- 业务模块通过接口扩展 ai 模块的能力

## 3. ai 模块职责详解

### 3.1 AI 模型集成

**提供的能力**：
- LangChain4j 框架集成和配置
- 多 AI 提供者管理（OpenAI、Anthropic、阿里云等）
- 模型选择和路由

**核心类**：
```java
// 模型提供者接口（使用字符串 serviceKey，不绑定业务）
public interface AiModelProvider {
    ChatModel getChatModel(String serviceKey);
    StreamingChatModel getStreamingChatModel(String serviceKey);
}

// Higress 网关实现
@Component
public class HigressAiModelProvider implements AiModelProvider {
    // 根据 serviceKey 从配置中获取模型
}

// AI 网关配置
@ConfigurationProperties(prefix = "ai")
public class AiGatewayProperties {
    // 网关配置、服务配置等
}
```

**设计要点**：
- `AiModelProvider` 使用字符串 `serviceKey`，不使用业务枚举
- 业务模块可以自定义 serviceKey（如 "code-generator"、"user-chat"）
- 配置文件中的 key 可以自由定义

### 3.2 流式消息处理（核心能力）

**提供的能力**：
- 将 LangChain4j 的 `TokenStream` 转换为统一的 `StreamMessage` 流
- 处理 AI 响应、工具调用、工具执行等消息类型
- 工具参数的增量解析

**核心类**：

```java
// 流式消息基类
public class StreamMessage {
    private String type;  // AI_RESPONSE, TOOL_REQUEST, TOOL_EXECUTED
}

// AI 响应消息
public class AiResponseMessage extends StreamMessage {
    private String data;      // AI 返回的文本内容
    private String msgType;   // 可选：log, info 等
}

// 工具请求消息
public class ToolRequestMessage extends StreamMessage {
    private String id;        // 工具调用 ID
    private String name;      // 工具名称
    private String filePath;  // 触发参数值（如文件路径）
    private String action;    // 操作类型（write, read, modify 等）
}

// 工具执行完成消息
public class ToolExecutedMessage extends StreamMessage {
    private String id;        // 工具调用 ID
    private String name;      // 工具名称
    private String arguments; // 完整的工具参数 JSON
    private String result;    // 工具执行结果
}
```

**流式响应处理器**：

```java
package com.dango.aicodegenerate.streaming;

/**
 * 流式响应处理器
 *
 * <h2>功能说明</h2>
 * 将 LangChain4j 的 TokenStream 转换为统一的 StreamMessage 流，
 * 便于业务模块进行流式处理和前端展示。
 *
 * <h2>使用场景</h2>
 * <ul>
 *   <li>代码生成场景：实时展示 AI 生成的代码和工具调用过程</li>
 *   <li>对话场景：实时展示 AI 的回复内容</li>
 *   <li>任何需要流式 AI 响应的场景</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 创建处理器（需要提供工具配置）
 * ToolConfig toolConfig = new MyToolConfig();
 * StreamingResponseProcessor processor = new StreamingResponseProcessor(toolConfig);
 *
 * // 2. 处理 TokenStream
 * TokenStream tokenStream = aiService.chat(userMessage);
 * Flux<StreamMessage> messageFlux = processor.process(tokenStream);
 *
 * // 3. 订阅消息流
 * messageFlux.subscribe(message -> {
 *     if (message instanceof AiResponseMessage) {
 *         // 处理 AI 响应
 *     } else if (message instanceof ToolRequestMessage) {
 *         // 处理工具请求
 *     } else if (message instanceof ToolExecutedMessage) {
 *         // 处理工具执行结果
 *     }
 * });
 *
 * // 4. 或者直接转换为 JSON 字符串流
 * Flux<String> jsonFlux = processor.processAsJson(tokenStream);
 * }</pre>
 *
 * <h2>扩展说明</h2>
 * 如果你的业务场景不需要工具调用，可以传入 null 作为 toolConfig：
 * <pre>{@code
 * StreamingResponseProcessor processor = new StreamingResponseProcessor(null);
 * }</pre>
 */
@Component
public class StreamingResponseProcessor {

    private final ToolConfig toolConfig;

    /**
     * 构造函数
     *
     * @param toolConfig 工具配置，如果不需要工具调用可以传 null
     */
    public StreamingResponseProcessor(@Autowired(required = false) ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }

    /**
     * 处理 TokenStream，转换为 StreamMessage 的 Flux
     *
     * @param tokenStream LangChain4j 的 TokenStream
     * @return StreamMessage 的响应式流
     */
    public Flux<StreamMessage> process(TokenStream tokenStream) {
        return Flux.create(sink -> {
            Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();

            tokenStream
                .onPartialResponse(partialResponse -> {
                    sink.next(new AiResponseMessage(partialResponse));
                })
                .onPartialToolCall(partialToolCall -> {
                    if (toolConfig == null) {
                        return; // 不处理工具调用
                    }

                    String toolId = partialToolCall.id();
                    String toolName = partialToolCall.name();
                    String delta = partialToolCall.partialArguments();

                    ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                        toolId,
                        id -> {
                            String triggerParam = toolConfig.getTriggerParam(toolName);
                            String action = toolConfig.getAction(toolName);
                            return new ToolArgumentsExtractor(id, toolName, triggerParam, action);
                        }
                    );

                    List<StreamMessage> messages = extractor.process(delta);
                    messages.forEach(sink::next);
                })
                .onToolExecuted(toolExecution -> {
                    sink.next(new ToolExecutedMessage(toolExecution));
                })
                .onCompleteResponse(response -> {
                    sink.complete();
                })
                .onError(error -> {
                    sink.error(error);
                })
                .start();
        });
    }

    /**
     * 处理并转换为 JSON 字符串流（便捷方法）
     *
     * @param tokenStream LangChain4j 的 TokenStream
     * @return JSON 字符串的响应式流
     */
    public Flux<String> processAsJson(TokenStream tokenStream) {
        return process(tokenStream)
            .map(JSONUtil::toJsonStr);
    }
}
```

**工具配置接口**：

```java
package com.dango.aicodegenerate.tool;

/**
 * 工具配置接口
 *
 * <h2>功能说明</h2>
 * 定义工具的配置信息，用于流式工具调用的参数解析。
 *
 * <h2>为什么需要这个接口</h2>
 * 在流式工具调用中，AI 会逐步返回工具参数的 JSON 片段。
 * 为了提升用户体验，我们希望在解析到关键参数（如文件路径）时，
 * 立即向前端发送 ToolRequestMessage，而不是等待所有参数解析完成。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyToolConfig implements ToolConfig {
 *
 *     @Override
 *     public String getTriggerParam(String toolName) {
 *         return switch (toolName) {
 *             case "writeFile" -> "filePath";
 *             case "searchUser" -> "userId";
 *             default -> null;  // 不需要提前触发
 *         };
 *     }
 *
 *     @Override
 *     public String getAction(String toolName) {
 *         return switch (toolName) {
 *             case "writeFile" -> "write";
 *             case "searchUser" -> "search";
 *             default -> "unknown";
 *         };
 *     }
 * }
 * }</pre>
 */
public interface ToolConfig {

    /**
     * 获取触发参数名
     *
     * <p>当解析到此参数时，会立即发送 ToolRequestMessage。
     *
     * @param toolName 工具名称
     * @return 触发参数名，如果不需要提前触发则返回 null
     */
    String getTriggerParam(String toolName);

    /**
     * 获取操作类型
     *
     * <p>用于 ToolRequestMessage 的 action 字段，
     * 便于前端展示不同的操作提示。
     *
     * @param toolName 工具名称
     * @return 操作类型（如 "write", "read", "search" 等）
     */
    String getAction(String toolName);
}
```

**工具参数提取器**：

```java
package com.dango.aicodegenerate.extractor;

/**
 * 工具参数提取器 - 状态机实现
 *
 * <h2>功能说明</h2>
 * 用于累积解析工具调用的 arguments JSON delta 片段。
 * 使用状态机管理解析过程，当解析到触发参数时立即发送消息。
 *
 * <h2>工作原理</h2>
 * <ol>
 *   <li>累积 arguments delta 片段</li>
 *   <li>状态机：INIT -> PARSING_TRIGGER_PARAM -> DONE</li>
 *   <li>解析 triggerParam（如 relativeFilePath）完成后发送 TOOL_REQUEST</li>
 *   <li>处理 JSON 转义字符（\n, \t, \", \\, \uXXXX）</li>
 * </ol>
 *
 * <h2>使用说明</h2>
 * 此类通常不需要直接使用，由 StreamingResponseProcessor 自动创建和管理。
 *
 * <h2>示例</h2>
 * <pre>{@code
 * // 创建提取器
 * ToolArgumentsExtractor extractor = new ToolArgumentsExtractor(
 *     "tool-123",           // 工具调用 ID
 *     "writeFile",          // 工具名称
 *     "relativeFilePath",   // 触发参数名
 *     "write"               // 操作类型
 * );
 *
 * // 处理 delta 片段
 * String delta1 = "{\"relativeFilePath\":\"src/";
 * List<StreamMessage> messages1 = extractor.process(delta1);  // 返回空列表
 *
 * String delta2 = "App.vue\",\"content\":\"...";
 * List<StreamMessage> messages2 = extractor.process(delta2);  // 返回 ToolRequestMessage
 * }</pre>
 */
public class ToolArgumentsExtractor {

    /**
     * 状态机状态
     */
    public enum State {
        INIT,                   // 初始状态
        PARSING_TRIGGER_PARAM,  // 正在解析触发参数
        DONE                    // 完成
    }

    private final String toolCallId;
    private final String toolName;
    private final String triggerParam;
    private final String action;

    private State state = State.INIT;
    private final StringBuilder rawBuffer = new StringBuilder();
    private int parsePosition = 0;
    private String triggerParamValue;
    private boolean toolRequestSent = false;

    /**
     * 构造函数
     *
     * @param toolCallId 工具调用 ID
     * @param toolName 工具名称
     * @param triggerParam 触发参数名，解析到此参数时立即发送消息
     * @param action 操作类型（write, read, search 等）
     */
    public ToolArgumentsExtractor(
        String toolCallId,
        String toolName,
        String triggerParam,
        String action
    ) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.triggerParam = triggerParam;
        this.action = action;
    }

    /**
     * 处理一个 delta 片段，返回需要发送的消息列表
     *
     * @param delta JSON delta 片段
     * @return 需要发送的消息列表（可能为空）
     */
    public List<StreamMessage> process(String delta) {
        List<StreamMessage> messages = new ArrayList<>();

        if (delta == null || delta.isEmpty() || triggerParam == null) {
            return messages;
        }

        // 累积原始数据
        rawBuffer.append(delta);
        String raw = rawBuffer.toString();

        // 根据状态处理
        switch (state) {
            case INIT -> processInit(raw, messages);
            case PARSING_TRIGGER_PARAM -> processTriggerParam(raw, messages);
            case DONE -> { /* 已完成，不再处理 */ }
        }

        return messages;
    }

    // ... 其他方法（状态机逻辑、JSON 解析、转义处理）
}
```

### 3.3 工具系统基础（核心能力）

**提供的能力**：
- 工具基类定义
- 工具注册表接口
- 工具的通用行为抽象

**核心类**：

```java
package com.dango.aicodegenerate.tool;

/**
 * 工具基类 - 定义所有工具的通用接口
 *
 * <h2>功能说明</h2>
 * 所有 AI 工具都应该继承此类，实现工具的基本信息和消息格式化方法。
 *
 * <h2>设计理念</h2>
 * <ul>
 *   <li>工具名称：对应 LangChain4j 的 @Tool 方法名</li>
 *   <li>显示名称：用于前端展示</li>
 *   <li>消息格式化：定义工具请求和执行结果的展示格式</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class FileWriteTool extends BaseTool {
 *
 *     @Tool("写入文件到指定路径")
 *     public String writeFile(
 *         @P("文件的相对路径") String filePath,
 *         @P("要写入文件的内容") String content
 *     ) {
 *         // 实现文件写入逻辑
 *         return "文件写入成功: " + filePath;
 *     }
 *
 *     @Override
 *     public String getToolName() {
 *         return "writeFile";
 *     }
 *
 *     @Override
 *     public String getDisplayName() {
 *         return "写入文件";
 *     }
 *
 *     @Override
 *     public String generateToolExecutedMessage(JSONObject arguments) {
 *         String filePath = arguments.getStr("filePath");
 *         String content = arguments.getStr("content");
 *         return String.format("[工具调用] 写入文件 %s\n```\n%s\n```", filePath, content);
 *     }
 * }
 * }</pre>
 *
 * <h2>扩展说明</h2>
 * 业务模块可以创建自己的工具基类，继承此类并添加业务特定的能力：
 * <pre>{@code
 * // 代码生成场景的工具基类
 * public abstract class CodeGenBaseTool extends BaseTool {
 *     protected Path getProjectRoot(Long appId) {
 *         // 代码生成特有的项目路径解析逻辑
 *     }
 * }
 *
 * // 用户管理场景的工具基类
 * public abstract class UserManagementBaseTool extends BaseTool {
 *     protected User getCurrentUser() {
 *         // 用户管理特有的用户获取逻辑
 *     }
 * }
 * }</pre>
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应 LangChain4j 的 @Tool 方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的显示名称
     *
     * @return 工具中文名称或其他语言的显示名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的显示内容
     *
     * <p>当 AI 选择使用此工具时，会调用此方法生成显示给用户的消息。
     * 子类可以覆盖此方法自定义显示格式。
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestMessage() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果的显示内容
     *
     * <p>当工具执行完成后，会调用此方法生成显示给用户的消息。
     * 子类必须实现此方法，定义如何格式化工具执行结果。
     *
     * @param arguments 工具执行的参数（JSON 对象）
     * @return 格式化的执行结果字符串
     */
    public abstract String generateToolExecutedMessage(JSONObject arguments);
}
```

```java
package com.dango.aicodegenerate.tool;

/**
 * 工具注册表接口
 *
 * <h2>功能说明</h2>
 * 定义工具管理的抽象，具体实现由业务模块提供。
 *
 * <h2>为什么需要这个接口</h2>
 * <ul>
 *   <li>ai 模块不应该知道具体有哪些工具</li>
 *   <li>不同业务模块可以有不同的工具集合</li>
 *   <li>工具的注册和管理是业务逻辑，不是技术基础设施</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyToolRegistry implements ToolRegistry {
 *
 *     private final Map<String, BaseTool> toolMap = new HashMap<>();
 *
 *     @Resource
 *     private BaseTool[] tools;  // Spring 自动注入所有 BaseTool 实现
 *
 *     @PostConstruct
 *     public void init() {
 *         for (BaseTool tool : tools) {
 *             registerTool(tool);
 *         }
 *     }
 *
 *     @Override
 *     public BaseTool getTool(String toolName) {
 *         return toolMap.get(toolName);
 *     }
 *
 *     @Override
 *     public BaseTool[] getAllTools() {
 *         return tools;
 *     }
 *
 *     @Override
 *     public void registerTool(BaseTool tool) {
 *         toolMap.put(tool.getToolName(), tool);
 *         log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
 *     }
 * }
 * }</pre>
 */
public interface ToolRegistry {

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具名称
     * @return 工具实例，如果不存在则返回 null
     */
    BaseTool getTool(String toolName);

    /**
     * 获取所有已注册的工具
     *
     * @return 工具数组
     */
    BaseTool[] getAllTools();

    /**
     * 注册一个工具
     *
     * @param tool 工具实例
     */
    void registerTool(BaseTool tool);
}
```

### 3.4 AI 基础能力

**提供的能力**：
- 守护栏（输入安全检查）
- 聊天记忆存储（Redis 集成）
- 流式上下文传播（跨线程 trace 传递）

**核心类**：

```java
// 守护栏
@Component
public class PromptSafetyInputGuardrail implements Guardrail {
    // 输入安全检查逻辑
}

// Redis 聊天记忆存储配置
@Configuration
public class RedisChatMemoryStoreConfig {
    @Bean
    public ChatMemoryStore redisChatMemoryStore() {
        // Redis 集成配置
    }
}

// 流式上下文传播配置
@Configuration
public class StreamingContextPropagationConfig {
    @Bean
    public AsyncTaskExecutor streamingContextPropagatingExecutor() {
        // SkyWalking trace 传递配置
    }
}
```

### 3.5 ai 模块不包含的内容

**明确排除**：
- ❌ 业务领域模型（App、ChatHistory 等）
- ❌ 工作流编排逻辑
- ❌ 具体的工具实现
- ❌ 业务服务类型枚举（如 CodeGenTypeEnum）
- ❌ AI 服务的输出模型（如 ModificationPlanResult）

## 4. app 模块职责详解

### 4.1 领域模型

**聚合根**：
- `App` - 应用聚合根
- `ChatHistory` - 聊天历史聚合根

**值对象**：
- `CodeGenTypeEnum` - 代码生成类型
- `OperationModeEnum` - 操作模式
- `ElementInfo` - 元素信息

**领域服务**：
- `AppDomainService` - 应用领域服务
- `FeatureAnalysisDomainService` - 特性分析领域服务
- `GenerationSessionDomainService` - 生成会话领域服务

### 4.2 工作流编排

**工作流定义**：
- `CodeGenWorkflow` - 代码生成工作流执行器
- `CodeGenWorkflowFactory` - 工作流工厂

**工作流节点**：
- `CodeGeneratorNode` - 代码生成节点
- `CodeFixerNode` - 代码修复节点
- `ModeRouterNode` - 模式路由节点
- `BuildCheckNode` - 构建检查节点
- 等等...

**工作流状态**：
- `WorkflowContext` - 工作流上下文
- `SqlExecutionResult` - SQL 执行结果
- `SqlStatement` - SQL 语句

### 4.3 业务工具实现

**工具基类**：

```java
package com.dango.dangoaicodeapp.domain.codegen.tools;

/**
 * 代码生成场景的工具基类
 * 继承 ai 模块的 BaseTool，添加代码生成特定的能力
 */
public abstract class CodeGenBaseTool extends com.dango.aicodegenerate.tool.BaseTool {

    /**
     * 根据 appId 获取项目根目录路径
     * 自动探测项目类型（vue_project、leetcode_project 等）
     */
    protected Path getProjectRoot(Long appId) {
        if (appId == null || appId <= 0) {
            return null;
        }

        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            String dirName = type.getValue() + "_" + appId;
            Path projectPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
            if (Files.exists(projectPath)) {
                return projectPath;
            }
        }

        return null;
    }

    /**
     * 根据 appId 获取项目根目录路径，如果不存在则使用默认类型创建路径
     */
    protected Path getProjectRootOrDefault(Long appId, CodeGenTypeEnum defaultType) {
        Path existingPath = getProjectRoot(appId);
        if (existingPath != null) {
            return existingPath;
        }

        if (defaultType == null) {
            defaultType = CodeGenTypeEnum.VUE_PROJECT;
        }
        String dirName = defaultType.getValue() + "_" + appId;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
    }
}
```

**具体工具实现**：

```java
@Component
public class FileWriteTool extends CodeGenBaseTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
        @P("文件的相对路径") String relativeFilePath,
        @P("要写入文件的内容") String content,
        @ToolMemoryId Long appId
    ) {
        // 使用父类的 getProjectRootOrDefault 方法
        Path projectRoot = getProjectRootOrDefault(appId, null);
        Path path = projectRoot.resolve(relativeFilePath);

        // 文件写入逻辑
        Files.write(path, content.getBytes());
        return "文件写入成功: " + relativeFilePath;
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedMessage(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String content = arguments.getStr("content");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        return String.format("""
            [工具调用] %s %s
            ```%s
            %s
            ```
            """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
```

**工具管理器**：

```java
@Component
public class ToolManager implements com.dango.aicodegenerate.tool.ToolRegistry {

    private final Map<String, BaseTool> toolMap = new HashMap<>();

    @Resource
    private BaseTool[] tools;  // Spring 自动注入所有 BaseTool 实现

    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            registerTool(tool);
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    @Override
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    @Override
    public BaseTool[] getAllTools() {
        return tools;
    }

    @Override
    public void registerTool(BaseTool tool) {
        toolMap.put(tool.getToolName(), tool);
        log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
    }
}
```

### 4.4 AI 服务定义和实现

**AI 服务接口**：

```java
// 代码生成服务接口
public interface CodeGeneratorService {
    TokenStream generateCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}

// Vue 项目代码生成服务
public interface VueCodeGeneratorService extends CodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    @Override
    TokenStream generateCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}

// LeetCode 项目代码生成服务
public interface LeetCodeCodeGeneratorService extends CodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-leetcode-project-system-prompt.txt")
    @Override
    TokenStream generateCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
```

**AI 服务工厂**：

```java
@Component
public class AiCodeGeneratorServiceFactory {

    @Resource
    private AiModelProvider aiModelProvider;  // 来自 ai 模块

    @Resource
    private ChatMemoryStore redisChatMemoryStore;  // 来自 ai 模块

    @Resource
    private ToolManager toolManager;  // app 模块的工具管理器

    private final Cache<String, CodeGeneratorService> serviceCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .build();

    public CodeGeneratorService getService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createService(appId, codeGenType));
    }

    private CodeGeneratorService createService(long appId, CodeGenTypeEnum codeGenType) {
        // 构建聊天记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .id("chat_" + appId)
            .chatMemoryStore(redisChatMemoryStore)
            .maxMessages(50)
            .build();

        // 选择服务类
        Class<? extends CodeGeneratorService> serviceClass = switch (codeGenType) {
            case LEETCODE_PROJECT -> LeetCodeCodeGeneratorService.class;
            case INTERVIEW_PROJECT -> InterviewCodeGeneratorService.class;
            default -> VueCodeGeneratorService.class;
        };

        // 构建 AI 服务
        return AiServices.builder(serviceClass)
            .streamingChatModel(
                aiModelProvider.getStreamingChatModel("code-generator")  // 使用字符串 key
            )
            .chatMemory(chatMemory)
            .tools(toolManager.getAllTools())
            .inputGuardrails(new PromptSafetyInputGuardrail())
            .build();
    }
}
```

**AI 输出模型**：

```java
// 这些模型移到 app 模块
package com.dango.dangoaicodeapp.infrastructure.ai.model;

@Description("应用名称和标签生成结果")
@Data
public class AppNameAndTagResult {
    @Description("应用名称，不超过20个字符")
    private String appName;

    @Description("应用标签")
    private String tag;
}

@Description("修改规划结果")
@Data
public class ModificationPlanResult {
    @Description("规划说明，简要描述分析结果")
    private String analysis;

    @Description("修改策略，说明整体修改思路")
    private String strategy;

    @Description("需要修改的文件列表")
    private List<FileModificationGuide> filesToModify;
}
```

### 4.5 端口和适配器

**端口定义**：

```java
// 代码生成端口
public interface CodeGenerationPort {
    TokenStream generateCodeStream(long appId, CodeGenTypeEnum codeGenType, String userMessage);
}

// 代码生成流端口
public interface CodeGenerationStreamPort {
    Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId);
}
```

**端口实现**：

```java
@Component
public class CodeGenerationStreamPortImpl implements CodeGenerationStreamPort {

    @Resource
    private StreamingResponseProcessor streamingProcessor;  // 来自 ai 模块

    @Resource
    private CodeGenerationPort codeGenerationPort;

    @Override
    public Flux<String> generateAndSaveCodeStream(
        String userMessage,
        CodeGenTypeEnum codeGenType,
        Long appId
    ) {
        TokenStream tokenStream = codeGenerationPort.generateCodeStream(
            appId, codeGenType, userMessage
        );

        // 使用 ai 模块的流式处理能力
        return streamingProcessor.processAsJson(tokenStream);
    }
}
```

**工具配置实现**：

```java
@Component
public class CodeGenToolConfig implements com.dango.aicodegenerate.tool.ToolConfig {

    private static final Map<String, String> TRIGGER_PARAMS = Map.of(
        "writeFile", "relativeFilePath",
        "modifyFile", "relativeFilePath",
        "readFile", "relativeFilePath",
        "readDir", "relativeDirPath",
        "deleteFile", "relativeFilePath",
        "searchContentImages", "query",
        "searchIllustrations", "query",
        "generateLogos", "description",
        "generateMermaidDiagram", "mermaidCode"
    );

    @Override
    public String getTriggerParam(String toolName) {
        return TRIGGER_PARAMS.get(toolName);
    }

    @Override
    public String getAction(String toolName) {
        return switch (toolName) {
            case "writeFile" -> "write";
            case "modifyFile" -> "modify";
            case "readFile", "readDir" -> "read";
            case "deleteFile" -> "delete";
            case "searchContentImages", "searchIllustrations" -> "search";
            case "generateLogos", "generateMermaidDiagram" -> "generate";
            default -> "unknown";
        };
    }
}
```

## 5. 模块依赖关系

### 5.1 依赖图

```
┌─────────────────────────────────────────────────────────┐
│                     业务模块层                            │
├─────────────────────────────────────────────────────────┤
│  app 模块                    user 模块                   │
│  - 代码生成业务逻辑           - 用户管理业务逻辑           │
│  - 工作流编排                - 用户工具实现               │
│  - 代码生成工具实现           - 用户 AI 服务              │
│  - CodeGenToolConfig        - UserToolConfig            │
└──────────────┬──────────────────────────┬────────────────┘
               │                          │
               │  依赖                     │  依赖
               ↓                          ↓
┌─────────────────────────────────────────────────────────┐
│                   AI 基础设施层                           │
├─────────────────────────────────────────────────────────┤
│  ai 模块                                                 │
│  - AiModelProvider (接口 + 实现)                         │
│  - StreamingResponseProcessor                           │
│  - ToolArgumentsExtractor                               │
│  - BaseTool (抽象类)                                     │
│  - ToolRegistry (接口)                                   │
│  - ToolConfig (接口)                                     │
│  - StreamMessage 系列                                    │
│  - 守护栏、记忆存储等                                     │
└──────────────┬──────────────────────────────────────────┘
               │
               │  依赖
               ↓
┌─────────────────────────────────────────────────────────┐
│                   技术框架层                              │
├─────────────────────────────────────────────────────────┤
│  - LangChain4j                                          │
│  - Spring Boot                                          │
│  - Redis                                                │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Maven 依赖

**ai 模块的 pom.xml**：

```xml
<dependencies>
    <!-- LangChain4j -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-anthropic-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-community-redis-spring-boot-starter</artifactId>
    </dependency>

    <!-- Common 模块 -->
    <dependency>
        <groupId>com.dango</groupId>
        <artifactId>common</artifactId>
    </dependency>
</dependencies>
```

**app 模块的 pom.xml**：

```xml
<dependencies>
    <!-- AI 模块 -->
    <dependency>
        <groupId>com.dango</groupId>
        <artifactId>ai</artifactId>
    </dependency>

    <!-- Common 模块 -->
    <dependency>
        <groupId>com.dango</groupId>
        <artifactId>common</artifactId>
    </dependency>

    <!-- 其他业务依赖 -->
</dependencies>
```

## 6. 扩展场景示例

### 6.1 场景一：user 模块需要 AI 对话功能

**步骤 1：实现工具配置**

```java
// user 模块
@Component
public class UserToolConfig implements com.dango.aicodegenerate.tool.ToolConfig {

    @Override
    public String getTriggerParam(String toolName) {
        return switch (toolName) {
            case "getUserProfile" -> "userId";
            case "searchUsers" -> "keyword";
            default -> null;
        };
    }

    @Override
    public String getAction(String toolName) {
        return switch (toolName) {
            case "getUserProfile" -> "query";
            case "searchUsers" -> "search";
            default -> "unknown";
        };
    }
}
```

**步骤 2：实现工具**

```java
// user 模块
@Component
public class UserProfileTool extends com.dango.aicodegenerate.tool.BaseTool {

    @Tool("获取用户资料")
    public String getUserProfile(@P("用户ID") Long userId) {
        // 查询用户资料
        User user = userService.getById(userId);
        return JSONUtil.toJsonStr(user);
    }

    @Override
    public String getToolName() {
        return "getUserProfile";
    }

    @Override
    public String getDisplayName() {
        return "获取用户资料";
    }

    @Override
    public String generateToolExecutedMessage(JSONObject arguments) {
        Long userId = arguments.getLong("userId");
        return String.format("[工具调用] 获取用户资料 (ID: %d)", userId);
    }
}
```

**步骤 3：使用流式处理**

```java
// user 模块
@Service
public class UserAiChatService {

    @Resource
    private StreamingResponseProcessor streamingProcessor;  // 来自 ai 模块

    @Resource
    private AiModelProvider aiModelProvider;  // 来自 ai 模块

    @Resource
    private UserToolRegistry userToolRegistry;  // user 模块的工具注册表

    public Flux<String> chat(String userMessage) {
        // 创建 AI 服务
        var chatService = AiServices.builder(UserChatService.class)
            .streamingChatModel(aiModelProvider.getStreamingChatModel("user-chat"))
            .tools(userToolRegistry.getAllTools())
            .build();

        TokenStream tokenStream = chatService.chat(userMessage);

        // 使用 ai 模块的流式处理能力
        return streamingProcessor.processAsJson(tokenStream);
    }
}
```

**配置文件**：

```yaml
ai:
  gateway:
    base-url: http://higress-gateway
    default-model: gpt-4
  services:
    user-chat:
      model: gpt-3.5-turbo
      max-tokens: 2048
```

### 6.2 场景二：report 模块需要数据分析功能

**步骤 1：定义 AI 服务**

```java
// report 模块
public interface DataAnalysisService {
    @SystemMessage("你是一个数据分析专家...")
    AnalysisResult analyze(@UserMessage String dataDescription);
}
```

**步骤 2：使用 AI 模块**

```java
// report 模块
@Service
public class ReportAnalysisService {

    @Resource
    private AiModelProvider aiModelProvider;  // 来自 ai 模块

    public AnalysisResult analyzeData(String dataDescription) {
        // 创建 AI 服务（不需要工具调用）
        var analysisService = AiServices.builder(DataAnalysisService.class)
            .chatModel(aiModelProvider.getChatModel("data-analysis"))
            .build();

        return analysisService.analyze(dataDescription);
    }
}
```

**配置文件**：

```yaml
ai:
  services:
    data-analysis:
      model: gpt-4
      max-tokens: 4096
```

## 7. 关键设计决策

### 7.1 为什么 AiServiceType 不在 ai 模块？

**问题**：`AiServiceType` 枚举包含业务服务类型（CODE_GENERATOR、CODE_MODIFIER 等）

**决策**：移除 `AiServiceType`，使用字符串 serviceKey

**理由**：
1. ai 模块不应该知道业务服务类型
2. 不同业务模块可以定义自己的服务类型
3. 配置文件中的 key 可以自由定义

### 7.2 为什么 AI 输出模型在 app 模块？

**问题**：`ModificationPlanResult`、`AppNameAndTagResult` 等模型看起来像技术层的数据结构

**决策**：这些模型移到 app 模块

**理由**：
1. 它们使用 `@Description` 注解是为了让 AI 理解业务概念
2. 它们的结构由业务需求决定，而不是技术实现
3. 其他服务不需要看到这些代码生成场景的模型

### 7.3 为什么 BaseTool 在 ai 模块？

**问题**：`BaseTool` 包含 `getProjectRoot()` 等业务方法

**决策**：将 `BaseTool` 拆分为两层
- ai 模块：`BaseTool`（通用接口）
- app 模块：`CodeGenBaseTool`（业务扩展）

**理由**：
1. 工具的基本抽象是通用的
2. 业务特定的能力通过继承添加
3. 其他服务可以创建自己的工具基类

### 7.4 为什么 ToolArgumentsExtractor 在 ai 模块？

**问题**：`ToolArgumentsExtractor` 包含工具配置（TOOL_TRIGGER_PARAMS）

**决策**：保持在 ai 模块，但改为可配置
- 核心解析逻辑在 ai 模块
- 工具配置通过 `ToolConfig` 接口由业务模块提供

**理由**：
1. JSON 解析、状态机等核心能力是通用的
2. 工具配置是业务逻辑，通过接口扩展
3. 其他服务可以提供自己的工具配置

## 8. 实施建议

### 8.1 实施步骤

**阶段一：ai 模块重构**
1. 移除 `AiServiceType` 枚举
2. 修改 `AiModelProvider` 使用字符串 serviceKey
3. 创建 `StreamingResponseProcessor`
4. 创建 `ToolConfig` 接口
5. 修改 `ToolArgumentsExtractor` 支持配置
6. 创建 `BaseTool` 抽象类
7. 创建 `ToolRegistry` 接口

**阶段二：app 模块重构**
1. 移动 AI 输出模型到 app 模块
2. 创建 `CodeGenBaseTool` 继承 `BaseTool`
3. 修改所有工具继承 `CodeGenBaseTool`
4. 实现 `CodeGenToolConfig`
5. 修改 `ToolManager` 实现 `ToolRegistry`
6. 修改 AI 服务工厂使用字符串 serviceKey
7. 更新配置文件

**阶段三：测试验证**
1. 单元测试：测试 ai 模块的各个组件
2. 集成测试：测试 app 模块的业务流程
3. 端到端测试：测试完整的代码生成流程

### 8.2 注意事项

1. **向后兼容**：如果有其他服务已经依赖 ai 模块，需要提供过渡方案
2. **配置迁移**：需要更新配置文件中的 serviceKey
3. **文档更新**：更新 API 文档和使用说明
4. **代码审查**：确保所有改动符合 DDD 原则

## 9. 总结

### 9.1 核心改进

1. **明确职责边界**
   - ai 模块：纯技术基础设施
   - app 模块：业务领域逻辑

2. **提升复用性**
   - 流式处理能力可被多个服务复用
   - 工具系统可扩展到其他业务场景

3. **降低耦合度**
   - ai 模块不依赖业务模块
   - 通过接口实现扩展

4. **符合 DDD 原则**
   - 技术层和领域层清晰分离
   - 业务概念在业务模块中定义

### 9.2 预期收益

1. **可维护性提升**：职责清晰，易于理解和修改
2. **可扩展性提升**：新业务模块可以快速接入 AI 能力
3. **可测试性提升**：模块独立，易于编写单元测试
4. **团队协作提升**：不同团队可以独立开发不同模块


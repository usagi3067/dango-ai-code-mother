# Higress AI Gateway 集成设计（DDD 分层 + Nacos 热更新）

## 概述

将后端 LangChain4j 的 AI 模型调用统一接入 Higress AI Gateway，采用 DDD 分层设计，实现：
- **统一 AI 模型管理**：所有 API Key、Failover、负载均衡由 Higress 管理
- **按服务粒度选模型**：每个 AI 服务通过逻辑模型名指定使用的模型
- **非官方 API 支持**：OpenAI 兼容代理作为 Higress AI Provider 接入
- **Nacos 热更新**：改配置即时生效，不需要重启服务
- **DDD 分层**：领域层不感知 Higress 技术细节

## 架构

### 改造前

```
11个AI服务工厂 → 6个Config类(管理各家api-key) → Anthropic API
              → Failover机制(3个类)            → DeepSeek API
                                               → Kimi API
```

### 改造后

```
11个AI服务工厂                  基础设施层                    Higress
  ↓                            ↓                           ↓
AiModelProvider(接口)  ←实现←  HigressAiModelProvider  →  Higress AI GW
  领域层                       读取 AiGatewayProperties      ↓
  只知道 AiServiceType枚举     按服务名构建模型实例      ├→ DeepSeek(官方)
                                                       ├→ 代理A(非官方)
                                                       └→ Anthropic(备用)
```

## DDD 分层设计

### 层级归属

```
backend/ai/src/main/java/com/dango/aicodegenerate/
├── model/                          # 领域层
│   ├── AiServiceType.java          # 枚举：AI 服务类型
│   └── AiModelProvider.java        # 接口：AI 模型提供者
├── config/                         # 基础设施层
│   ├── AiGatewayProperties.java    # 配置绑定
│   └── HigressAiModelProvider.java # AiModelProvider 实现
```

### 1. AiServiceType（领域层 - 枚举）

定义系统中所有 AI 服务类型，与 YAML 配置的 key 对应。

```java
package com.dango.aicodegenerate.model;

public enum AiServiceType {
    CODE_GENERATOR("code-generator"),
    CODE_MODIFIER("code-modifier"),
    CODE_FIXER("code-fixer"),
    QA("qa"),
    ANIMATION_ADVISOR("animation-advisor"),
    MODIFICATION_PLANNER("modification-planner"),
    APP_INFO_GENERATOR("app-info-generator"),
    INTENT_CLASSIFIER("intent-classifier"),
    FEATURE_ANALYZER("feature-analyzer"),
    CODE_QUALITY_CHECK("code-quality-check"),
    IMAGE_COLLECTION("image-collection");

    private final String configKey;

    AiServiceType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
```

### 2. AiModelProvider（领域层 - 接口）

领域层定义的抽象，不包含任何技术细节。

```java
package com.dango.aicodegenerate.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public interface AiModelProvider {
    ChatModel getChatModel(AiServiceType serviceType);
    StreamingChatModel getStreamingChatModel(AiServiceType serviceType);
}
```

### 3. AiGatewayProperties（基础设施层 - 配置绑定）

纯 POJO，只做 YAML → Java 的数据绑定。

```java
package com.dango.aicodegenerate.config;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiGatewayProperties {

    private Gateway gateway;
    private String defaultModel = "cheap-model";
    private Map<String, ServiceConfig> services = new HashMap<>();

    @Data
    public static class Gateway {
        private String baseUrl;
        private String apiKey;
        private Integer defaultMaxTokens = 8192;
        private Duration defaultTimeout = Duration.ofSeconds(60);
        private Boolean logRequests = true;
        private Boolean logResponses = true;
    }

    @Data
    public static class ServiceConfig {
        private String model;
        private Integer maxTokens;
        private Duration timeout;
    }
}
```

### 4. HigressAiModelProvider（基础设施层 - 实现）

核心实现类，负责：
- 读取配置构建模型实例
- 缓存模型实例（避免每次请求都创建）
- 监听 Nacos 配置变更，清空缓存触发重建

```java
package com.dango.aicodegenerate.config;

@Component
@Slf4j
public class HigressAiModelProvider implements AiModelProvider {

    private final AiGatewayProperties properties;
    private final List<ChatModelListener> listeners;
    private final AsyncTaskExecutor streamingExecutor;

    // 缓存：按 AiServiceType 缓存模型实例
    private final ConcurrentHashMap<AiServiceType, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AiServiceType, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    @Override
    public ChatModel getChatModel(AiServiceType serviceType) {
        return chatModelCache.computeIfAbsent(serviceType, this::buildChatModel);
    }

    @Override
    public StreamingChatModel getStreamingChatModel(AiServiceType serviceType) {
        return streamingModelCache.computeIfAbsent(serviceType, this::buildStreamingChatModel);
    }

    private ChatModel buildChatModel(AiServiceType serviceType) {
        var gw = properties.getGateway();
        var svc = properties.getServices().getOrDefault(serviceType.getConfigKey(), new ServiceConfig());
        String modelName = svc.getModel() != null ? svc.getModel() : properties.getDefaultModel();
        int maxTokens = svc.getMaxTokens() != null ? svc.getMaxTokens() : gw.getDefaultMaxTokens();
        Duration timeout = svc.getTimeout() != null ? svc.getTimeout() : gw.getDefaultTimeout();

        log.info("构建 ChatModel: service={}, model={}, maxTokens={}, timeout={}",
                serviceType.getConfigKey(), modelName, maxTokens, timeout);

        var builder = OpenAiChatModel.builder()
                .baseUrl(gw.getBaseUrl())
                .apiKey(gw.getApiKey())
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(gw.getLogRequests())
                .logResponses(gw.getLogResponses());
        if (listeners != null && !listeners.isEmpty()) {
            builder.listeners(listeners);
        }
        return builder.build();
    }

    private StreamingChatModel buildStreamingChatModel(AiServiceType serviceType) {
        // 类似 buildChatModel，额外注入 streamingExecutor
        // ...
    }

    /**
     * 监听 Nacos 配置变更（Spring Cloud 配置刷新事件）
     * 清空模型缓存，下次调用时用新配置重建
     */
    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigChange(EnvironmentChangeEvent event) {
        Set<String> changedKeys = event.getKeys();
        boolean aiConfigChanged = changedKeys.stream()
                .anyMatch(key -> key.startsWith("ai."));
        if (aiConfigChanged) {
            log.info("检测到 AI 配置变更，清空模型缓存。变更的 key: {}", changedKeys);
            chatModelCache.clear();
            streamingModelCache.clear();
        }
    }
}
```

## Nacos 热更新方案

### 更新链路

```
Nacos 修改 shared-ai.yml
    ↓
Spring Cloud Nacos Config 检测到变更
    ↓
触发 EnvironmentChangeEvent
    ↓
AiGatewayProperties 的字段自动更新（@ConfigurationProperties 绑定）
    ↓
HigressAiModelProvider.onConfigChange() 监听到事件
    ↓
清空 chatModelCache / streamingModelCache
    ↓
下次 AI 服务调用时 computeIfAbsent → 用新配置重建模型实例
```

### 关键点

1. **`@ConfigurationProperties` 天然支持 Nacos 刷新**：Spring Cloud Alibaba Nacos Config 会自动更新 `@ConfigurationProperties` bean 的字段值，不需要 `@RefreshScope`
2. **`EnvironmentChangeEvent`**：Spring Cloud 配置变更时自动触发，包含变更的 key 列表
3. **懒重建**：清空缓存后不立即重建，等下次请求时按新配置构建，避免浪费
4. **AI 服务工厂的 Caffeine 缓存也需要清空**：`AiCodeGeneratorServiceFactory` 等工厂缓存了 AI 服务实例（内含旧模型），配置变更时也要清空

### AI 服务工厂缓存清空

在各工厂中监听同一个事件，或者抽取一个公共的缓存管理方法：

```java
// AiCodeGeneratorServiceFactory 中
@EventListener(EnvironmentChangeEvent.class)
public void onConfigChange(EnvironmentChangeEvent event) {
    if (event.getKeys().stream().anyMatch(k -> k.startsWith("ai."))) {
        log.info("AI 配置变更，清空 AI 服务实例缓存");
        serviceCache.invalidateAll();
    }
}
```

## YAML 配置结构（Nacos shared-ai.yml）

```yaml
ai:
  gateway:
    base-url: http://localhost:8082/v1
    api-key: ${HIGRESS_API_KEY:higress}
    default-max-tokens: 8192
    default-timeout: 60s
    log-requests: true
    log-responses: true

  # 全局默认模型（测试阶段全部用 cheap-model）
  default-model: cheap-model

  services:
    code-generator:
      model: cheap-model
      max-tokens: 65536
      timeout: 300s
    code-modifier:
      model: cheap-model
      max-tokens: 65536
      timeout: 300s
    code-fixer:
      model: cheap-model
      timeout: 120s
    qa:
      model: cheap-model
      timeout: 120s
    animation-advisor:
      model: cheap-model
      timeout: 120s
    modification-planner:
      model: cheap-model
      timeout: 120s
    app-info-generator:
      model: cheap-model
    intent-classifier:
      model: cheap-model
    feature-analyzer:
      model: cheap-model
    code-quality-check:
      model: cheap-model
    image-collection:
      model: cheap-model
```

## Higress 控制台配置（测试阶段）

### AI Provider

| Provider 名称 | 类型 | Base URL | API Key |
|-------------|------|----------|---------|
| deepseek | OpenAI 兼容 | `https://api.deepseek.com` | 你的 DeepSeek key |

### AI 路由规则

| 路由名称 | 匹配模型名 | 目标 Provider | 目标模型 |
|---------|-----------|-------------|---------|
| cheap-model | `cheap-model` | deepseek | `deepseek-chat` |

后续扩展时添加更多 Provider 和路由：
- `reasoning-model` → Anthropic → `claude-opus-4-6`
- `standard-model` → 代理A → `claude-sonnet-4-6`

## 需要删除的文件（9个）

### Anthropic 配置（3个）
- `config/AnthropicChatModelConfig.java`
- `config/AnthropicReasoningStreamingChatModelConfig.java`
- `config/AnthropicOrdinaryStreamingChatModelConfig.java`

### OpenAI 配置（3个）
- `config/OpenAiChatModelConfig.java`
- `config/OpenAiStreamingChatModelConfig.java`
- `config/ReasoningStreamingChatModelConfig.java`

### Failover（3个）
- `config/failover/FailoverModelConfig.java`
- `config/failover/FailoverChatModel.java`
- `config/failover/FailoverStreamingChatModel.java`

## 需要新增的文件（3个）

| 文件 | 层级 | 职责 |
|------|------|------|
| `model/AiServiceType.java` | 领域层 | AI 服务类型枚举 |
| `model/AiModelProvider.java` | 领域层 | AI 模型提供者接口 |
| `config/AiGatewayProperties.java` | 基础设施层 | YAML 配置绑定 |
| `config/HigressAiModelProvider.java` | 基础设施层 | 模型构建 + 缓存 + 热更新 |

## 需要修改的文件（11个工厂）

所有工厂改为注入 `AiModelProvider` 接口 + 使用 `AiServiceType` 枚举：

| 工厂 | 改造要点 |
|------|---------|
| `AiCodeGeneratorServiceFactory` | `reasoningStreamingChatModel` → `aiModelProvider.getStreamingChatModel(CODE_GENERATOR)` + 增加缓存清空监听 |
| `AiCodeModifierServiceFactory` | 同上，使用 `CODE_MODIFIER` |
| `AiCodeFixerServiceFactory` | `streamingChatModel` → `aiModelProvider.getStreamingChatModel(CODE_FIXER)` + 增加缓存清空监听 |
| `AiQAServiceFactory` | `streamingChatModel` → `aiModelProvider.getStreamingChatModel(QA)` |
| `AiAnimationAdvisorServiceFactory` | `streamingChatModel` → `aiModelProvider.getStreamingChatModel(ANIMATION_ADVISOR)` |
| `AiModificationPlannerServiceFactory` | `ordinaryChatModel` → `aiModelProvider.getChatModel(MODIFICATION_PLANNER)` |
| `AiAppInfoGeneratorServiceFactory` | `chatModel` → `aiModelProvider.getChatModel(APP_INFO_GENERATOR)` |
| `AiIntentClassifierServiceFactory` | `chatModel` → `aiModelProvider.getChatModel(INTENT_CLASSIFIER)` |
| `AiFeatureAnalyzerServiceFactory` | `chatModel` → `aiModelProvider.getChatModel(FEATURE_ANALYZER)` |
| `CodeQualityCheckServiceFactory` | `chatModel` → `aiModelProvider.getChatModel(CODE_QUALITY_CHECK)` |
| `ImageCollectionServiceFactory` | `chatModel` → `aiModelProvider.getChatModel(IMAGE_COLLECTION)` |

## 迁移策略

### 步骤

1. **Higress AI Gateway 配置** — 在控制台添加 DeepSeek Provider + cheap-model 路由
2. **curl 验证** — 直接请求 Higress `/v1/chat/completions` 确认 cheap-model 路由正常
3. **新增领域层文件** — `AiServiceType`、`AiModelProvider`
4. **新增基础设施层文件** — `AiGatewayProperties`、`HigressAiModelProvider`
5. **修改 11 个工厂** — 注入 `AiModelProvider`，使用枚举
6. **删除旧文件** — 6 个 Config 类 + 3 个 Failover 类
7. **更新 Nacos shared-ai.yml** — 替换为新的配置结构
8. **集成测试** — 验证代码生成、QA、修改等链路
9. **Nacos 热更新测试** — 在 Nacos 改 model 名，验证不重启生效

### 回滚方案

用 git revert 回退代码改动 + Nacos 恢复旧配置即可。

### 风险点

- 流式响应兼容性：Higress 转换协议时是否影响 SSE 流
- 超时配置：Higress 路由超时需 > 客户端超时
- 首次测试全部用 cheap-model，降低风险

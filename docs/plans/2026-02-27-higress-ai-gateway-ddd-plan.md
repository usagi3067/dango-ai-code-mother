# Higress AI Gateway DDD 集成实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 LangChain4j AI 模型调用统一接入 Higress AI Gateway，采用 DDD 分层设计，支持按服务粒度选模型和 Nacos 热更新。

**Architecture:** 领域层定义 `AiModelProvider` 接口和 `AiServiceType` 枚举，基础设施层通过 `HigressAiModelProvider` 实现，读取 `AiGatewayProperties` 配置，按服务名构建模型实例并缓存。监听 `EnvironmentChangeEvent` 实现 Nacos 热更新。

**Tech Stack:** Java 21, Spring Boot 3.5, LangChain4j 1.11.0-beta19, Higress AI Gateway, Nacos, Spring Cloud Alibaba

---

### Task 1: 新增领域层 — AiServiceType 枚举 + AiModelProvider 接口

**Files:**
- Create: `backend/ai/src/main/java/com/dango/aicodegenerate/model/AiServiceType.java`
- Create: `backend/ai/src/main/java/com/dango/aicodegenerate/model/AiModelProvider.java`

**Step 1: 创建 AiServiceType 枚举**

```java
package com.dango.aicodegenerate.model;

import lombok.Getter;

@Getter
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
}
```

**Step 2: 创建 AiModelProvider 接口**

```java
package com.dango.aicodegenerate.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public interface AiModelProvider {
    ChatModel getChatModel(AiServiceType serviceType);
    StreamingChatModel getStreamingChatModel(AiServiceType serviceType);
}
```

**Step 3: 编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn compile -pl ai -am -q`
Expected: BUILD SUCCESS

**Step 4: 提交**

```bash
git add backend/ai/src/main/java/com/dango/aicodegenerate/model/AiServiceType.java backend/ai/src/main/java/com/dango/aicodegenerate/model/AiModelProvider.java
git commit -m "feat: 新增 AiServiceType 枚举和 AiModelProvider 接口（领域层）"
```

---

### Task 2: 新增基础设施层 — AiGatewayProperties 配置绑定

**Files:**
- Create: `backend/ai/src/main/java/com/dango/aicodegenerate/config/AiGatewayProperties.java`

**Step 1: 创建 AiGatewayProperties**

```java
package com.dango.aicodegenerate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiGatewayProperties {

    private Gateway gateway = new Gateway();
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

**Step 2: 编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn compile -pl ai -am -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add backend/ai/src/main/java/com/dango/aicodegenerate/config/AiGatewayProperties.java
git commit -m "feat: 新增 AiGatewayProperties 配置绑定类（基础设施层）"
```

---

### Task 3: 新增基础设施层 — HigressAiModelProvider 实现

**Files:**
- Create: `backend/ai/src/main/java/com/dango/aicodegenerate/config/HigressAiModelProvider.java`

**Step 1: 创建 HigressAiModelProvider**

注意事项：
- 构造函数注入 `AiGatewayProperties`、`List<ChatModelListener>`（可选）、`AsyncTaskExecutor`（可选，`@Qualifier("streamingContextPropagatingExecutor")`）
- 使用 `ConcurrentHashMap` 缓存模型实例
- 监听 `EnvironmentChangeEvent` 清空缓存实现 Nacos 热更新
- StreamingChatModel 构建时需注入 `streamingExecutor` 以支持上下文传播

```java
package com.dango.aicodegenerate.config;

import com.dango.aicodegenerate.model.AiModelProvider;
import com.dango.aicodegenerate.model.AiServiceType;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class HigressAiModelProvider implements AiModelProvider {

    private final AiGatewayProperties properties;
    private final List<ChatModelListener> listeners;
    private final AsyncTaskExecutor streamingExecutor;

    private final ConcurrentHashMap<AiServiceType, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AiServiceType, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    public HigressAiModelProvider(
            AiGatewayProperties properties,
            @Autowired(required = false) List<ChatModelListener> listeners,
            @Autowired(required = false) @Qualifier("streamingContextPropagatingExecutor") AsyncTaskExecutor streamingExecutor
    ) {
        this.properties = properties;
        this.listeners = listeners;
        this.streamingExecutor = streamingExecutor;
    }

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
        var svc = getServiceConfig(serviceType);
        String modelName = resolveModelName(svc);
        int maxTokens = svc.getMaxTokens() != null ? svc.getMaxTokens() : gw.getDefaultMaxTokens();
        Duration timeout = svc.getTimeout() != null ? svc.getTimeout() : gw.getDefaultTimeout();

        log.info("构建 ChatModel: service={}, model={}, maxTokens={}, timeout={}s",
                serviceType.getConfigKey(), modelName, maxTokens, timeout.toSeconds());

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
        var gw = properties.getGateway();
        var svc = getServiceConfig(serviceType);
        String modelName = resolveModelName(svc);
        int maxTokens = svc.getMaxTokens() != null ? svc.getMaxTokens() : gw.getDefaultMaxTokens();

        log.info("构建 StreamingChatModel: service={}, model={}, maxTokens={}",
                serviceType.getConfigKey(), modelName, maxTokens);

        var builder = OpenAiStreamingChatModel.builder()
                .baseUrl(gw.getBaseUrl())
                .apiKey(gw.getApiKey())
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(gw.getLogRequests())
                .logResponses(gw.getLogResponses());
        if (streamingExecutor != null) {
            builder.httpClientBuilder(SpringRestClient.builder().streamingRequestExecutor(streamingExecutor));
        }
        if (listeners != null && !listeners.isEmpty()) {
            builder.listeners(listeners);
        }
        return builder.build();
    }

    private AiGatewayProperties.ServiceConfig getServiceConfig(AiServiceType serviceType) {
        return properties.getServices()
                .getOrDefault(serviceType.getConfigKey(), new AiGatewayProperties.ServiceConfig());
    }

    private String resolveModelName(AiGatewayProperties.ServiceConfig svc) {
        return svc.getModel() != null ? svc.getModel() : properties.getDefaultModel();
    }

    /**
     * 监听 Nacos 配置变更，清空模型缓存
     * 下次调用时用新配置重建模型实例
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

**Step 2: 编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn compile -pl ai -am -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add backend/ai/src/main/java/com/dango/aicodegenerate/config/HigressAiModelProvider.java
git commit -m "feat: 新增 HigressAiModelProvider（基础设施层，含 Nacos 热更新）"
```

---

### Task 4: 改造 11 个 AI 服务工厂 — 注入 AiModelProvider

**Files (全部在 `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/ai/factory/` 下):**
- Modify: `AiCodeGeneratorServiceFactory.java`
- Modify: `AiCodeModifierServiceFactory.java`
- Modify: `AiCodeFixerServiceFactory.java`
- Modify: `AiQAServiceFactory.java`
- Modify: `AiAnimationAdvisorServiceFactory.java`
- Modify: `AiModificationPlannerServiceFactory.java`
- Modify: `AiAppInfoGeneratorServiceFactory.java`
- Modify: `AiIntentClassifierServiceFactory.java`
- Modify: `AiFeatureAnalyzerServiceFactory.java`
- Modify: `CodeQualityCheckServiceFactory.java`
- Modify: `ImageCollectionServiceFactory.java`

**Step 1: 改造各工厂**

每个工厂的改造模式相同：
1. 将 `@Resource private StreamingChatModel xxx` 或 `@Resource private ChatModel xxx` 替换为 `@Resource private AiModelProvider aiModelProvider`
2. 在 `AiServices.builder()` 中使用 `aiModelProvider.getChatModel(AiServiceType.XXX)` 或 `aiModelProvider.getStreamingChatModel(AiServiceType.XXX)`
3. 有 Caffeine 缓存的工厂增加 `@EventListener(EnvironmentChangeEvent.class)` 监听，配置变更时清空缓存
4. 添加 `import com.dango.aicodegenerate.model.AiModelProvider` 和 `import com.dango.aicodegenerate.model.AiServiceType`

**改造清单：**

**AiCodeGeneratorServiceFactory** — 使用 StreamingChatModel + CODE_GENERATOR：
```java
// 替换：
@Resource
private StreamingChatModel reasoningStreamingChatModel;

// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换 createService 方法中：
.streamingChatModel(reasoningStreamingChatModel)
// 为：
.streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.CODE_GENERATOR))

// 新增 Nacos 热更新监听：
@EventListener(EnvironmentChangeEvent.class)
public void onConfigChange(EnvironmentChangeEvent event) {
    if (event.getKeys().stream().anyMatch(k -> k.startsWith("ai."))) {
        log.info("AI 配置变更，清空代码生成服务缓存");
        serviceCache.invalidateAll();
    }
}
```

**AiCodeModifierServiceFactory** — 使用 StreamingChatModel + CODE_MODIFIER：
```java
// 替换：
@Resource
private StreamingChatModel reasoningStreamingChatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.streamingChatModel(reasoningStreamingChatModel)
// 为：
.streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.CODE_MODIFIER))

// 新增 Nacos 热更新监听（同 AiCodeGeneratorServiceFactory 模式）
```

**AiCodeFixerServiceFactory** — 使用 StreamingChatModel + CODE_FIXER：
```java
// 替换：
@Resource
private StreamingChatModel streamingChatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.streamingChatModel(streamingChatModel)
// 为：
.streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.CODE_FIXER))

// 新增 Nacos 热更新监听（同上）
```

**AiQAServiceFactory** — 使用 StreamingChatModel + QA：
```java
// 替换：
@Resource
private StreamingChatModel streamingChatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.streamingChatModel(streamingChatModel)
// 为：
.streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.QA))
```

**AiAnimationAdvisorServiceFactory** — 使用 StreamingChatModel + ANIMATION_ADVISOR：
```java
// 替换：
@Resource
private StreamingChatModel streamingChatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换两处：
.streamingChatModel(streamingChatModel)
// 为：
.streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.ANIMATION_ADVISOR))
```

**AiModificationPlannerServiceFactory** — 使用 ChatModel + MODIFICATION_PLANNER：
```java
// 替换：
@Resource
private ChatModel ordinaryChatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.chatModel(ordinaryChatModel)
// 为：
.chatModel(aiModelProvider.getChatModel(AiServiceType.MODIFICATION_PLANNER))
```

**AiAppInfoGeneratorServiceFactory** — 使用 ChatModel + APP_INFO_GENERATOR：
```java
// 注意：此工厂是 @Configuration + @Bean 模式
// 替换：
@Resource
private ChatModel chatModel;

@Bean
public AiAppInfoGeneratorService aiAppInfoGeneratorService(ChatModel chatModel) {
    return AiServices.builder(AiAppInfoGeneratorService.class)
            .chatModel(chatModel)
            .build();
}

// 为：
@Resource
private AiModelProvider aiModelProvider;

@Bean
public AiAppInfoGeneratorService aiAppInfoGeneratorService() {
    return AiServices.builder(AiAppInfoGeneratorService.class)
            .chatModel(aiModelProvider.getChatModel(AiServiceType.APP_INFO_GENERATOR))
            .build();
}
```

**AiIntentClassifierServiceFactory** — 使用 ChatModel + INTENT_CLASSIFIER：
```java
// 替换：
@Resource
private ChatModel chatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.chatModel(chatModel)
// 为：
.chatModel(aiModelProvider.getChatModel(AiServiceType.INTENT_CLASSIFIER))
```

**AiFeatureAnalyzerServiceFactory** — 使用 ChatModel + FEATURE_ANALYZER：
```java
// 注意：此工厂是 @Configuration + @Bean 模式
// 替换：
@Resource
private ChatModel chatModel;

@Bean
public AiFeatureAnalyzerService aiFeatureAnalyzerService(ChatModel chatModel) {
// 为：
@Resource
private AiModelProvider aiModelProvider;

@Bean
public AiFeatureAnalyzerService aiFeatureAnalyzerService() {
    return AiServices.builder(AiFeatureAnalyzerService.class)
            .chatModel(aiModelProvider.getChatModel(AiServiceType.FEATURE_ANALYZER))
            .build();
}
```

**CodeQualityCheckServiceFactory** — 使用 ChatModel + CODE_QUALITY_CHECK：
```java
// 注意：此工厂是 @Configuration + @Bean 模式
// 替换：
@Resource
private ChatModel chatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换：
.chatModel(chatModel)
// 为：
.chatModel(aiModelProvider.getChatModel(AiServiceType.CODE_QUALITY_CHECK))
```

**ImageCollectionServiceFactory** — 使用 ChatModel + IMAGE_COLLECTION：
```java
// 注意：此工厂是 @Configuration + @Bean 模式，有两个 @Bean 方法
// 替换：
@Resource
private ChatModel chatModel;
// 为：
@Resource
private AiModelProvider aiModelProvider;

// 替换两处：
.chatModel(chatModel)
// 为：
.chatModel(aiModelProvider.getChatModel(AiServiceType.IMAGE_COLLECTION))
```

**Step 2: 编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn compile -pl app/app-service -am -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/domain/codegen/ai/factory/
git commit -m "refactor: 11 个 AI 服务工厂改为注入 AiModelProvider 接口"
```

---

### Task 5: 删除旧的配置类和 Failover 机制

**Files (全部在 `backend/ai/src/main/java/com/dango/aicodegenerate/config/` 下):**
- Delete: `AnthropicChatModelConfig.java`
- Delete: `AnthropicReasoningStreamingChatModelConfig.java`
- Delete: `AnthropicOrdinaryStreamingChatModelConfig.java`
- Delete: `OpenAiChatModelConfig.java`
- Delete: `OpenAiStreamingChatModelConfig.java`
- Delete: `ReasoningStreamingChatModelConfig.java`
- Delete: `failover/FailoverModelConfig.java`
- Delete: `failover/FailoverChatModel.java`
- Delete: `failover/FailoverStreamingChatModel.java`
- Delete: `failover/` 目录

**保留的文件：**
- `StreamingContextPropagationConfig.java` — 流式线程池上下文传播，仍然需要
- `RedisChatMemoryStoreConfig.java` — Redis 对话记忆存储，仍然需要

**Step 1: 删除文件**

```bash
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/AnthropicChatModelConfig.java
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/AnthropicReasoningStreamingChatModelConfig.java
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/AnthropicOrdinaryStreamingChatModelConfig.java
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/OpenAiChatModelConfig.java
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/OpenAiStreamingChatModelConfig.java
rm backend/ai/src/main/java/com/dango/aicodegenerate/config/ReasoningStreamingChatModelConfig.java
rm -r backend/ai/src/main/java/com/dango/aicodegenerate/config/failover/
```

**Step 2: 编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn compile -pl app/app-service -am -q`
Expected: BUILD SUCCESS（如果有编译错误说明还有其他地方引用了旧的类，需要排查）

**Step 3: 提交**

```bash
git add -A backend/ai/src/main/java/com/dango/aicodegenerate/config/
git commit -m "refactor: 删除 6 个旧模型配置类和 3 个 Failover 类"
```

---

### Task 6: 更新 Nacos 配置（shared-ai.yml）

**操作方式：** 在 Nacos 控制台手动修改 `shared-ai.yml` 配置

**新配置内容：**

```yaml
ai:
  gateway:
    base-url: http://localhost:8082/v1
    api-key: ${HIGRESS_API_KEY:higress}
    default-max-tokens: 8192
    default-timeout: 60s
    log-requests: true
    log-responses: true

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

**注意：** 删除旧的 `ai.anthropic.*`、`langchain4j.open-ai.*`、`ai.provider` 相关配置。

---

### Task 7: 配置 Higress AI Gateway

**操作方式：** 在 Higress 控制台（http://localhost:8001）手动配置

**Step 1: 添加 AI Provider**

在 Higress 控制台 → AI 服务提供者 → 新增：
- 名称：`deepseek`
- 类型：OpenAI 兼容
- Base URL：`https://api.deepseek.com`
- API Key：你的 DeepSeek API Key

**Step 2: 添加 AI 路由**

在 Higress 控制台 → AI 路由 → 新增：
- 路由名称：`cheap-model`
- 匹配条件：model 字段 = `cheap-model`
- 目标 Provider：deepseek
- 目标模型：`deepseek-chat`
- 超时：360s

**Step 3: curl 验证 Higress 路由**

```bash
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer higress" \
  -d '{
    "model": "cheap-model",
    "messages": [{"role": "user", "content": "hello"}],
    "max_tokens": 50
  }'
```

Expected: 返回 DeepSeek 的正常响应

---

### Task 8: 集成测试 — 启动服务验证全链路

**Step 1: 启动 app-service**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn clean package -pl app/app-service -am -DskipTests && java -jar app/app-service/target/app-service-1.0-SNAPSHOT.jar`

Expected:
- 启动日志中看到 `AiGatewayProperties` 加载的配置
- 没有 Bean 创建失败的错误

**Step 2: 触发一次代码生成请求**

通过前端或 API 触发一次简单的代码生成，检查：
- 日志中出现 `构建 StreamingChatModel: service=code-generator, model=cheap-model`
- 请求成功到达 Higress → DeepSeek
- 流式响应正常返回

**Step 3: Nacos 热更新测试**

在 Nacos 修改 `shared-ai.yml` 中某个服务的 model 名称（比如改为 `cheap-model-v2`），观察：
- 日志中出现 `检测到 AI 配置变更，清空模型缓存`
- 下次请求时重新构建模型实例
- 注意：如果 Higress 没有 `cheap-model-v2` 的路由，请求会失败，这是预期的——验证的是热更新机制生效

---

### Task 9: 清理与收尾

**Step 1: 检查是否有遗留引用**

```bash
cd /Users/dango/Documents/code/dango-ai-code-mother/backend
grep -r "AnthropicChatModel\|AnthropicStreamingChatModel\|FailoverChatModel\|FailoverStreamingChatModel\|FailoverModelConfig" --include="*.java" .
```

Expected: 无匹配结果

**Step 2: 检查 pom.xml 依赖**

确认 `backend/ai/pom.xml` 中 `langchain4j-anthropic-spring-boot-starter` 依赖是否还需要保留。如果未来还可能直连 Anthropic 则保留；如果完全走 Higress 则可以删除。

建议：暂时保留，等 Higress 集成稳定后再清理。

**Step 3: 最终编译验证**

Run: `cd /Users/dango/Documents/code/dango-ai-code-mother/backend && mvn clean compile -q`
Expected: BUILD SUCCESS

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 AI 代码生成平台，采用前后端分离的微服务架构。用户可以通过自然语言描述生成 Web 应用代码。

## 技术栈

**前端**: Vue 3 + TypeScript + Vite + Ant Design Vue + Pinia

**后端**: Java 21 + Spring Boot 3.5 + Spring Cloud + Dubbo 3.3 + MyBatis-Flex + MySQL + Redis + LangChain4j

**微服务基础设施**:
- **Nacos** (v2.4.3): 服务注册与配置中心 - 端口 8848, 9848
- **Higress**: API 网关 - 端口 8001, 8080, 8443
- **Apache SkyWalking** (10.3.0): 分布式追踪和 APM - 端口 8080
- **Elasticsearch** (8.19.10) + **Kibana** (8.19.10): 日志存储和可视化 - 端口 9200, 5601
- **Prometheus** (v3.9.1) + **Grafana** (12.3.2): 监控和可视化 - 端口 9090, 3000

## 常用命令

### 前端 (frontend/)

```bash
npm install              # 安装依赖
npm run dev              # 启动开发服务器
npm run build            # 生产构建
npm run type-check       # TypeScript 类型检查
npm run lint             # ESLint 检查
npm run openapi2ts       # 从 OpenAPI 生成 TypeScript 类型
```

### 后端 (backend/)

```bash
mvn clean install                              # 构建所有模块
mvn clean package -pl user/user-service -am   # 构建用户服务
mvn clean package -pl app/app-service -am     # 构建应用服务
```

运行服务:
- 用户服务: `java -jar user/user-service/target/user-service-1.0-SNAPSHOT.jar` (端口 8123)
- 应用服务: `java -jar app/app-service/target/app-service-1.0-SNAPSHOT.jar` (端口 8124)

## 架构概览

### 微服务基础设施

项目采用完整的微服务治理体系：

- **服务注册与发现**: Nacos 作为注册中心，所有微服务启动时注册到 Nacos
- **API 网关**: Higress 作为统一入口，处理路由、限流、鉴权等
- **分布式追踪**: SkyWalking 收集服务调用链路，监控性能瓶颈
- **监控告警**: Prometheus 采集指标，Grafana 展示监控面板

### 后端微服务模块

- **user/**: 用户服务 - 认证、用户管理
  - `api/`: 接口定义 (DTO, VO, Entity, Service 接口)
  - `user-service/`: 服务实现
- **app/**: 应用服务 - 核心业务，AI 代码生成
  - `app-api/`: 接口定义
  - `app-service/`: 服务实现，包含 `core/` 目录下的 AI 代码生成核心逻辑
- **ai/**: AI 代码生成模块 - LangChain4j 集成，代码生成/修改/修复服务
- **screenshot/**: 截图服务
- **common/**: 公共模块 - 异常处理、工具类、配置

### 核心设计模式

- **Facade 模式**: `AiCodeGeneratorFacade`, `AppInfoGeneratorFacade` 封装 AI 生成流程
- **Template 模式**: `CodeFileSaverTemplate`, `CodeParser` 处理代码解析和保存
- **Strategy 模式**: `AiCodeGenTypeRoutingService` 路由不同代码生成类型
- **Stream 处理**: `JsonMessageStreamHandler` 处理 AI 流式响应

### 前端结构

- `src/pages/`: 页面组件 (HomePage, AppChatPage, AppEditPage 等)
- `src/components/`: 可复用组件
- `src/config/`: 配置 (routes.ts, env.ts, appTag.ts, codeGenType.ts)
- `src/stores/`: Pinia 状态管理

## 数据库

初始化脚本: `backend/sql/created_table.sql`

核心表: user, app, chat_history

## 配置文件

- 前端环境: `frontend/.env.development`
- 后端配置: `backend/*/src/main/resources/application.yml`, `application-local.yml`

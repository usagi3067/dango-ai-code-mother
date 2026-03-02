# CodeForge AI - 快速部署指南

本指南适用于单机快速部署，所有服务运行在同一台机器上。

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 6GB 可用内存
- 至少 10GB 可用磁盘空间

## 部署步骤

### 第一步：准备环境

```bash
# 1. 克隆项目
git clone https://github.com/your-username/CodeForge-AI.git
cd CodeForge-AI

# 2. 复制环境变量配置
cp .env.quickstart .env

# 3. 编辑 .env 文件（可选）
# 如需修改数据库密码、Nacos Token 等，请编辑 .env 文件
vim .env
```

### 第二步：启动基础设施

```bash
# 启动 MySQL、Redis、Nacos（按顺序启动，等待健康检查通过）
docker-compose -f docker-compose.quickstart.yml up -d mysql redis nacos

# 查看启动日志，等待 Nacos 完全启动（约 60 秒）
docker-compose -f docker-compose.quickstart.yml logs -f nacos

# 看到 "Nacos started successfully" 后按 Ctrl+C 退出日志查看
```

### 第三步：配置 Nacos

Nacos 启动后，需要导入配置文件：

```bash
# 访问 Nacos 控制台
open http://localhost:8848/nacos

# 登录信息：
# 用户名: nacos
# 密码: nacos
```

**导入配置文件**：

配置文件位于 `nacos-configs/quickstart/` 目录，已脱敏处理。

详细的配置导入指南请参考：[nacos-configs/quickstart/README.md](./nacos-configs/quickstart/README.md)

**快速导入步骤**：

1. 登录 Nacos 控制台（http://localhost:8848/nacos，用户名/密码：nacos/nacos）
2. 进入「配置管理」→「配置列表」
3. 点击右上角「+」创建配置，逐个导入以下文件：

**SHARED_GROUP**（必需）：
- `shared-datasource.yml` - 修改 MySQL/Redis 连接信息
- `shared-dubbo.yml` - 修改 Nacos 地址
- `shared-sa-token.yml` - 无需修改
- `shared-mybatis.yml` - 无需修改
- `shared-common.yml` - 无需修改
- `shared-ai.yml` - **重要**：修改 AI Gateway 地址和 API Key

**DEFAULT_GROUP**（必需）：
- `dango-ai-code-user.yml` - 无需修改
- `dango-ai-code-app.yml` - 可选修改部署域名
- `dango-ai-code-screenshot.yml` - 无需修改

**关键配置项**（必须修改）：

1. `shared-datasource.yml`：
   ```yaml
   url: jdbc:mysql://mysql:3306/dango_ai_code
   username: root
   password: root123456  # 与 .env 一致
   redis:
     host: redis
     password: redis123456  # 与 .env 一致
   ```

2. `shared-ai.yml`：
   ```yaml
   ai:
     gateway:
       base-url: https://your-ai-gateway.com/v1
       api-key: sk-your-api-key-here
   ```

### 第四步：启动 Higress 网关

```bash
# 启动 Higress
docker-compose -f docker-compose.quickstart.yml up -d higress

# 查看日志
docker-compose -f docker-compose.quickstart.yml logs -f higress
```

**配置 Higress 路由**：

1. 访问 Higress 控制台：http://localhost:8001
2. 登录密码：见 `.env` 文件中的 `HIGRESS_CONSOLE_PASSWORD`（默认 `admin123`）
3. 进入「路由配置」，添加以下路由：

**路由 1：用户服务**
- 路由名称：`user-service`
- 域名：`*`（所有域名）
- 路径：`/user/**`
- 目标服务：`user-service:8123`

**路由 2：应用服务**
- 路由名称：`app-service`
- 域名：`*`
- 路径：`/app/**`
- 目标服务：`app-service:8124`

**路由 3：静态资源**
- 路由名称：`static-resources`
- 域名：`*`
- 路径：`/static/**`
- 目标服务：`app-service:8124`

### 第五步：启动业务服务

```bash
# 启动所有业务服务
docker-compose -f docker-compose.quickstart.yml up -d user-service app-service screenshot-app

# 查看启动日志
docker-compose -f docker-compose.quickstart.yml logs -f user-service app-service

# 等待服务注册到 Nacos（约 30 秒）
# 可以在 Nacos 控制台的「服务管理」→「服务列表」中查看
```

### 第六步：启动前端

```bash
# 启动前端
docker-compose -f docker-compose.quickstart.yml up -d frontend

# 查看所有服务状态
docker-compose -f docker-compose.quickstart.yml ps
```

### 第七步：访问应用

- **前端应用**：http://localhost
- **Nacos 控制台**：http://localhost:8848/nacos
- **Higress 控制台**：http://localhost:8001

## 常见问题

### Q1: 服务启动失败，提示 OOM？

**原因**：Docker 可用内存不足。

**解决方案**：
增加 Docker 可用内存（Docker Desktop → Settings → Resources → Memory，建议至少 6GB）

### Q2: 业务服务无法连接 Nacos？

**原因**：Nacos 未完全启动或认证配置错误。

**解决方案**：
1. 检查 Nacos 日志：`docker-compose -f docker-compose.quickstart.yml logs nacos`
2. 确认 `.env` 中的 `NACOS_AUTH_TOKEN` 至少 32 位
3. 重启业务服务：`docker-compose -f docker-compose.quickstart.yml restart user-service app-service`

### Q3: AI 调用失败？

**原因**：AI Gateway 配置错误或 API Key 无效。

**解决方案**：
1. 登录 Nacos 控制台
2. 编辑 `shared-ai.yml` 配置（SHARED_GROUP）
3. 确认 `ai.gateway.base-url` 和 `ai.gateway.api-key` 配置正确
4. 重启 app-service：`docker-compose -f docker-compose.quickstart.yml restart app-service`

### Q4: 前端无法访问后端接口？

**原因**：Higress 路由配置错误或服务未注册。

**解决方案**：
1. 检查 Nacos 服务列表，确认服务已注册
2. 检查 Higress 路由配置是否正确
3. 查看 Higress 日志：`docker-compose -f docker-compose.quickstart.yml logs higress`

### Q5: 如何查看服务日志？

```bash
# 查看所有服务日志
docker-compose -f docker-compose.quickstart.yml logs -f

# 查看特定服务日志
docker-compose -f docker-compose.quickstart.yml logs -f app-service

# 查看最近 100 行日志
docker-compose -f docker-compose.quickstart.yml logs --tail=100 app-service
```

### Q6: 如何停止所有服务？

```bash
# 停止所有服务
docker-compose -f docker-compose.quickstart.yml down

# 停止并删除数据卷（会清空数据库）
docker-compose -f docker-compose.quickstart.yml down -v
```

### Q7: 如何重新构建镜像？

```bash
# 重新构建所有镜像
docker-compose -f docker-compose.quickstart.yml build --no-cache

# 重新构建特定服务
docker-compose -f docker-compose.quickstart.yml build --no-cache app-service
```

## 服务端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848, 9848 | 配置中心 + 服务注册 |
| Higress | 8001, 8080, 8443 | 网关控制台 + HTTP/HTTPS |
| user-service | 8123, 50051 | 用户服务 + Dubbo |
| app-service | 8124, 50053 | 应用服务 + Dubbo |
| screenshot-app | 8125, 50054 | 截图服务 + Dubbo |
| frontend | 80 | 前端 Nginx |

## 下一步

- 查看 [API 文档](http://localhost:8124/doc.html)
- 查看 [架构设计文档](./docs/plans/)
- 查看 [部署故障排查](./docs/deployment/deployment-troubleshooting.md)

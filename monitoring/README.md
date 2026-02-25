# 监控栈部署指南

NAS 上部署 Prometheus + Grafana + AlertManager，采集云服务器上 4 个微服务的指标。

## 快速开始

1. 复制环境变量文件并修改 Grafana 密码：

```bash
cp .env.example .env
# 编辑 .env，修改 GF_SECURITY_ADMIN_PASSWORD
```

2. 确认 prometheus.yml 中的目标 IP 正确（默认已配置）：
   - Machine A (腾讯云): 42.194.244.5:8124
   - Machine B (百度云): 106.13.14.164:8123/8125/8126

3. 启动监控栈：

```bash
docker compose up -d
```

4. 访问服务：
   - Grafana: http://NAS_IP:3000（默认账号 admin）
   - Prometheus: http://NAS_IP:9090
   - AlertManager: http://NAS_IP:9093

## 验证步骤

1. 检查 Prometheus targets: http://NAS_IP:9090/targets
   - 所有 4 个服务应显示 State=UP

2. 检查 Grafana dashboards: http://NAS_IP:3000
   - 应看到 3 个预置 Dashboard：JVM 监控、Spring Boot HTTP 监控、AI 调用监控

3. 检查 AlertManager: http://NAS_IP:9093

## 采集架构

```
NAS (Prometheus) --家庭宽带--> 腾讯云 42.194.244.5:8124 (app-service)
                 --家庭宽带--> 百度云 106.13.14.164:8123 (user-service)
                 --家庭宽带--> 百度云 106.13.14.164:8125 (screenshot-app)
                 --家庭宽带--> 百度云 106.13.14.164:8126 (supabase-service)
```

采集间隔 15s，走家庭宽带出口，不消耗花生壳穿透流量。

## 告警配置

默认告警规则已配置（见 prometheus/alert-rules.yml），通知渠道需手动配置：
编辑 alertmanager/alertmanager.yml，取消邮件通知的注释并填写实际值。

## 常用操作

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f prometheus
docker compose logs -f grafana

# 重启（修改配置后）
docker compose restart

# 热加载 Prometheus 配置（无需重启）
curl -X POST http://localhost:9090/-/reload
```

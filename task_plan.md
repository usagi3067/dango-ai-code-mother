# Dango AI Code 项目任务计划

## 当前状态: 无进行中任务

## 已完成的功能迭代

### 2026-02-15 ~ 02-16: Vue 项目生成稳定性改造 ✅
- Phase 1: 模板脚手架 + 构建驱动修复循环 (`2be402a`)
- Phase 2: 统一代码生成类型为 VUE_PROJECT (`d317fd2`)
- Phase 3: HTML 上传自动转换为 Vue 项目 (`06438bf`)
- 设计文档: `docs/plans/2026-02-15-vue-project-stability-design.md`

### 2026-02-18 ~ 02-19: DDD 领域驱动重构 ✅
- App 聚合根充血模型 + Repository 模式
- ChatHistory 聚合根充血模型 + Repository 模式
- Controller 瘦身，Application Service 用例方法
- 去除 IService 继承

### 2026-02-19: 基础设施文件保护 ✅
- 防止 AI 覆盖 index.html、package.json 等模板文件 (`1875fb2`)

### 2026-02-19: AI 进度输出格式化 ✅
- 修改/修复提示词增加子步骤进度格式
- 修复指南增加修复计划格式

### 2026-02-19: npm 依赖预构建 ✅
- NodeModulesPrebuilder 服务启动时预构建
- 符号链接共享 node_modules，跳过重复 npm install (`933a3bf`)

### 2026-02-19: MVP 功能选择 + 精简生成 ✅
- 新增功能分析 AI 服务（FeatureAnalyzerFacade）
- 前端 FeatureSelectionModal 组件
- 主页集成功能选择弹窗
- 代码生成提示词 MVP 优化（token 15000，文件 15 个）
- 设计文档: `docs/plans/2026-02-19-mvp-feature-selection-design.md`

### 2026-02-19: Ant Design Vue 集成 ✅
- 模板 package.json 加入 ant-design-vue 依赖
- 模板 main.js 全局注册 Antd
- 代码生成/修改/修复/规划提示词更新
- 组件速查表加入生成和修改提示词
- 构建验证通过

## 未提交的变更（工作区）

| 文件 | 说明 |
|------|------|
| RedisChatMemoryStoreConfig.java | Redis 聊天记忆配置 |
| DangoAiCodeAppApplication.java | 应用启动类 |
| App.java / ChatHistory.java | DDD 实体 |
| 5 个 ServiceFactory 类 | AI 服务工厂 |
| AppMapper.xml | MyBatis 映射 |
| SaTokenConfigure.java | Sa-Token 配置 |
| JedisBackedChatMemoryStore.java | 新增：Jedis 聊天记忆存储 |

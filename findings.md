# 发现与研究记录

## 设计文档参考
- 总体设计: [`docs/plans/2026-02-15-vue-project-stability-design.md`](docs/plans/2026-02-15-vue-project-stability-design.md)
- 实现计划: [`docs/plans/2026-02-15-vue-stability-phase1-plan.md`](docs/plans/2026-02-15-vue-stability-phase1-plan.md)
- Supabase 集成计划: [`docs/plans/2026-02-13-supabase-workflow-integration-plan.md`](docs/plans/2026-02-13-supabase-workflow-integration-plan.md)

## 设计文档核心决策摘要

### 为什么做这个改造（设计文档「背景」）
- AI 每次从零生成 package.json/vite.config.js 等基础设施文件，版本号、配置项容易出错
- 质检依赖"AI 审查 AI"，而非真实编译器验证
- 构建在质检循环之外，build 失败后无法自动修复
- 参考 bolt.new / lovable.dev: AI 不负责生成项目骨架，只负责填充业务代码

### 改造后的工作流（设计文档「改造后的完整工作流」）
创建模式:
```
ModeRouterNode → 创建子图(图片收集 → PromptEnhancer) → CodeGeneratorNode(先scaffold再AI生成) → BuildCheckNode(npm build循环修复) → END
```
修改模式:
```
ModeRouterNode → 修改子图(CodeReader → Planner → DB → Modifier) → BuildCheckNode(npm build循环修复) → END
```

### Supabase 兼容性（设计文档「改动 2」备注）
模板不含 Supabase 文件。数据库初始化 API（已实现）会在用户启用数据库时独立写入 `src/integrations/supabase/client.js` 并更新 `package.json`，与模板脚手架不冲突。

## 现有代码分析

### VueProjectBuilder 现状
- 位置: `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/core/builder/VueProjectBuilder.java`
- 当前 `buildProject()` 返回 `boolean`，不捕获错误详情
- 使用 `RuntimeUtil.exec()` 执行命令，超时: install 5min, build 3min
- 需要改造为返回 `BuildResult`，包含 stderr 信息

### CodeGenWorkflow 现状
- 质检子图: `CodeQualityCheckNode ←→ CodeFixerNode` 循环
- `ProjectBuilderNode` 在质检子图之后单独执行
- 问题: build 失败后没有修复机会
- 改造: 合并为 `BuildCheckNode ←→ CodeFixerNode` 循环

### CodeGeneratorNode 现状
- 从 context 读取 `generationType`（由 RouterNode 设置）
- 调用 `AiCodeGeneratorFacade.generateAndSaveCodeStream()`
- 需要在 AI 调用前插入脚手架复制

### 模板文件 classpath 读取
- Spring `ResourcePatternResolver` 可以用 `classpath:templates/vue-project/**` 模式
- 但 classpath 资源在 jar 包中时，`**` 模式可能有问题
- 备选方案: 硬编码文件列表（模板文件固定，不需要动态发现）

## 版本号验证
- 待 Task 1 Step 5 验证后记录实际可用的版本号

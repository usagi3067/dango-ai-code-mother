# Vue 项目生成稳定性改造 - 任务计划

## 状态: ✅ 全部完成

## 设计文档
- 总体设计: [`docs/plans/2026-02-15-vue-project-stability-design.md`](docs/plans/2026-02-15-vue-project-stability-design.md)
- 实现计划: [`docs/plans/2026-02-15-vue-stability-phase1-plan.md`](docs/plans/2026-02-15-vue-stability-phase1-plan.md)

## Phase 1: 模板脚手架 + 构建驱动修复循环 ✅

| Task | 描述 | 状态 |
|------|------|------|
| 1 | 创建 Vue 项目模板文件 + 验证可构建 | ✅ |
| 2 | 创建 VueProjectScaffoldService | ✅ |
| 3 | CodeGeneratorNode 调用脚手架 | ✅ |
| 4 | VueProjectBuilder 增强构建错误捕获 | ✅ |
| 5 | 创建 BuildCheckNode | ✅ |
| 6 | CodeGenWorkflow 用构建检查替换质量检查子图 | ✅ |
| 7 | 更新 Vue 项目生成 Prompt | ✅ |
| 8 | 更新 Vue 项目修复 Prompt | ✅ |
| 9 | 更新 Vue 项目修改 Prompt | ✅ |
| 10 | CodeFixerNode 适配构建错误 | ✅ |
| 11 | 最终验证 + 提交 (`2be402a`) | ✅ |

## Phase 2: 统一代码生成类型为 VUE_PROJECT ✅

| Task | 描述 | 状态 |
|------|------|------|
| 1 | 删除 RouterNode + 路由 Prompt | ✅ |
| 2 | 删除 HTML/MULTI_FILE Prompt 文件（6 个） | ✅ |
| 3 | 简化 AI Service 接口 | ✅ |
| 4 | 简化 AiCodeGeneratorFacade | ✅ |
| 5 | 删除 Parser + Saver 类（8 个文件） | ✅ |
| 6 | 简化 Factory 类（3 个） | ✅ |
| 7 | 简化 Node 类 | ✅ |
| 8 | 简化 AppServiceImpl | ✅ |
| 9 | 删除 AiCodeGenTypeRoutingService | ✅ |
| 10 | 清理辅助类 | ✅ |
| 11 | 更新测试 | ✅ |
| 12 | 编译验证 + 提交 (`d317fd2`) | ✅ |

## Phase 3: HTML 上传自动转换为 Vue 项目 ✅

| Task | 描述 | 状态 |
|------|------|------|
| 1 | 实现 HtmlToVueConverterService | ✅ |
| 2 | 修改 createAppFromHtml 流程 | ✅ |
| 3 | ModeRouterNode 检测裸 HTML 项目 | ✅ |
| 4 | 上传后自动触发 AI 转换（前端 autoSend） | ✅ |
| 5 | 编译验证 + 提交 (`06438bf`, `b0ce529`) | ✅ |

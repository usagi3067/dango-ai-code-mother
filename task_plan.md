# Vue 项目生成稳定性改造 Phase 1 - 任务计划

## 设计文档
- 总体设计: [`docs/plans/2026-02-15-vue-project-stability-design.md`](docs/plans/2026-02-15-vue-project-stability-design.md)
- 实现计划: [`docs/plans/2026-02-15-vue-stability-phase1-plan.md`](docs/plans/2026-02-15-vue-stability-phase1-plan.md)

## 目标（来自设计文档）
通过模板脚手架 + 构建驱动修复循环，确保 AI 生成的 Vue 项目能稳定通过 npm install && npm run build。

核心改动（设计文档 4 个改动点）：
1. **改动 2 - 模板脚手架**: 预置经过验证的 Vue3 项目骨架，AI 只生成业务文件
2. **改动 3 - 构建驱动修复循环**: 用真实 `npm run build` 编译错误替代 AI 质检
3. **改动 4 - Prompt 优化**: 告知 AI 已有模板、禁止修改基础设施文件
4. **改动 1 - 统一 VUE_PROJECT**: Phase 1 暂不执行，Phase 2 再做

## 阶段划分

### Phase A: 模板脚手架基础 (Task 1-3)
对应设计文档「改动 2：模板脚手架」

| Task | 描述 | 状态 | 设计文档对应 |
|------|------|------|------------|
| 1 | 创建 Vue 项目模板文件 + 验证可构建 | pending | 改动 2 - 模板内容 |
| 2 | 创建 VueProjectScaffoldService | pending | 改动 2 - 新增服务 |
| 3 | CodeGeneratorNode 调用脚手架 | pending | 改动 2 - 执行时机 |

### Phase B: 构建驱动修复循环 (Task 4-6)
对应设计文档「改动 3：构建驱动修复循环」

| Task | 描述 | 状态 | 设计文档对应 |
|------|------|------|------------|
| 4 | VueProjectBuilder 增强构建错误捕获 | pending | 改动 3 - 复用 VueProjectBuilder |
| 5 | 创建 BuildCheckNode | pending | 改动 3 - 新增节点 |
| 6 | CodeGenWorkflow 用构建检查替换质量检查子图 | pending | 改动 3 - 改造后的质检子图 |

### Phase C: Prompt 优化 + 适配 (Task 7-10)
对应设计文档「改动 4：Prompt 优化」

| Task | 描述 | 状态 | 设计文档对应 |
|------|------|------|------------|
| 7 | 更新 Vue 项目生成 Prompt | pending | 改动 4 - codegen-vue-project |
| 8 | 更新 Vue 项目修复 Prompt | pending | 改动 4 - codegen-fix-vue-project |
| 9 | 更新 Vue 项目修改 Prompt | pending | 改动 4 - codegen-modify-vue-project |
| 10 | CodeFixerNode 适配构建错误 | pending | 改动 3 - 修改节点 CodeFixerNode |

### Phase D: 收尾 (Task 11)

| Task | 描述 | 状态 |
|------|------|------|
| 11 | 最终验证 + 提交 | pending |

## 依赖关系
- Task 2 → Task 1（模板文件必须先存在）
- Task 3 → Task 2（脚手架服务必须先创建）
- Task 5 → Task 4（BuildCheckNode 需要 BuildResult）
- Task 6 → Task 5（工作流重构需要 BuildCheckNode）
- Task 7-10 可并行，但建议顺序执行
- Task 11 → 所有前置任务

## 决策记录
- 模板版本号需实际验证后确定（Task 1 Step 5）
- VueProjectBuilder.buildProject 返回类型从 boolean 改为 BuildResult
- Supabase 兼容性: 模板不含 Supabase 文件，数据库初始化 API 独立写入（见设计文档）

# 发现与研究记录

## 项目架构概览

### 代码生成工作流（当前）
创建模式:
```
ModeRouterNode → 创建子图(图片收集 → PromptEnhancer) → CodeGeneratorNode(scaffold + AI生成) → BuildCheckNode(npm build循环修复, 最多3次) → END
```
修改模式:
```
ModeRouterNode → 修改子图(CodeReader → Planner → DB → Modifier) → BuildCheckNode → END
```

### 技术栈
- 模板脚手架: Vue 3 + Vite + Vue Router 4 + Ant Design Vue 4
- AI 框架: LangChain4j（结构化输出 + 流式生成 + Tool 调用）
- 后端架构: DDD（聚合根 + Repository + Application Service）
- npm 优化: 预构建 node_modules + 符号链接

### 关键设计决策

1. **模板脚手架 vs AI 生成骨架**: AI 只负责业务代码，基础设施文件由模板提供
2. **构建驱动修复**: 用 npm build 真实编译器错误替代 AI 审查 AI
3. **MVP 功能选择**: 代码生成前让用户确认功能范围，避免生成多余内容
4. **Ant Design Vue 全局引入**: `app.use(Antd)` + `reset.css`，提示词含组件速查表
5. **npm 预构建**: 服务启动时后台预构建，符号链接共享，跳过重复安装

## 提示词文件清单

| 文件 | 用途 |
|------|------|
| codegen-vue-project-system-prompt.txt | 代码生成（含 Antd 速查表） |
| codegen-modify-vue-project-system-prompt.txt | 代码修改（含 Antd 速查表） |
| codegen-fix-vue-project-system-prompt.txt | 代码修复 |
| modification-planner-system-prompt.txt | 修改规划 |
| image-collection-plan-system-prompt.txt | 图片规划 |
| image-collection-system-prompt.txt | 图片收集 |
| database-analyzer-system-prompt.txt | 数据库分析 |
| app-info-generator-system-prompt.txt | 应用信息生成 |
| feature-analyzer-system-prompt.txt | 功能分析（新增） |

## 未提交变更备注

工作区有 11 个未暂存的修改文件，涉及：
- Redis 聊天记忆配置重构（RedisChatMemoryStoreConfig + JedisBackedChatMemoryStore）
- DDD 实体字段调整（App.java, ChatHistory.java）
- AI 服务工厂类调整（5 个 Factory）
- Sa-Token 配置更新
- AppMapper.xml 更新

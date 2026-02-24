# 应用卡片截图优化设计

## 问题

1. 截图只在部署后生成，不部署的应用没有封面图
2. 压缩质量 30%，卡片上模糊不清
3. 全页截图缩到 160px 高的卡片后，内容看不清，所有应用看起来大同小异

## 方案

### 1. 截图时机：生成完成后立即截图

- 在 AI 代码生成完成（SSE done 事件）后，用预览 URL 异步触发截图
- 预览 URL：`{serverHost}/api/static/{codeGenType}_{appId}/dist/index.html`
- 部署时不再重复截图

改动文件：`AppApplicationServiceImpl.java`

### 2. 截图裁剪与质量提升

- 视口尺寸：1280x720
- 裁剪策略：截取全页后，只保留顶部 50%（1280x360），聚焦 hero 区域
- 压缩质量：0.3 → 0.75
- 裁剪用 `BufferedImage.getSubimage()` 实现

改动文件：`WebScreenshotUtils.java`

### 3. 前端无改动

AppCard 展示逻辑不变，图片内容从全页缩略图变为顶部裁剪图。

## 改动范围

| 文件 | 改动内容 |
|------|----------|
| `WebScreenshotUtils.java` | 压缩质量提升、新增裁剪方法 |
| `AppApplicationServiceImpl.java` | 生成完成后触发异步截图 |

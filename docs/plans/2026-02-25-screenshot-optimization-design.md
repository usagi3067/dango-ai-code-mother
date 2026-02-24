# 应用卡片截图优化 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 优化应用卡片截图体验 — 生成完成后立即截图、裁剪顶部 hero 区域、提升压缩质量。

**Architecture:** 后端两处改动：(1) `WebScreenshotUtils` 增加裁剪逻辑并提升压缩质量；(2) `CodeGenApplicationServiceImpl` 在生成完成回调中触发异步截图。前端无改动。

**Tech Stack:** Java 21, Selenium WebDriver, BufferedImage, Spring Boot

---

### Task 1: WebScreenshotUtils — 调整视口尺寸

**Files:**
- Modify: `backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java:30-33`

**Step 1: 修改视口尺寸常量**

将 `DEFAULT_WIDTH` 从 1600 改为 1280，`DEFAULT_HEIGHT` 从 900 改为 720：

```java
static {
    final int DEFAULT_WIDTH = 1280;
    final int DEFAULT_HEIGHT = 720;
    webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
}
```

**Step 2: 提交**

```bash
git add backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java
git commit -m "refactor: 截图视口尺寸从 1600x900 调整为 1280x720"
```

---

### Task 2: WebScreenshotUtils — 提升压缩质量

**Files:**
- Modify: `backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java:146-154`

**Step 1: 修改压缩质量常量**

将 `COMPRESSION_QUALITY` 从 0.3f 改为 0.75f：

```java
private static void compressImage(String originalImagePath, String compressedImagePath) {
    final float COMPRESSION_QUALITY = 0.75f;
    try {
        ImgUtil.compress(
                FileUtil.file(originalImagePath),
                FileUtil.file(compressedImagePath),
                COMPRESSION_QUALITY
        );
    } catch (Exception e) {
        log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
    }
}
```

**Step 2: 提交**

```bash
git add backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java
git commit -m "refactor: 截图压缩质量从 0.3 提升到 0.75"
```

---

### Task 3: WebScreenshotUtils — 新增顶部裁剪逻辑

**Files:**
- Modify: `backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java`

**Step 1: 添加 import**

在文件顶部 import 区域添加：

```java
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
```

**Step 2: 新增 cropTopHalf 方法**

在 `compressImage` 方法之后添加裁剪方法：

```java
/**
 * 裁剪图片顶部 50%
 * 聚焦 hero 区域，提升卡片封面的信息密度
 */
private static byte[] cropTopHalf(byte[] imageBytes) {
    try {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        int cropHeight = original.getHeight() / 2;
        BufferedImage cropped = original.getSubimage(0, 0, original.getWidth(), cropHeight);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(cropped, "png", baos);
        return baos.toByteArray();
    } catch (Exception e) {
        log.error("裁剪图片失败", e);
        return imageBytes;
    }
}
```

**Step 3: 在 saveWebPageScreenshot 中调用裁剪**

修改 `saveWebPageScreenshot` 方法，在截图后、保存前插入裁剪步骤。将第 66-68 行：

```java
// 截图
byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
// 保存原始图片
saveImage(screenshotBytes, imageSavePath);
```

改为：

```java
// 截图
byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
// 裁剪顶部 50%（聚焦 hero 区域）
byte[] croppedBytes = cropTopHalf(screenshotBytes);
// 保存裁剪后的图片
saveImage(croppedBytes, imageSavePath);
```

**Step 4: 提交**

```bash
git add backend/screenshot/screenshot-app/src/main/java/com/dango/dangoaicodescreenshot/utils/WebScreenshotUtils.java
git commit -m "feat: 截图后裁剪顶部 50% hero 区域，提升卡片封面信息密度"
```

---

### Task 4: CodeGenApplicationServiceImpl — 生成完成后触发截图

**Files:**
- Modify: `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/application/service/impl/CodeGenApplicationServiceImpl.java:42-55`（注入依赖）
- Modify: `backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/application/service/impl/CodeGenApplicationServiceImpl.java:197-201`（doOnComplete 回调）

**Step 1: 注入 AppApplicationService 和 deployHost 配置**

在类的依赖注入区域（第 53 行 `genTaskService` 之后）添加：

```java
@Resource
private AppApplicationService appApplicationService;
@Resource
private AppRepository appRepository;
@Value("${app.deploy-host:http://localhost}")
private String deployHost;
```

注意：`appRepository` 已存在（第 44-45 行），不需要重复添加。只需添加 `appApplicationService` 和 `deployHost`。

同时添加 import：

```java
import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import org.springframework.beans.factory.annotation.Value;
```

**Step 2: 在 startBackgroundGeneration 的 doOnComplete 中触发截图**

修改第 197-201 行的 `.doOnComplete()` 回调，在 `log.info` 之后添加截图触发逻辑：

```java
.doOnComplete(() -> {
    genTaskService.markCompleted(appId, userId);
    chatHistoryService.updateAiMessage(chatHistoryId, fullContentBuilder.toString(), "completed");
    MonitorContextHolder.clearContext();
    log.info("后台生成任务完成: appId={}, userId={}", appId, userId);
    // 生成完成后异步截图（用预览 URL，不依赖部署）
    try {
        App completedApp = appRepository.findById(appId).orElse(null);
        if (completedApp != null && completedApp.getCodeGenType() != null) {
            String previewUrl = String.format("%s/api/static/%s_%s/dist/index.html",
                    deployHost, completedApp.getCodeGenType(), appId);
            appApplicationService.generateAppScreenshotAsync(appId, previewUrl);
            log.info("已触发生成完成截图: appId={}", appId);
        }
    } catch (Exception e) {
        log.warn("触发截图失败（不影响主流程）: appId={}, error={}", appId, e.getMessage());
    }
})
```

**Step 3: 提交**

```bash
git add backend/app/app-service/src/main/java/com/dango/dangoaicodeapp/application/service/impl/CodeGenApplicationServiceImpl.java
git commit -m "feat: 代码生成完成后自动触发截图，不再依赖部署"
```

---

### Task 5: 验证与清理

**Step 1: 编译验证**

```bash
cd backend && mvn clean compile -pl screenshot/screenshot-app -am -q
cd backend && mvn clean compile -pl app/app-service -am -q
```

预期：两个模块都编译通过，无错误。

**Step 2: 提交最终状态**

如果编译有问题，修复后提交。如果一切正常，无需额外提交。

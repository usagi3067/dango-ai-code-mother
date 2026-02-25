package com.dango.dangoaicodescreenshot.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1280;
        final int DEFAULT_HEIGHT = 720;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     * 生成网页截图
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }
        try {
            // 创建临时目录
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始截图文件路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 访问网页
            webDriver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(webDriver);
            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 直接保存完整截图（1280x720 与卡片容器比例接近，无需裁剪）
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);
            // 压缩图片
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);
            // 删除原始图片，只保留压缩图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            return null;
        }
    }


    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            ChromeOptions options = new ChromeOptions();
            // 通过环境变量 CHROME_BIN 指定浏览器路径（Docker 容器中为 /usr/bin/chromium-browser）
            String chromeBin = System.getenv("CHROME_BIN");
            if (StrUtil.isNotBlank(chromeBin) && new File(chromeBin).exists()) {
                options.setBinary(chromeBin);
            } else {
                // macOS 本地开发回退
                String macChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
                if (new File(macChromePath).exists()) {
                    options.setBinary(macChromePath);
                }
            }
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            options.addArguments("--disable-extensions");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 通过环境变量 CHROMEDRIVER_PATH 显式指定 ChromeDriver 路径，避免 Selenium Manager 自动下载
            String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
            WebDriver driver;
            if (StrUtil.isNotBlank(chromeDriverPath) && new File(chromeDriverPath).exists()) {
                log.info("使用指定的 ChromeDriver: {}", chromeDriverPath);
                ChromeDriverService service = new ChromeDriverService.Builder()
                        .usingDriverExecutable(new File(chromeDriverPath))
                        .build();
                driver = new ChromeDriver(service, options);
            } else {
                log.info("未找到 CHROMEDRIVER_PATH 环境变量，使用 Selenium 自动管理");
                driver = new ChromeDriver(options);
            }
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败，请确保已安装 Chrome/Chromium 浏览器", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败：" + e.getMessage());
        }
    }


    /**
     * 保存图片到文件
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     */
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


    /**
     * 等待页面加载完成
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

}

package com.dango.dangoaicodemother.service;

/**
 * @author dango
 * @description
 * @date
 */
public interface ScreenshotService {
    /**
     * 生成并上传截图
     * @param webUrl 网站URL
     * @return 截图图片的URL
     */
    String generateAndUploadScreenshot(String webUrl);
}

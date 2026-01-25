package com.dango.dangoaicodemother.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author dango
 * @description
 * @date
 */
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String screenshotPath = WebScreenshotUtils.saveWebPageScreenshot("https://www.baidu.com");
        assertNotNull(screenshotPath);
    }
}
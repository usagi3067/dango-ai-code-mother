package com.dango.dangoaicodescreenshot;

/**
 * 截图内部服务接口
 * 定义供其他微服务内部调用的方法，不对外暴露
 *
 * @author dango
 */
public interface InnerScreenshotService {

    /**
     * 生成网页截图并上传到对象存储
     *
     * @param webUrl 网页 URL
     * @return 截图图片的 URL
     */
    String generateAndUploadScreenshot(String webUrl);
}

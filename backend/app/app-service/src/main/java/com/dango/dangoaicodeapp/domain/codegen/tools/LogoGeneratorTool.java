package com.dango.dangoaicodeapp.domain.codegen.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.dango.aicodegenerate.model.ImageCategoryEnum;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodecommon.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logo 生成工具
 * 使用阿里云 DashScope 生成 Logo 图片
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    @Resource
    private CosManager cosManager;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1)
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String imageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(imageUrl)) {
                        String cosUrl = downloadAndUploadToCos(imageUrl);
                        if (StrUtil.isNotBlank(cosUrl)) {
                            logoList.add(ImageResource.builder()
                                    .category(ImageCategoryEnum.LOGO)
                                    .description(description)
                                    .url(cosUrl)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }

    private String downloadAndUploadToCos(String imageUrl) {
        File tempFile = null;
        try {
            String fileName = RandomUtil.randomString(8) + ".png";
            tempFile = FileUtil.createTempFile("logo_", ".png", true);
            HttpUtil.downloadFile(imageUrl, tempFile);
            if (!tempFile.exists() || tempFile.length() == 0) {
                log.error("下载图片失败，文件为空: {}", imageUrl);
                return null;
            }
            String cosKey = String.format("/logo/%s/%s", RandomUtil.randomString(8), fileName);
            String cosUrl = cosManager.uploadFile(cosKey, tempFile);
            log.info("Logo 上传 COS 成功: {} -> {}", imageUrl, cosUrl);
            return cosUrl;
        } catch (Exception e) {
            log.error("下载并上传图片到 COS 失败: {}", e.getMessage(), e);
            return null;
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }
}

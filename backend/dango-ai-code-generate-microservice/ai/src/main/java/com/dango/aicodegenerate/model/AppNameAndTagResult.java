package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * AI 生成应用名称和标签的结构化输出结果
 */
@Description("应用名称和标签生成结果")
@Data
public class AppNameAndTagResult {

    @Description("应用名称，不超过20个字符")
    private String appName;

    @Description("应用标签，必须是以下之一：tool、website、data_analysis、activity_page、management_platform、user_app、personal_management、game")
    private String tag;
}

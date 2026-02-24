package com.dango.dangoaicodeapp.infrastructure.config;

import dev.langchain4j.spi.prompt.PromptTemplateFactory;

/**
 * 禁用模板变量替换的 PromptTemplateFactory 实现
 *
 * 默认的 DefaultPromptTemplateFactory 会将 {{xxx}} 识别为模板变量并尝试替换，
 * 当用户输入包含 Vue/React 模板语法（如 {{msg}}）时会抛出 IllegalArgumentException。
 *
 * 此实现直接返回原始文本，不做任何变量替换，彻底规避该问题。
 */
public class NoopPromptTemplateFactory implements PromptTemplateFactory {

    @Override
    public Template create(Input input) {
        return variables -> input.getTemplate();
    }
}

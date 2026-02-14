package com.dango.aicodegenerate.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;

import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt 安全输入护轨
 * 在用户输入传递给 AI 模型之前进行安全审查
 * 
 * 功能：
 * 1. 拒绝过长的 Prompt
 * 2. 拒绝包含敏感词的 Prompt
 * 3. 拒绝包含注入攻击的 Prompt
 */
@Slf4j
public class PromptSafetyInputGuardrail implements InputGuardrail {

    /**
     * 最大输入长度限制
     */
    private static final int MAX_INPUT_LENGTH = 50000;

    /**
     * 敏感词列表
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // 中文敏感词
            "忽略之前的指令", "忽略上面的指令", "忽略所有指令",
            "破解", "越狱", "绕过限制", "绕过安全",
            // 英文敏感词
            "ignore previous instructions", "ignore above", "ignore all instructions",
            "hack", "bypass", "jailbreak", "bypass security",
            "disregard previous", "forget everything above"
    );

    /**
     * 注入攻击模式
     */
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
//            // 忽略指令类攻击
//            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
//            // 遗忘指令类攻击
//            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
//            // 角色扮演类攻击
//            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
//            // 系统提示注入
//            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
//            // 新指令注入
//            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:"),
//            // DAN 类越狱攻击
//            Pattern.compile("(?i)(?:DAN|do\\s+anything\\s+now)"),
//            // 开发者模式攻击
//            Pattern.compile("(?i)(?:developer|dev)\\s+mode\\s+(?:enabled|on|activated)")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        
        // 1. 检查是否为空
        if (input == null || input.trim().isEmpty()) {
            log.warn("护轨拦截：输入内容为空");
            return fatal("输入内容不能为空");
        }

        // 2. 检查输入长度
        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("护轨拦截：输入内容过长，长度: {}", input.length());
            return fatal("输入内容过长，请不要超过 " + MAX_INPUT_LENGTH + " 字");
        }

        // 3. 检查敏感词
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                log.warn("护轨拦截：检测到敏感词 [{}]", sensitiveWord);
                return fatal("输入包含不当内容，请修改后重试");
            }
        }

        // 4. 检查注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("护轨拦截：检测到注入攻击模式 [{}]", pattern.pattern());
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }

        log.debug("护轨检查通过，输入长度: {}", input.length());
        return success();
    }
}

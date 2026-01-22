package com.dango.dangoaicodemother.core;

import com.dango.dangoaicodemother.ai.model.HtmlCodeResult;
import com.dango.dangoaicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析器
 * 该类主要用于从 AI 生成的 Markdown 文本中提取特定类型的代码块（如 HTML, CSS, JS）。
 * 由于 AI 输出通常包含描述性文字和 Markdown 代码格式，我们需要正则提取核心代码以便后续处理和保存。
 *
 * @author dango
 */
@Deprecated
public class CodeParser {

    /**
     * 匹配 HTML 代码块的正则表达式
     * 规则：匹配以 ```html 开头，以 ``` 结尾的内容，忽略大小写，允许换行符。
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配 CSS 代码块的正则表达式
     * 规则：匹配以 ```css 开头，以 ``` 结尾的内容。
     */
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配 JavaScript 代码块的正则表达式
     * 规则：匹配以 ```js 或 ```javascript 开头，以 ``` 结尾的内容。
     */
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 HTML 单文件代码
     * 从 AI 返回的字符串中提取 HTML 代码块，如果未找到代码块，则将全文视为代码。
     *
     * @param codeContent AI 返回的原始文本字符串
     * @return 包含解析后 HTML 代码的结果对象
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 尝试根据正则表达式提取 HTML 代码块
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果正则提取失败，为了容错，将整个输入内容作为 HTML 原始内容返回
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 解析多文件代码（同时提取 HTML、CSS 和 JS）
     * 适用于生成分离式的前端代码结构。
     *
     * @param codeContent AI 返回的原始文本字符串
     * @return 包含解析后 HTML, CSS, JS 代码的结构化结果对象
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        
        // 分别提取各类代码块
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        
        // 如果提取到了 HTML 代码，设置到结果对象中
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        
        // 如果提取到了 CSS 代码，设置到结果对象中
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        
        // 如果提取到了 JS 代码，设置到结果对象中
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        
        return result;
    }

    /**
     * 提取 HTML 代码内容的便捷私有方法
     *
     * @param content 原始内容
     * @return 匹配到的 HTML 代码块内容，未找到则返回 null
     */
    private static String extractHtmlCode(String content) {
        return extractCodeByPattern(content, HTML_CODE_PATTERN);
    }

    /**
     * 通用的根据正则表达式模式提取代码块的方法
     *
     * @param content 原始内容文本
     * @param pattern 预编译的正则表达式对象
     * @return 提取出的第一个匹配组（即代码块内部内容），未匹配到则返回 null
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        // 如果找到匹配项，返回第一个捕获组的内容
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

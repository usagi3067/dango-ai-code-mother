/**
 * Markdown 渲染工具
 * 
 * 使用 marked 解析 Markdown，配合 highlight.js 实现代码高亮
 */

import { marked } from 'marked'
import hljs from 'highlight.js/lib/core'

// 按需导入常用语言，减少打包体积
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml' // HTML
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'

// 注册语言
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('css', css)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('sql', sql)

/**
 * 配置 marked 渲染器
 */
const renderer = new marked.Renderer()

// 自定义代码块渲染
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
  let highlighted: string
  
  try {
    if (language === 'plaintext') {
      highlighted = hljs.highlightAuto(text).value
    } else {
      highlighted = hljs.highlight(text, { language }).value
    }
  } catch {
    highlighted = text
  }
  
  return `<pre class="hljs-code-block"><code class="hljs language-${language}">${highlighted}</code></pre>`
}

// 自定义行内代码渲染
renderer.codespan = ({ text }: { text: string }) => {
  return `<code class="hljs-inline">${text}</code>`
}

// 配置 marked
marked.setOptions({
  renderer,
  gfm: true, // 启用 GitHub 风格 Markdown
  breaks: true, // 将换行符转换为 <br>
})

/**
 * 渲染 Markdown 内容
 * 
 * @param content - Markdown 文本
 * @returns 渲染后的 HTML
 */
export const renderMarkdown = (content: string): string => {
  if (!content) return ''
  
  try {
    return marked.parse(content) as string
  } catch {
    // 解析失败时返回原始内容，简单转义
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
  }
}

export default renderMarkdown

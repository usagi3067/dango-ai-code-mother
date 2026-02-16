<template>
  <div class="auth-brand-panel">
    <div class="brand-header">
      <img src="@/assets/logo.svg" alt="Logo" class="brand-logo" />
      <span class="brand-name">Dango AI Code</span>
    </div>
    <div class="brand-content">
      <h1 class="brand-title">
        用 <span class="gradient-text">AI</span> 生成你的 Web 应用
      </h1>
      <p class="brand-subtitle">输入需求描述，自动生成完整可运行的代码项目</p>

      <!-- 底部预览区域：默认插槽，可被使用方覆盖 -->
      <slot>
        <!-- 默认内容：模拟 AI 对话卡片 -->
        <div class="preview-card">
          <div class="chat-message user-message">
            <div class="message-avatar user-avatar">U</div>
            <div class="message-bubble user-bubble">帮我生成一个待办事项应用</div>
          </div>
          <div class="chat-message">
            <div class="message-avatar ai-avatar">AI</div>
            <div class="message-bubble ai-bubble">
              <div class="code-line"><span class="code-tag">&lt;template&gt;</span></div>
              <div class="code-line">&nbsp;&nbsp;<span class="code-tag">&lt;div</span> <span class="code-attr">class</span>=<span class="code-str">"todo-app"</span><span class="code-tag">&gt;</span></div>
              <div class="code-line">&nbsp;&nbsp;&nbsp;&nbsp;<span class="code-tag">&lt;h1&gt;</span>待办事项<span class="code-tag">&lt;/h1&gt;</span></div>
              <div class="code-line">&nbsp;&nbsp;&nbsp;&nbsp;<span class="code-tag">&lt;input</span> <span class="code-attr">v-model</span>=<span class="code-str">"newTodo"</span> /&gt;</div>
              <div class="code-line"><span class="code-comment">&nbsp;&nbsp;&nbsp;&nbsp;...</span></div>
            </div>
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.auth-brand-panel {
  flex: 1;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  padding: 40px 60px;
  overflow: hidden;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 32px;
  height: 32px;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 480px;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.3;
  margin-bottom: 12px;
}

.gradient-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 40px;
}

.preview-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-avatar {
  background: #e8e8e8;
  color: #666;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.message-bubble {
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  max-width: 320px;
}

.user-bubble {
  background: #f0f0f0;
  color: #333;
}

.ai-bubble {
  background: #f8f7ff;
  color: #333;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.code-line { white-space: nowrap; }
.code-tag { color: #e06c75; }
.code-attr { color: #d19a66; }
.code-str { color: #98c379; }
.code-comment { color: #999; }

@media (max-width: 768px) {
  .auth-brand-panel {
    display: none;
  }
}
</style>

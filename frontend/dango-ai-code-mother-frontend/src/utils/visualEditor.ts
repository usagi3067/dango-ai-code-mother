/**
 * 可视化编辑器工具类
 * 
 * 负责管理 iframe 内的可视化编辑功能
 * 实现元素选择、高亮、通信等功能
 * 
 * 【核心功能】
 * 1. 开启/关闭编辑模式
 * 2. 向 iframe 注入编辑脚本
 * 3. 处理 iframe 与主页面的消息通信
 * 4. 管理选中元素状态
 */

/**
 * 元素信息接口
 * 描述用户选中的 DOM 元素的详细信息
 */
export interface ElementInfo {
  tagName: string      // 标签名，如 'DIV', 'BUTTON'
  id: string           // 元素 ID
  className: string    // 元素类名
  textContent: string  // 元素文本内容（截取前100字符）
  selector: string     // CSS 选择器路径
  pagePath: string     // 页面路径（查询参数和锚点）
  rect: {              // 元素位置和尺寸
    top: number
    left: number
    width: number
    height: number
  }
}

/**
 * 可视化编辑器配置选项
 */
export interface VisualEditorOptions {
  onElementSelected?: (elementInfo: ElementInfo) => void  // 元素被选中时的回调
  onElementHover?: (elementInfo: ElementInfo) => void     // 元素被悬浮时的回调
}

/**
 * 可视化编辑器类
 * 
 * 使用方法：
 * 1. 创建实例：const editor = new VisualEditor({ onElementSelected: ... })
 * 2. 初始化：editor.init(iframeElement)
 * 3. 开启编辑：editor.enableEditMode()
 * 4. 监听消息：window.addEventListener('message', editor.handleIframeMessage)
 */
export class VisualEditor {
  private iframe: HTMLIFrameElement | null = null
  private isEditMode = false
  private options: VisualEditorOptions

  constructor(options: VisualEditorOptions = {}) {
    this.options = options
  }

  /**
   * 初始化编辑器
   * @param iframe - 要编辑的 iframe 元素
   */
  init(iframe: HTMLIFrameElement) {
    this.iframe = iframe
  }

  /**
   * 获取当前编辑模式状态
   */
  getEditMode(): boolean {
    return this.isEditMode
  }

  /**
   * 开启编辑模式
   */
  enableEditMode() {
    if (!this.iframe) {
      return
    }
    this.isEditMode = true
    // 延迟注入脚本，确保 iframe 内容已加载
    setTimeout(() => {
      this.injectEditScript()
    }, 300)
  }

  /**
   * 关闭编辑模式
   */
  disableEditMode() {
    this.isEditMode = false
    this.sendMessageToIframe({
      type: 'TOGGLE_EDIT_MODE',
      editMode: false,
    })
    // 清除所有编辑状态
    this.sendMessageToIframe({
      type: 'CLEAR_ALL_EFFECTS',
    })
  }

  /**
   * 切换编辑模式
   * @returns 切换后的编辑模式状态
   */
  toggleEditMode(): boolean {
    if (this.isEditMode) {
      this.disableEditMode()
    } else {
      this.enableEditMode()
    }
    return this.isEditMode
  }

  /**
   * 强制同步状态并清理
   */
  syncState() {
    if (!this.isEditMode) {
      this.sendMessageToIframe({
        type: 'CLEAR_ALL_EFFECTS',
      })
    }
  }

  /**
   * 清除选中的元素
   */
  clearSelection() {
    this.sendMessageToIframe({
      type: 'CLEAR_SELECTION',
    })
  }

  /**
   * iframe 加载完成时调用
   * 如果处于编辑模式，重新注入脚本
   */
  onIframeLoad() {
    if (this.isEditMode) {
      setTimeout(() => {
        this.injectEditScript()
      }, 500)
    } else {
      // 确保非编辑模式时清理状态
      setTimeout(() => {
        this.syncState()
      }, 500)
    }
  }

  /**
   * 处理来自 iframe 的消息
   * @param event - MessageEvent 对象
   */
  handleIframeMessage(event: MessageEvent) {
    const { type, data } = event.data || {}
    
    switch (type) {
      case 'ELEMENT_SELECTED':
        if (this.options.onElementSelected && data?.elementInfo) {
          this.options.onElementSelected(data.elementInfo)
        }
        break
      case 'ELEMENT_HOVER':
        if (this.options.onElementHover && data?.elementInfo) {
          this.options.onElementHover(data.elementInfo)
        }
        break
    }
  }

  /**
   * 向 iframe 发送消息
   * @param message - 要发送的消息对象
   */
  private sendMessageToIframe(message: Record<string, unknown>) {
    if (this.iframe?.contentWindow) {
      this.iframe.contentWindow.postMessage(message, '*')
    }
  }

  /**
   * 注入编辑脚本到 iframe
   * 
   * 【实现原理】
   * 1. 等待 iframe 加载完成
   * 2. 检查是否已注入过脚本（避免重复注入）
   * 3. 创建 script 元素并注入到 iframe 的 head 中
   */
  private injectEditScript() {
    if (!this.iframe) return

    const waitForIframeLoad = () => {
      try {
        if (this.iframe!.contentWindow && this.iframe!.contentDocument) {
          // 检查是否已经注入过脚本
          if (this.iframe!.contentDocument.getElementById('visual-edit-script')) {
            // 已注入，直接发送开启编辑模式的消息
            this.sendMessageToIframe({
              type: 'TOGGLE_EDIT_MODE',
              editMode: true,
            })
            return
          }

          const script = this.generateEditScript()
          const scriptElement = this.iframe!.contentDocument.createElement('script')
          scriptElement.id = 'visual-edit-script'
          scriptElement.textContent = script
          this.iframe!.contentDocument.head.appendChild(scriptElement)
        } else {
          // iframe 未加载完成，延迟重试
          setTimeout(waitForIframeLoad, 100)
        }
      } catch {
        // 静默处理注入失败（可能是跨域问题）
      }
    }

    waitForIframeLoad()
  }

  /**
   * 生成编辑脚本内容
   * 
   * 这段脚本会被注入到 iframe 中执行，实现：
   * 1. 鼠标悬浮时显示虚线边框
   * 2. 点击元素时显示实线边框并选中
   * 3. 将选中的元素信息发送给父窗口
   */
  private generateEditScript(): string {
    return `(function() {
  let isEditMode = true;
  let currentHoverElement = null;
  let currentSelectedElement = null;

  /**
   * 注入编辑模式样式
   */
  function injectStyles() {
    if (document.getElementById('edit-mode-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'edit-mode-styles';
    style.textContent = \`
      /* 悬浮效果：蓝色虚线边框 */
      .edit-hover {
        outline: 2px dashed #1890ff !important;
        outline-offset: 2px !important;
        cursor: crosshair !important;
        transition: outline 0.2s ease !important;
        position: relative !important;
      }
      
      .edit-hover::before {
        content: '' !important;
        position: absolute !important;
        top: -4px !important;
        left: -4px !important;
        right: -4px !important;
        bottom: -4px !important;
        background: rgba(24, 144, 255, 0.02) !important;
        pointer-events: none !important;
        z-index: -1 !important;
      }
      
      /* 选中效果：绿色实线边框 */
      .edit-selected {
        outline: 3px solid #52c41a !important;
        outline-offset: 2px !important;
        cursor: default !important;
        position: relative !important;
      }
      
      .edit-selected::before {
        content: '' !important;
        position: absolute !important;
        top: -4px !important;
        left: -4px !important;
        right: -4px !important;
        bottom: -4px !important;
        background: rgba(82, 196, 26, 0.03) !important;
        pointer-events: none !important;
        z-index: -1 !important;
      }
    \`;
    document.head.appendChild(style);
  }

  /**
   * 生成元素的 CSS 选择器
   * @param element - DOM 元素
   * @returns CSS 选择器字符串
   */
  function generateSelector(element) {
    const path = [];
    let current = element;
    
    while (current && current !== document.body) {
      let selector = current.tagName.toLowerCase();
      
      // 如果有 ID，直接使用 ID 选择器
      if (current.id) {
        selector += '#' + current.id;
        path.unshift(selector);
        break;
      }
      
      // 添加类名（排除编辑模式添加的类）
      if (current.className) {
        const classes = current.className.split(' ').filter(c => c && !c.startsWith('edit-'));
        if (classes.length > 0) {
          selector += '.' + classes.join('.');
        }
      }
      
      // 添加 nth-child 索引
      const siblings = Array.from(current.parentElement?.children || []);
      const index = siblings.indexOf(current) + 1;
      selector += ':nth-child(' + index + ')';
      
      path.unshift(selector);
      current = current.parentElement;
    }
    
    return path.join(' > ');
  }

  /**
   * 获取元素详细信息
   * @param element - DOM 元素
   * @returns 元素信息对象
   */
  function getElementInfo(element) {
    const rect = element.getBoundingClientRect();
    
    // 获取页面路径（查询参数和锚点）
    let pagePath = window.location.search + window.location.hash;
    if (!pagePath) {
      pagePath = '';
    }
    
    return {
      tagName: element.tagName,
      id: element.id,
      className: element.className,
      textContent: element.textContent?.trim().substring(0, 100) || '',
      selector: generateSelector(element),
      pagePath: pagePath,
      rect: {
        top: rect.top,
        left: rect.left,
        width: rect.width,
        height: rect.height
      }
    };
  }

  /**
   * 清除悬浮效果
   */
  function clearHoverEffect() {
    if (currentHoverElement) {
      currentHoverElement.classList.remove('edit-hover');
      currentHoverElement = null;
    }
  }

  /**
   * 清除选中效果
   */
  function clearSelectedEffect() {
    const selected = document.querySelectorAll('.edit-selected');
    selected.forEach(el => el.classList.remove('edit-selected'));
    currentSelectedElement = null;
  }

  let eventListenersAdded = false;

  /**
   * 添加事件监听器
   */
  function addEventListeners() {
    if (eventListenersAdded) return;

    // 鼠标悬浮事件
    const mouseoverHandler = (event) => {
      if (!isEditMode) return;
      
      const target = event.target;
      if (target === currentHoverElement || target === currentSelectedElement) return;
      if (target === document.body || target === document.documentElement) return;
      if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;
      
      clearHoverEffect();
      target.classList.add('edit-hover');
      currentHoverElement = target;
    };

    // 鼠标移出事件
    const mouseoutHandler = (event) => {
      if (!isEditMode) return;
      
      const target = event.target;
      if (!event.relatedTarget || !target.contains(event.relatedTarget)) {
        clearHoverEffect();
      }
    };

    // 点击事件
    const clickHandler = (event) => {
      if (!isEditMode) return;
      
      event.preventDefault();
      event.stopPropagation();
      
      const target = event.target;
      if (target === document.body || target === document.documentElement) return;
      if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;
      
      clearSelectedEffect();
      clearHoverEffect();
      
      target.classList.add('edit-selected');
      currentSelectedElement = target;
      
      const elementInfo = getElementInfo(target);
      
      try {
        // 向父窗口发送选中元素信息
        window.parent.postMessage({
          type: 'ELEMENT_SELECTED',
          data: { elementInfo }
        }, '*');
      } catch {
        // 静默处理发送失败
      }
    };

    document.body.addEventListener('mouseover', mouseoverHandler, true);
    document.body.addEventListener('mouseout', mouseoutHandler, true);
    document.body.addEventListener('click', clickHandler, true);
    
    eventListenersAdded = true;
  }

  /**
   * 设置事件监听器
   */
  function setupEventListeners() {
    addEventListeners();
  }

  /**
   * 显示编辑模式提示
   */
  function showEditTip() {
    if (document.getElementById('edit-tip')) return;
    
    const tip = document.createElement('div');
    tip.id = 'edit-tip';
    tip.innerHTML = '🎯 编辑模式已开启<br/>悬浮查看元素，点击选中元素';
    tip.style.cssText = \`
      position: fixed;
      top: 20px;
      right: 20px;
      background: #1890ff;
      color: white;
      padding: 12px 16px;
      border-radius: 6px;
      font-size: 14px;
      z-index: 9999;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      animation: fadeIn 0.3s ease;
    \`;
    
    const style = document.createElement('style');
    style.textContent = '@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }';
    document.head.appendChild(style);
    
    document.body.appendChild(tip);
    
    // 3秒后自动隐藏
    setTimeout(() => {
      if (tip.parentNode) {
        tip.style.animation = 'fadeIn 0.3s ease reverse';
        setTimeout(() => tip.remove(), 300);
      }
    }, 3000);
  }

  // 监听父窗口消息
  window.addEventListener('message', (event) => {
    const { type, editMode } = event.data || {};
    
    switch (type) {
      case 'TOGGLE_EDIT_MODE':
        isEditMode = editMode;
        if (isEditMode) {
          injectStyles();
          setupEventListeners();
          showEditTip();
        } else {
          clearHoverEffect();
          clearSelectedEffect();
        }
        break;
      case 'CLEAR_SELECTION':
        clearSelectedEffect();
        break;
      case 'CLEAR_ALL_EFFECTS':
        isEditMode = false;
        clearHoverEffect();
        clearSelectedEffect();
        const tip = document.getElementById('edit-tip');
        if (tip) tip.remove();
        break;
    }
  });

  // 初始化
  injectStyles();
  setupEventListeners();
  showEditTip();
})();`
  }
}

export default VisualEditor

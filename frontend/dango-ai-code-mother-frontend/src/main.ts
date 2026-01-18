/**
 * main.ts - Vue 应用的入口文件
 * 这个文件负责创建 Vue 应用实例，注册插件，并将应用挂载到 DOM 上
 * 执行顺序：index.html 加载 -> 执行 main.ts -> 创建 Vue 应用 -> 挂载到 #app 元素
 */

/**
 * 从 Vue 核心库导入 createApp 函数
 * createApp: 用于创建 Vue 3 应用实例的工厂函数
 * Vue 3 使用 createApp 替代了 Vue 2 的 new Vue() 方式
 */
import { createApp } from 'vue'

/**
 * 从 Pinia 库导入 createPinia 函数
 * Pinia: Vue 3 官方推荐的状态管理库（替代 Vuex）
 * createPinia: 创建 Pinia 实例，用于管理全局状态
 * 状态管理：在多个组件之间共享数据，如用户信息、购物车等
 */
import { createPinia } from 'pinia'

/**
 * 导入根组件
 * App.vue: 应用的根组件，所有其他组件都是它的子组件
 * .vue 文件是 Vue 的单文件组件（SFC），包含模板、脚本和样式
 */
import App from './App.vue'

/**
 * 导入路由实例
 * router: 在 ./router/index.ts 中配置的路由实例
 * Vue Router: 用于实现单页应用（SPA）的页面导航
 * 可以在不刷新页面的情况下切换不同的视图
 */
import router from './router'

/**
 * 导入 Ant Design Vue 组件库
 * Antd: Ant Design Vue 的完整组件库
 * 包含按钮、表单、布局等丰富的 UI 组件
 */
import Antd from 'ant-design-vue'

/**
 * 导入 Ant Design Vue 的重置样式
 * reset.css: 重置浏览器默认样式，确保组件在不同浏览器中显示一致
 * 这个文件会覆盖浏览器的默认样式，提供统一的基础样式
 */
import 'ant-design-vue/dist/reset.css'

/**
 * 创建 Vue 应用实例
 * createApp(App): 以 App 组件作为根组件创建应用
 * 返回的 app 对象提供了配置应用的方法（use、mount 等）
 */
const app = createApp(App)

/**
 * 注册 Pinia 插件
 * app.use(): Vue 3 的插件注册方法
 * createPinia(): 创建 Pinia 实例
 * 注册后，所有组件都可以使用 Pinia 进行状态管理
 */
app.use(createPinia())

/**
 * 注册 Vue Router 插件
 * 注册后，所有组件都可以使用：
 * - <RouterLink>: 路由链接组件
 * - <RouterView>: 路由视图组件
 * - useRouter(): 路由实例钩子
 * - useRoute(): 当前路由信息钩子
 */
app.use(router)

/**
 * 注册 Ant Design Vue 插件
 * 注册后，所有组件都可以直接使用 Ant Design 的组件
 * 如：<a-button>、<a-layout>、<a-menu> 等
 * 不需要在每个组件中单独导入
 */
app.use(Antd)

/**
 * 将 Vue 应用挂载到 DOM 元素上
 * '#app': CSS 选择器，对应 index.html 中的 <div id="app"></div>
 * 挂载后，Vue 会接管这个 DOM 元素，将虚拟 DOM 渲染成真实 DOM
 * 此时应用开始运行，用户可以看到页面内容
 */
app.mount('#app')

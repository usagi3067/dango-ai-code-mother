/**
 * openapi2ts.config.ts - OpenAPI 自动生成请求代码配置文件
 * 
 * 功能：
 * 根据后端提供的 Swagger/OpenAPI 文档自动生成前端请求代码
 * 
 * 优点：
 * 1. 自动生成：无需手动编写请求代码
 * 2. 类型安全：自动生成 TypeScript 类型定义
 * 3. 同步更新：后端接口变更后重新生成即可
 * 4. 减少错误：避免手动编写时的拼写错误
 * 5. 提高效率：节省大量重复劳动
 * 
 * 使用方式：
 * 1. 确保后端服务已启动（http://localhost:8123）
 * 2. 运行命令：npm run openapi2ts
 * 3. 生成的代码会保存在 src/api 目录下
 * 
 * 参考文档：
 * https://github.com/umijs/openapi
 */

export default {
  /**
   * requestLibPath: 请求库的导入路径
   * 
   * 作用：指定生成的代码使用哪个请求库
   * 
   * 这里使用我们自定义的 request.ts 文件
   * 生成的代码会自动导入：import request from '@/request'
   * 
   * 示例生成的代码：
   * import request from '@/request'
   * 
   * export async function getUserList(params) {
   *   return request('/user/list', { method: 'GET', params })
   * }
   */
  requestLibPath: "import request from '@/request'",

  /**
   * schemaPath: OpenAPI 文档的地址
   * 
   * 作用：指定从哪里获取 API 文档
   * 
   * 格式：
   * - 本地文件：'./swagger.json'
   * - 远程地址：'http://localhost:8123/api/v3/api-docs'
   * 
   * 注意事项：
   * 1. 确保后端服务已启动
   * 2. 确保 Swagger 文档可以访问
   * 3. 如果是跨域请求，需要后端配置 CORS
   * 
   * 常见的 Swagger 文档地址：
   * - Swagger 2.0: /v2/api-docs
   * - OpenAPI 3.0: /v3/api-docs
   * - Swagger UI: /swagger-ui.html
   * 
   * 验证方法：
   * 在浏览器中访问 http://localhost:8123/api/v3/api-docs
   * 应该能看到 JSON 格式的 API 文档
   */
  schemaPath: 'http://localhost:8123/api/v3/api-docs',

  /**
   * serversPath: 生成代码的保存路径
   * 
   * 作用：指定生成的代码保存在哪个目录
   * 
   * 这里设置为 './src'，生成的代码会保存在：
   * - src/api/xxxController.ts (各个控制器的请求方法)
   * - src/api/typings.d.ts (TypeScript 类型定义)
   * 
   * 目录结构示例：
   * src/
   * ├── api/
   * │   ├── healthController.ts    (健康检查接口)
   * │   ├── userController.ts      (用户相关接口)
   * │   ├── postController.ts      (文章相关接口)
   * │   └── typings.d.ts           (类型定义)
   * └── request.ts                 (请求配置)
   */
  serversPath: './src',
}

/**
 * 配置说明：
 * 
 * 1. requestLibPath 配置
 *    - 必须与实际的 request.ts 文件路径一致
 *    - 使用 @ 别名指向 src 目录
 *    - 生成的代码会自动导入这个请求库
 * 
 * 2. schemaPath 配置
 *    - 必须是可访问的 URL 或本地文件路径
 *    - 生成前确保后端服务已启动
 *    - 支持 Swagger 2.0 和 OpenAPI 3.0 格式
 * 
 * 3. serversPath 配置
 *    - 相对于项目根目录的路径
 *    - 生成的文件会自动创建 api 子目录
 *    - 建议使用 './src' 保持项目结构清晰
 * 
 * 高级配置（可选）：
 * 
 * export default {
 *   requestLibPath: "import request from '@/request'",
 *   schemaPath: 'http://localhost:8123/api/v3/api-docs',
 *   serversPath: './src',
 *   
 *   // 自定义生成的文件名
 *   apiPrefix: '@/api',
 *   
 *   // 自定义命名空间
 *   namespace: 'API',
 *   
 *   // 是否生成 mock 数据
 *   mock: false,
 *   
 *   // 自定义类型前缀
 *   typePrefix: '',
 *   
 *   // 是否生成枚举
 *   enumStyle: 'string-literal',
 *   
 *   // 钩子函数：生成前
 *   hook: {
 *     customFunctionName: (data) => {
 *       // 自定义函数名称
 *       return data.operationId
 *     }
 *   }
 * }
 */

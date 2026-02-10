/**
 * openapi2ts.config.ts - OpenAPI 自动生成请求代码配置文件
 *
 * 支持多个微服务模块：
 * - user 模块 (8123) -> src/api/user/
 * - app 模块 (8124) -> src/api/app/
 * - screenshot 模块 (8125) -> src/api/screenshot/
 *
 * 使用方式：npm run openapi2ts
 */

export default [
  {
    requestLibPath: "import request from '@/request'",
    schemaPath: 'http://localhost:8123/api/v3/api-docs',
    serversPath: './src/api',
    projectName: 'user',
  },
  {
    requestLibPath: "import request from '@/request'",
    schemaPath: 'http://localhost:8124/api/v3/api-docs',
    serversPath: './src/api',
    projectName: 'app',
  },
  {
    requestLibPath: "import request from '@/request'",
    schemaPath: 'http://localhost:8125/api/v3/api-docs',
    serversPath: './src/api',
    projectName: 'screenshot',
  },
]

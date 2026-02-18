package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import com.dango.aicodegenerate.model.AppNameAndTagResult;

import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodeapp.domain.codegen.service.AppInfoGeneratorFacade;
import com.dango.dangoaicodeapp.domain.codegen.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.domain.codegen.handler.StreamHandlerExecutor;
import com.dango.dangoaicodeapp.infrastructure.config.SupabaseClientConfig;
import com.dango.dangoaicodeapp.infrastructure.mapper.AppMapper;
import com.dango.dangoaicodeapp.infrastructure.monitor.MonitorContext;
import com.dango.dangoaicodeapp.infrastructure.monitor.MonitorContextHolder;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.model.entity.ElementInfo;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodescreenshot.InnerScreenshotService;
import com.dango.dangoaicodeuser.dto.UserDTO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.InnerUserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用服务层实现
 *
 * @author dango
 */
@Slf4j
@Service
public class AppApplicationServiceImpl extends ServiceImpl<AppMapper, App> implements AppApplicationService {

    @DubboReference
    private InnerUserService innerUserService;
    @DubboReference
    private InnerScreenshotService innerScreenshotService;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private AppInfoGeneratorFacade appInfoGeneratorFacade;


    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            UserDTO userDTO = innerUserService.getById(userId);
            UserVO userVO = innerUserService.toUserVO(userDTO);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String tag = appQueryRequest.getTag();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("tag", tag)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = innerUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(UserDTO::getId, innerUserService::toUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public Flux<String> chatToGenCode(Long appId, String message, long userId) {
        // 委托给新方法，无元素信息
        return chatToGenCode(appId, message, null, userId);
    }

    @Override
    @Deprecated
    public Flux<String> chatToGenCode(Long appId, String message, long userId, boolean agent) {
        // 委托给新方法
        return chatToGenCode(appId, message, null, userId);
    }

    @Override
    @Deprecated
    public Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId, boolean agent) {
        // 委托给新方法
        return chatToGenCode(appId, message, elementInfo, userId);
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 保存用户消息到对话历史
        try {
            chatHistoryService.saveUserMessage(appId, userId, message);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage());
        }
        // 6. 检查数据库是否启用，如果启用则查询当前 Schema
        boolean databaseEnabled = Boolean.TRUE.equals(app.getHasDatabase());
        String databaseSchema = null;
        if (databaseEnabled) {
            try {
                List<com.dango.supabase.dto.TableSchemaDTO> tables = supabaseService.getSchema(appId);
                if (tables != null && !tables.isEmpty()) {
                    databaseSchema = formatTableSchemas(tables);
                    log.info("应用 {} 已启用数据库，当前有 {} 个表", appId, tables.size());
                }
            } catch (Exception e) {
                log.error("查询数据库 Schema 失败: {}", e.getMessage(), e);
                // Schema 查询失败不影响工作流执行，只是数据库功能不可用
            }
        }
        // 7. 创建监控上下文
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(String.valueOf(userId))
                .appId(appId.toString())
                .build();
        // 设置到当前线程
        MonitorContextHolder.setContext(monitorContext);
        // 8. 使用 Agent 模式（工作流）生成代码
        log.info("使用 Agent 模式（工作流）生成代码, appId: {}, hasElementInfo: {}, databaseEnabled: {}",
                appId, elementInfo != null, databaseEnabled);
        Flux<String> codeStream = new CodeGenWorkflow().executeWorkflowWithFlux(
                message, appId, elementInfo, databaseEnabled, databaseSchema, monitorContext);
        // 9. 收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, userId)
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public String deployApp(Long appId, long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 执行 Vue 项目构建
        boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
        ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
        // 检查 dist 目录是否存在
        File distDir = new File(sourceDirPath, "dist");
        ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
        // 将 dist 目录作为部署源
        sourceDir = distDir;
        log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = innerScreenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, long userId) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(userId);
        // 使用 AI 智能生成应用名称和标签
        AppNameAndTagResult appInfo = appInfoGeneratorFacade.generateAppInfo(initPrompt);
        app.setAppName(appInfo.getAppName());
        app.setTag(appInfo.getTag());
        // 统一使用 VUE_PROJECT 类型
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), CodeGenTypeEnum.VUE_PROJECT.getValue());
        return app.getId();
    }

    @Override
    public Long createAppFromVueProject(MultipartFile[] files, String[] paths, long userId) {
        // 1. 读取 package.json 内容用于 AI 分析
        String packageJsonContent = "";
        String appVueContent = "";
        for (int i = 0; i < files.length; i++) {
            if ("package.json".equals(paths[i])) {
                try {
                    packageJsonContent = new String(files[i].getBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.warn("读取 package.json 失败", e);
                }
            }
            if (paths[i].endsWith("App.vue") && appVueContent.isEmpty()) {
                try {
                    String content = new String(files[i].getBytes(), StandardCharsets.UTF_8);
                    appVueContent = content.length() > 1000 ? content.substring(0, 1000) : content;
                } catch (IOException e) {
                    log.warn("读取 App.vue 失败", e);
                }
            }
        }

        // 2. AI 分析生成应用名称和标签
        String analysisPrompt = "分析以下 Vue 项目信息，生成一个简短的应用名称和分类标签。\n\n";
        if (!packageJsonContent.isEmpty()) {
            analysisPrompt += "package.json：\n" + packageJsonContent + "\n\n";
        }
        if (!appVueContent.isEmpty()) {
            analysisPrompt += "App.vue 片段：\n" + appVueContent;
        }
        AppNameAndTagResult appInfo = appInfoGeneratorFacade.generateAppInfo(analysisPrompt);

        // 3. 创建 App 记录
        App app = new App();
        app.setUserId(userId);
        app.setAppName(appInfo.getAppName());
        app.setTag(appInfo.getTag());
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());

        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建应用失败");

        Long appId = app.getId();

        // 4. 保存文件到项目目录
        String baseDir = System.getProperty("user.dir") + "/tmp/code_output";
        Path projectDir = Path.of(baseDir, "vue_project_" + appId);
        try {
            for (int i = 0; i < files.length; i++) {
                Path targetFile = projectDir.resolve(paths[i]);
                Files.createDirectories(targetFile.getParent());
                Files.write(targetFile, files[i].getBytes());
            }
        } catch (IOException e) {
            // 保存失败，清理已创建的记录
            this.removeById(appId);
            log.error("保存项目文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存项目文件失败");
        }

        // 5. 构建校验
        VueProjectBuilder.BuildResult buildResult = vueProjectBuilder.buildProjectWithResult(projectDir.toString());
        if (!buildResult.isSuccess()) {
            // 构建失败，清理文件和数据库记录
            cn.hutool.core.io.FileUtil.del(projectDir.toFile());
            this.removeById(appId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "项目构建失败：" + buildResult.getErrorSummary());
        }

        log.info("Vue 项目上传成功，ID: {}, 文件数: {}", appId, files.length);
        return appId;
    }

    @DubboReference
    private com.dango.supabase.service.SupabaseService supabaseService;

    @Resource
    private SupabaseClientConfig supabaseClientConfig;

    @Override
    public void initializeDatabase(Long appId, long userId) {
        // 1. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 2. 权限校验：只有应用创建者可以初始化数据库
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        }

        // 3. 校验是否已启用数据库
        if (Boolean.TRUE.equals(app.getHasDatabase())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该应用已启用数据库");
        }

        // 4. 校验代码生成类型（仅支持 VUE_PROJECT）
        String codeGenType = app.getCodeGenType();
        if (!CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅支持 Vue 项目启用数据库");
        }

        // 5. 校验项目目录是否存在
        String projectDir = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId;
        File projectDirFile = new File(projectDir);
        if (!projectDirFile.exists() || !projectDirFile.isDirectory()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目目录不存在，请先生成代码");
        }

        // 6. 调用 supabase-service 创建 Schema
        try {
            supabaseService.createSchema(appId);
            log.info("Schema 创建成功: app_{}", appId);
        } catch (Exception e) {
            log.error("创建 Schema 失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建数据库 Schema 失败: " + e.getMessage());
        }

        // 7. 写入 Supabase 客户端配置文件
        writeSupabaseClientConfig(projectDir, appId);

        // 8. 更新 package.json 添加依赖
        updatePackageJson(projectDir);

        // 9. 更新 app.has_database = true
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setHasDatabase(true);
        updateApp.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新应用状态失败");

        log.info("应用数据库初始化成功，appId: {}", appId);
    }

    /**
     * 写入 Supabase 客户端配置文件
     */
    private void writeSupabaseClientConfig(String projectDir, Long appId) {
        // 创建目录 src/integrations/supabase
        String supabaseDir = projectDir + File.separator + "src" + File.separator + "integrations" + File.separator + "supabase";
        File supabaseDirFile = new File(supabaseDir);
        if (!supabaseDirFile.exists()) {
            supabaseDirFile.mkdirs();
        }

        // 从配置文件读取 URL 和 Key
        String supabaseUrl = supabaseClientConfig.getUrl();
        String supabaseAnonKey = supabaseClientConfig.getAnonKey();

        // 写入 client.js
        String clientContent = """
            import { createClient } from '@supabase/supabase-js';

            const SUPABASE_URL = "%s";
            const SUPABASE_ANON_KEY = "%s";

            // Schema 名称（每个应用独立的数据库空间）
            export const SCHEMA_NAME = "app_%d";

            // 创建 Supabase 客户端
            // 导入方式: import { supabase } from "@/integrations/supabase/client";
            export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
                db: {
                    schema: SCHEMA_NAME
                }
            });
            """.formatted(supabaseUrl, supabaseAnonKey, appId);

        File clientFile = new File(supabaseDir, "client.js");
        FileUtil.writeString(clientContent, clientFile, StandardCharsets.UTF_8);
        log.info("Supabase 客户端配置文件写入成功: {}", clientFile.getAbsolutePath());
    }

    /**
     * 更新 package.json 添加 Supabase 依赖
     */
    private void updatePackageJson(String projectDir) {
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.warn("package.json 不存在，跳过依赖更新");
            return;
        }

        String content = FileUtil.readString(packageJsonFile, StandardCharsets.UTF_8);

        // 检查是否已包含 supabase 依赖
        if (content.contains("@supabase/supabase-js")) {
            log.info("package.json 已包含 Supabase 依赖，跳过更新");
            return;
        }

        // 在 dependencies 中添加 supabase
        // 简单的字符串替换方式
        if (content.contains("\"dependencies\"")) {
            content = content.replace(
                    "\"dependencies\": {",
                    "\"dependencies\": {\n    \"@supabase/supabase-js\": \"^2.49.4\","
            );
            FileUtil.writeString(content, packageJsonFile, StandardCharsets.UTF_8);
            log.info("package.json 已添加 Supabase 依赖");
        } else {
            log.warn("package.json 中未找到 dependencies 字段");
        }
    }

    /**
     * 格式化表结构为字符串
     * 用于传递给工作流上下文
     *
     * @param tables 表结构列表（扁平结构，每行代表一个列）
     * @return 格式化后的字符串
     */
    private String formatTableSchemas(List<com.dango.supabase.dto.TableSchemaDTO> tables) {
        if (tables == null || tables.isEmpty()) {
            return "";
        }

        // 按表名分组
        Map<String, List<com.dango.supabase.dto.TableSchemaDTO>> tableMap = tables.stream()
                .collect(Collectors.groupingBy(com.dango.supabase.dto.TableSchemaDTO::getTableName));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<com.dango.supabase.dto.TableSchemaDTO>> entry : tableMap.entrySet()) {
            String tableName = entry.getKey();
            List<com.dango.supabase.dto.TableSchemaDTO> columns = entry.getValue();

            sb.append("表 ").append(tableName).append(":\n");
            for (com.dango.supabase.dto.TableSchemaDTO column : columns) {
                sb.append("  - ").append(column.getColumnName())
                        .append(": ").append(column.getDataType());
                if (Boolean.TRUE.equals(column.getIsNullable())) {
                    sb.append(" (nullable)");
                }
                if (StrUtil.isNotBlank(column.getColumnDefault())) {
                    sb.append(" DEFAULT ").append(column.getColumnDefault());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}

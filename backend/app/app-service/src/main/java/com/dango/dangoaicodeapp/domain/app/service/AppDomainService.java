package com.dango.dangoaicodeapp.domain.app.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.codegen.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.infrastructure.config.SupabaseClientConfig;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodescreenshot.InnerScreenshotService;
import com.dango.supabase.service.SupabaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 应用领域服务 - 封装部署、数据库初始化等核心业务逻辑
 *
 * @author dango
 */
@Slf4j
@Service
public class AppDomainService {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @DubboReference
    private InnerScreenshotService innerScreenshotService;

    @DubboReference
    private SupabaseService supabaseService;

    @Resource
    private SupabaseClientConfig supabaseClientConfig;

    /**
     * 执行应用部署的核心逻辑：构建项目、复制到部署目录、生成 deployKey
     *
     * @param app 应用实体（需包含 id、codeGenType、deployKey）
     * @return 生成或已有的 deployKey
     */
    public String deployApp(App app) {
        Long appId = app.getId();
        String deployKey = app.getDeployKey();

        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }

        // 执行 Vue 项目构建
        boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
        ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");

        // 检查 dist 目录是否存在
        File distDir = new File(sourceDirPath, "dist");
        ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");

        // 将 dist 目录作为部署源
        sourceDir = distDir;
        log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());

        // 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }

        return deployKey;
    }

    /**
     * 初始化应用数据库的核心逻辑：创建 Schema、写入配置、更新 package.json
     *
     * @param app 应用实体（需包含 id、codeGenType）
     */
    public void initializeDatabase(App app) {
        Long appId = app.getId();
        String codeGenType = app.getCodeGenType();
        String projectDir = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + codeGenType + "_" + appId;

        // 调用 supabase-service 创建 Schema
        try {
            supabaseService.createSchema(appId);
            log.info("Schema 创建成功: app_{}", appId);
        } catch (Exception e) {
            log.error("创建 Schema 失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建数据库 Schema 失败: " + e.getMessage());
        }

        // 写入 Supabase 客户端配置文件
        writeSupabaseClientConfig(projectDir, appId);

        // 更新 package.json 添加依赖
        updatePackageJson(projectDir);
    }

    /**
     * 异步生成应用截图并返回截图 URL
     *
     * @param appId  应用 ID
     * @param appUrl 应用访问 URL
     * @return 截图 URL
     */
    public String generateScreenshot(Long appId, String appUrl) {
        String screenshotUrl = innerScreenshotService.generateAndUploadScreenshot(appUrl);
        log.info("应用截图生成成功, appId: {}, url: {}", appId, screenshotUrl);
        return screenshotUrl;
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
}

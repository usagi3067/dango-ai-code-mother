package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import com.dango.aicodegenerate.model.AppNameAndTagResult;

import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import com.dango.dangoaicodeapp.domain.app.service.AppDomainService;
import com.dango.dangoaicodeapp.domain.codegen.service.AppInfoGeneratorFacade;
import com.dango.dangoaicodeapp.domain.codegen.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.infrastructure.mapper.AppMapper;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @Resource
    private AppDomainService appDomainService;
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
    public String deployApp(Long appId, long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 使用聚合根的权限校验
        app.checkOwnership(userId);
        // 委托领域服务执行核心部署逻辑
        String deployKey = appDomainService.deployApp(app);
        // 使用聚合根的状态变更方法
        app.markDeployed(deployKey);
        boolean updateResult = this.updateById(app);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
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
        Thread.startVirtualThread(() -> {
            String screenshotUrl = appDomainService.generateScreenshot(appId, appUrl);
            App app = this.getById(appId);
            if (app != null) {
                app.updateCover(screenshotUrl);
                boolean updated = this.updateById(app);
                ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
            }
        });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, long userId) {
        String initPrompt = appAddRequest.getInitPrompt();
        App.validateInitPrompt(initPrompt);
        // 使用 AI 智能生成应用名称和标签
        AppNameAndTagResult appInfo = appInfoGeneratorFacade.generateAppInfo(initPrompt);
        // 使用工厂方法创建应用
        App app = App.createNew(userId, initPrompt, appInfo.getAppName(), appInfo.getTag());
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

    @Override
    public void initializeDatabase(Long appId, long userId) {
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 使用聚合根的权限校验
        app.checkOwnership(userId);
        // 使用聚合根的数据库启用方法（含业务校验）
        app.enableDatabase();
        // 校验项目目录是否存在
        String projectDir = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + app.getProjectDirName();
        File projectDirFile = new File(projectDir);
        if (!projectDirFile.exists() || !projectDirFile.isDirectory()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目目录不存在，请先生成代码");
        }
        // 委托领域服务执行核心数据库初始化逻辑
        appDomainService.initializeDatabase(app);
        // 持久化状态变更
        boolean result = this.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新应用状态失败");
        log.info("应用数据库初始化成功，appId: {}", appId);
    }
}

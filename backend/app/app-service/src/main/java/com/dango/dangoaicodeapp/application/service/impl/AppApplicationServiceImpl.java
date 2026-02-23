package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import com.dango.aicodegenerate.model.AppNameAndTagResult;

import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import com.dango.dangoaicodeapp.application.service.AppSearchService;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.app.repository.AppRepository;
import com.dango.dangoaicodeapp.domain.app.service.AppDomainService;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.domain.codegen.service.AppInfoGeneratorFacade;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppAdminUpdateRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodeuser.dto.UserDTO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.InnerUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
public class AppApplicationServiceImpl implements AppApplicationService {

    @Value("${app.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private AppRepository appRepository;
    @DubboReference
    private InnerUserService innerUserService;
    @Resource
    private AppDomainService appDomainService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private AppInfoGeneratorFacade appInfoGeneratorFacade;
    @Resource
    private AppSearchService appSearchService;
    @Resource
    private ChatHistoryService chatHistoryService;

    // ========== 查询用例 ==========

    @Override
    public AppVO getAppDetail(Long appId) {
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR);
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return getAppVO(app);
    }

    @Override
    public Page<AppVO> listMyApps(AppQueryRequest request, Long userId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageSize = request.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        request.setUserId(userId);
        QueryWrapper queryWrapper = getQueryWrapper(request);
        Page<App> appPage = appRepository.findPage(Page.of(request.getPageNum(), pageSize), queryWrapper);
        Page<AppVO> voPage = new Page<>(request.getPageNum(), pageSize, appPage.getTotalRow());
        voPage.setRecords(getAppVOList(appPage.getRecords()));
        return voPage;
    }

    @Override
    public Page<AppVO> listGoodApps(AppQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageSize = request.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        request.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = getQueryWrapper(request);
        Page<App> appPage = appRepository.findPage(Page.of(request.getPageNum(), pageSize), queryWrapper);
        Page<AppVO> voPage = new Page<>(request.getPageNum(), pageSize, appPage.getTotalRow());
        voPage.setRecords(getAppVOList(appPage.getRecords()));
        return voPage;
    }

    @Override
    public Page<AppVO> listAppsByCursor(AppQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        int pageSize = request.getPageSize();
        if (pageSize <= 0) {
            pageSize = 12;
            request.setPageSize(pageSize);
        }
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        Page<App> appPage = appSearchService.searchApps(request);
        Page<AppVO> voPage = new Page<>();
        voPage.setPageSize(appPage.getPageSize());
        voPage.setRecords(getAppVOList(appPage.getRecords()));
        return voPage;
    }

    @Override
    public Page<AppVO> adminListApps(AppQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = getQueryWrapper(request);
        Page<App> appPage = appRepository.findPage(Page.of(request.getPageNum(), request.getPageSize()), queryWrapper);
        Page<AppVO> voPage = new Page<>(request.getPageNum(), request.getPageSize(), appPage.getTotalRow());
        voPage.setRecords(getAppVOList(appPage.getRecords()));
        return voPage;
    }

    @Override
    public AppVO adminGetAppDetail(Long appId) {
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR);
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return getAppVO(app);
    }

    // ========== 命令用例 ==========

    @Override
    public Long createApp(AppAddRequest appAddRequest, long userId) {
        String initPrompt = appAddRequest.getInitPrompt();
        App.validateInitPrompt(initPrompt);

        String appName;
        String tag;

        // 如果前端已提供名称和标签，直接使用，跳过 AI 生成
        if (appAddRequest.getAppName() != null && !appAddRequest.getAppName().isBlank()
                && appAddRequest.getTag() != null && !appAddRequest.getTag().isBlank()) {
            appName = appAddRequest.getAppName();
            tag = appAddRequest.getTag();
        } else {
            AppNameAndTagResult appInfo = appInfoGeneratorFacade.generateAppInfo(initPrompt);
            appName = appInfo.getAppName();
            tag = appInfo.getTag();
        }

        App app = App.createNew(userId, initPrompt, appName, tag, appAddRequest.getCodeGenType());
        appRepository.save(app);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), app.getCodeGenType());
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

        App savedApp = appRepository.save(app);
        Long appId = savedApp.getId();

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
            appRepository.deleteById(appId);
            log.error("保存项目文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存项目文件失败");
        }

        // 5. 构建校验
        VueProjectBuilder.BuildResult buildResult = vueProjectBuilder.buildProjectWithResult(projectDir.toString());
        if (!buildResult.isSuccess()) {
            cn.hutool.core.io.FileUtil.del(projectDir.toFile());
            appRepository.deleteById(appId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "项目构建失败：" + buildResult.getErrorSummary());
        }

        log.info("Vue 项目上传成功，ID: {}, 文件数: {}", appId, files.length);
        return appId;
    }

    @Override
    public void updateApp(Long appId, String appName, String tag, long userId) {
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        app.checkOwnership(userId);
        app.updateInfo(appName, tag);
        boolean result = appRepository.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void deleteApp(Long appId, long userId) {
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        app.checkOwnership(userId);
        chatHistoryService.deleteByAppId(appId);
        appRepository.deleteById(appId);
    }

    @Override
    public String deployApp(Long appId, long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        app.checkOwnership(userId);
        String deployKey = appDomainService.deployApp(app);
        app.markDeployed(deployKey);
        boolean updateResult = appRepository.updateById(app);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        String appDeployUrl = String.format("%s/d/%s/", deployHost, deployKey);
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Override
    public void initializeDatabase(Long appId, long userId) {
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        app.checkOwnership(userId);
        app.enableDatabase();
        String projectDir = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + app.getProjectDirName();
        File projectDirFile = new File(projectDir);
        if (!projectDirFile.exists() || !projectDirFile.isDirectory()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目目录不存在，请先生成代码");
        }
        appDomainService.initializeDatabase(app);
        boolean result = appRepository.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新应用状态失败");
        log.info("应用数据库初始化成功，appId: {}", appId);
    }

    // ========== 管理员命令 ==========

    @Override
    public void adminUpdateApp(AppAdminUpdateRequest request) {
        Long id = request.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        App app = appRepository.findById(id).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        BeanUtil.copyProperties(request, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appRepository.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void adminDeleteApp(Long appId) {
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        chatHistoryService.deleteByAppId(appId);
        appRepository.deleteById(appId);
    }

    // ========== 其他 ==========

    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        Thread.startVirtualThread(() -> {
            String screenshotUrl = appDomainService.generateScreenshot(appId, appUrl);
            App app = appRepository.findById(appId).orElse(null);
            if (app != null) {
                app.updateCover(screenshotUrl);
                boolean updated = appRepository.updateById(app);
                ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
            }
        });
    }

    // ========== 私有方法 ==========

    private AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        Long userId = app.getUserId();
        if (userId != null) {
            UserDTO userDTO = innerUserService.getById(userId);
            UserVO userVO = innerUserService.toUserVO(userDTO);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    private QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
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

    private List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
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
}

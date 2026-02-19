package com.dango.dangoaicodeapp.interfaces.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.dango.dangoaicodeapp.application.service.ProjectDownloadService;
import com.dango.dangoaicodecommon.ratelimit.annotation.RateLimit;
import com.dango.dangoaicodecommon.ratelimit.enums.RateLimitType;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.model.dto.app.*;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import com.dango.dangoaicodeapp.application.service.CodeGenApplicationService;
import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.DeleteRequest;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author dango
 */
@Slf4j
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    @Lazy
    private AppApplicationService appService;

    @Resource
    private CodeGenApplicationService codeGenApplicationService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appService.createApp(appAddRequest, StpUtil.getLoginIdAsLong());
        return ResultUtils.success(appId);
    }

    /**
     * 上传 Vue 项目文件夹创建应用
     */
    @PostMapping("/upload/vue-project")
    public BaseResponse<Long> uploadVueProject(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String[] paths) {
        ThrowUtils.throwIf(files == null || files.length == 0,
                ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(paths == null || paths.length != files.length,
                ErrorCode.PARAMS_ERROR, "文件路径数量不匹配");
        long totalSize = 0;
        for (MultipartFile file : files) {
            totalSize += file.getSize();
        }
        ThrowUtils.throwIf(totalSize > 50 * 1024 * 1024,
                ErrorCode.PARAMS_ERROR, "项目总大小不能超过 50MB");
        Long appId = appService.createAppFromVueProject(files, paths, StpUtil.getLoginIdAsLong());
        return ResultUtils.success(appId);
    }

    /**
     * 更新应用（用户只能更新自己的应用名称和标签）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        appService.updateApp(appUpdateRequest.getId(), appUpdateRequest.getAppName(),
                appUpdateRequest.getTag(), StpUtil.getLoginIdAsLong());
        return ResultUtils.success(true);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        appService.deleteApp(deleteRequest.getId(), StpUtil.getLoginIdAsLong());
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.getAppDetail(id));
    }

    /**
     * 分页获取当前用户创建的应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listMyApps(appQueryRequest, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 分页获取精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.dango.dangoaicodecommon.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listGoodApps(appQueryRequest));
    }

    /**
     * 游标分页获取应用列表（支持搜索和标签筛选）
     */
    @PostMapping("/list/cursor/vo")
    public BaseResponse<Page<AppVO>> listAppByCursor(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.listAppsByCursor(appQueryRequest));
    }

    /**
     * 管理员删除应用
     */
    @PostMapping("/admin/delete")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        appService.adminDeleteApp(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 管理员更新应用
     */
    @PostMapping("/admin/update")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        appService.adminUpdateApp(appAdminUpdateRequest);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @SaCheckRole("admin")
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.adminListApps(appQueryRequest));
    }

    /**
     * 管理员根据 id 获取应用详情
     */
    @GetMapping("/admin/get/vo")
    @SaCheckRole("admin")
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.adminGetAppDetail(id));
    }

    /**
     * 应用聊天生成代码（流式 SSE）
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(required = false) String elementInfo) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        long loginUserId = StpUtil.getLoginIdAsLong();

        ElementInfo parsedElementInfo = null;
        if (StrUtil.isNotBlank(elementInfo)) {
            try {
                ElementInfoDTO dto = JSONUtil.toBean(elementInfo, ElementInfoDTO.class);
                parsedElementInfo = convertToElementInfo(dto);
                log.info("解析 elementInfo 成功: selector={}", parsedElementInfo.getSelector());
            } catch (Exception e) {
                log.warn("解析 elementInfo 失败: {}", e.getMessage());
            }
        }

        Flux<String> contentFlux = codeGenApplicationService.chatToGenCode(appId, message, parsedElementInfo, loginUserId);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 将 ElementInfoDTO 转换为 ElementInfo 实体
     */
    private ElementInfo convertToElementInfo(ElementInfoDTO dto) {
        return ElementInfo.builder()
                .tagName(dto.getTagName())
                .id(dto.getId())
                .className(dto.getClassName())
                .textContent(dto.getTextContent())
                .selector(dto.getSelector())
                .pagePath(dto.getPagePath())
                .build();
    }

    /**
     * 应用部署
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        String deployUrl = appService.deployApp(appId, StpUtil.getLoginIdAsLong());
        return ResultUtils.success(deployUrl);
    }

    /**
     * 下载应用代码
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletResponse response) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 通过 getAppDetail 校验应用存在性
        AppVO appVO = appService.getAppDetail(appId);
        ThrowUtils.throwIf(appVO == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验：仅本人可下载
        ThrowUtils.throwIf(!appVO.getUserId().equals(StpUtil.getLoginIdAsLong()),
                ErrorCode.NO_AUTH_ERROR, "无权下载该应用");
        String projectDirName = appVO.getCodeGenType() + "_" + appVO.getId();
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + projectDirName;
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        String downloadFileName = String.valueOf(appId);
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    /**
     * 初始化应用数据库
     */
    @PostMapping("/{appId}/database")
    public BaseResponse<Boolean> initializeDatabase(@PathVariable Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        appService.initializeDatabase(appId, StpUtil.getLoginIdAsLong());
        return ResultUtils.success(true);
    }
}

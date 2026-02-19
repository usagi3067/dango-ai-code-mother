package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppAdminUpdateRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用服务层（用例接口）
 *
 * @author dango
 */
public interface AppApplicationService {

    // ========== 查询用例 ==========

    /**
     * 获取应用详情
     */
    AppVO getAppDetail(Long appId);

    /**
     * 分页获取当前用户的应用列表
     */
    Page<AppVO> listMyApps(AppQueryRequest request, Long userId);

    /**
     * 分页获取精选应用列表
     */
    Page<AppVO> listGoodApps(AppQueryRequest request);

    /**
     * 游标分页获取应用列表（支持搜索和标签筛选）
     */
    Page<AppVO> listAppsByCursor(AppQueryRequest request);

    /**
     * 管理员分页获取应用列表
     */
    Page<AppVO> adminListApps(AppQueryRequest request);

    /**
     * 管理员获取应用详情
     */
    AppVO adminGetAppDetail(Long appId);

    // ========== 命令用例 ==========

    /**
     * 创建应用
     */
    Long createApp(AppAddRequest request, long userId);

    /**
     * 通过上传 Vue 项目文件夹创建应用
     */
    Long createAppFromVueProject(MultipartFile[] files, String[] paths, long userId);

    /**
     * 更新应用（用户只能更新自己的应用名称和标签）
     */
    void updateApp(Long appId, String appName, String tag, long userId);

    /**
     * 删除应用（用户只能删除自己的应用）
     */
    void deleteApp(Long appId, long userId);

    /**
     * 部署应用
     */
    String deployApp(Long appId, long userId);

    /**
     * 初始化应用数据库
     */
    void initializeDatabase(Long appId, long userId);

    // ========== 管理员命令 ==========

    /**
     * 管理员更新应用
     */
    void adminUpdateApp(AppAdminUpdateRequest request);

    /**
     * 管理员删除应用
     */
    void adminDeleteApp(Long appId);

    // ========== 其他 ==========

    /**
     * 异步生成应用截图并更新封面
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);
}

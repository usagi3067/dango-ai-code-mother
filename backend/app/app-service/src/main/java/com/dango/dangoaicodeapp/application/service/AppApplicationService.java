package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.model.vo.AppVO;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author dango
 */
public interface AppApplicationService extends IService<App> {

    /**
     * 根据实体对象获取VO对象。
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 根据查询条件获取查询条件包装器。
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 根据实体对象列表获取VO对象列表。
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 部署应用
      * @param appId 应用id
      * @param userId 用户 ID
     * @return 部署地址
     */
    String deployApp(Long appId, long userId);

    /**
     * 异步生成应用截图
      * @param appId 应用id
      * @param appUrl 应用地址
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     * @param appAddRequest
     * @param userId
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, long userId);

    /**
     * 通过上传 Vue 项目文件夹创建应用
     *
     * @param files     项目文件数组
     * @param paths     每个文件对应的相对路径
     * @param userId    用户 ID
     * @return 应用 ID
     */
    Long createAppFromVueProject(MultipartFile[] files, String[] paths, long userId);

    /**
     * 初始化应用数据库
     * <p>
     * 流程：
     * 1. 校验启用条件（codeGenType = VUE_PROJECT，项目目录存在）
     * 2. 调用 supabase-service 创建 Schema
     * 3. 写入 Supabase 客户端配置文件
     * 4. 更新 package.json 添加依赖
     * 5. 更新 app.has_database = true
     *
     * @param appId     应用 ID
     * @param userId    用户 ID
     */
    void initializeDatabase(Long appId, long userId);
}

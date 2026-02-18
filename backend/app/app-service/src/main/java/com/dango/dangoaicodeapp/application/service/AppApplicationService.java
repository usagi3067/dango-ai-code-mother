package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.model.entity.ElementInfo;
import com.dango.dangoaicodeapp.model.vo.AppVO;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

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
     * 根据应用id和用户消息生成代码
     * @param appId
     * @param message
     * @param userId
     * @return
     * @deprecated 请使用 {@link #chatToGenCode(Long, String, ElementInfo, long)} 方法，默认使用 Agent 模式
     */
    @Deprecated
    Flux<String> chatToGenCode(Long appId, String message, long userId);

    /**
     * 根据应用id和用户消息生成代码（支持 Agent 模式）
     * @param appId 应用 ID
     * @param message 用户消息
     * @param userId 用户 ID
     * @param agent 是否启用 Agent 模式（工作流模式）
     * @return 生成的代码流
     * @deprecated 请使用 {@link #chatToGenCode(Long, String, ElementInfo, long)} 方法，默认使用 Agent 模式
     */
    @Deprecated
    Flux<String> chatToGenCode(Long appId, String message, long userId, boolean agent);

    /**
     * 根据应用id和用户消息生成代码（支持 Agent 模式和元素信息）
     *
     * @param appId 应用 ID
     * @param message 用户消息
     * @param elementInfo 选中的元素信息（可选，用于修改模式）
     * @param userId 用户 ID
     * @param agent 是否启用 Agent 模式（工作流模式）
     * @return 生成的代码流
     * @deprecated 请使用 {@link #chatToGenCode(Long, String, ElementInfo, long)} 方法，默认使用 Agent 模式
     */
    @Deprecated
    Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId, boolean agent);

    /**
     * 根据应用id和用户消息生成代码（Agent 模式）
     * <p>
     * 默认使用 Agent 模式（工作流模式）生成代码
     *
     * @param appId 应用 ID
     * @param message 用户消息
     * @param elementInfo 选中的元素信息（可选，用于修改模式）
     * @param userId 用户 ID
     * @return 生成的代码流
     */
    Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId);

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

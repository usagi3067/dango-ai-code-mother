package com.dango.dangoaicodeapp.service;

import com.dango.dangoaicodeapp.model.dto.app.AppAddRequest;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.model.vo.AppVO;
import com.dango.dangoaicodeuser.model.entity.User;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author dango
 */
public interface AppService extends IService<App> {

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
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
      * @param appId 应用id
      * @param loginUser 登录用户
     * @return 部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图
      * @param appId 应用id
      * @param appUrl 应用地址
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);
}

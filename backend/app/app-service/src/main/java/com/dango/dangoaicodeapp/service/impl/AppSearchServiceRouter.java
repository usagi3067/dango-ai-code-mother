package com.dango.dangoaicodeapp.service.impl;

import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.service.AppSearchService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 搜索服务路由层
 * <p>
 * 支持金丝雀灰度，可按用户标识切换 MySQL 或 ES 实现。
 *
 * @author dango
 */
@Service
@Primary
public class AppSearchServiceRouter implements AppSearchService {

    @Resource
    private AppMySqlSearchServiceImpl mysqlSearch;

    // ES 实现（后续扩展时注入）
    // @Resource(required = false)
    // private AppEsSearchServiceImpl esSearch;

    @Value("${search.engine:mysql}")
    private String defaultEngine;

    @Override
    public Page<App> searchApps(AppQueryRequest request) {
        // 优先检查用户灰度标识
        String engine = getUserGrayEngine(request.getUserId());
        if (engine == null) {
            engine = defaultEngine;
        }

        // 后续扩展 ES 时启用
        // if ("es".equals(engine) && esSearch != null) {
        //     return esSearch.searchApps(request);
        // }

        return mysqlSearch.searchApps(request);
    }

    /**
     * 从 Redis/Nacos 获取用户灰度标识
     * <p>
     * 可按用户 ID、用户标签、白名单等策略判断
     *
     * @param userId 用户 ID
     * @return 搜索引擎标识（mysql/es），null 表示走默认配置
     */
    private String getUserGrayEngine(Long userId) {
        // TODO: 后续实现灰度逻辑
        return null;
    }
}

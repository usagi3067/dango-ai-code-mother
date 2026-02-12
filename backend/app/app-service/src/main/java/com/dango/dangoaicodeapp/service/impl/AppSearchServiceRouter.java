package com.dango.dangoaicodeapp.service.impl;

import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.service.AppSearchService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 搜索服务路由层
 * <p>
 * 支持金丝雀灰度，可按用户 ID 尾数切换 MySQL 或 ES 实现。
 *
 * @author dango
 */
@Service
@Primary
public class AppSearchServiceRouter implements AppSearchService {

    @Resource
    private AppMySqlSearchServiceImpl mysqlSearch;

    @Resource(required = false)
    private AppEsSearchServiceImpl esSearch;

    @Value("${search.engine:mysql}")
    private String defaultEngine;

    @Value("${search.es.gray.enabled:false}")
    private boolean grayEnabled;

    /**
     * 灰度用户 ID 尾数集合
     * 例如配置 0,1,2 表示 ID 尾数为 0、1、2 的用户走 ES
     */
    @Value("${search.es.gray.user-id-suffix:}")
    private Set<Integer> grayUserIdSuffix;

    @Override
    public Page<App> searchApps(AppQueryRequest request) {
        String engine = resolveEngine(request.getUserId());

        if ("es".equals(engine) && esSearch != null) {
            return esSearch.searchApps(request);
        }
        return mysqlSearch.searchApps(request);
    }

    /**
     * 解析搜索引擎
     *
     * @param userId 用户 ID
     * @return 搜索引擎标识（mysql/es）
     */
    private String resolveEngine(Long userId) {
        // 灰度开启时，按用户 ID 尾数判断
        if (grayEnabled && userId != null && grayUserIdSuffix != null && !grayUserIdSuffix.isEmpty()) {
            int suffix = (int) (userId % 10);
            if (grayUserIdSuffix.contains(suffix)) {
                return "es";
            }
        }
        return defaultEngine;
    }
}

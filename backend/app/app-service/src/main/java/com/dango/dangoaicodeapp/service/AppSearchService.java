package com.dango.dangoaicodeapp.service;

import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.mybatisflex.core.paginate.Page;

/**
 * 应用搜索服务接口
 * <p>
 * 抽象搜索逻辑，支持 MySQL 和 Elasticsearch 双实现，
 * 通过配置 + 金丝雀灰度平滑切换。
 *
 * @author dango
 */
public interface AppSearchService {

    /**
     * 游标分页搜索应用
     *
     * @param request 查询请求（包含 lastId、searchText、tag、pageSize）
     * @return 分页结果
     */
    Page<App> searchApps(AppQueryRequest request);
}

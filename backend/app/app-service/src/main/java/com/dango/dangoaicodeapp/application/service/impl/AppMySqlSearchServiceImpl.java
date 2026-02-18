package com.dango.dangoaicodeapp.application.service.impl;

import com.dango.dangoaicodeapp.application.service.AppSearchService;
import com.dango.dangoaicodeapp.infrastructure.mapper.AppMapper;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MySQL 搜索实现（稳定版本）
 *
 * @author dango
 */
@Service("mysqlSearchService")
public class AppMySqlSearchServiceImpl implements AppSearchService {

    @Resource
    private AppMapper appMapper;

    @Override
    public Page<App> searchApps(AppQueryRequest request) {
        String tag = request.getTag();
        String searchText = request.getSearchText();
        Long lastId = request.getLastId();
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 12;

        // 调用 Mapper 游标分页查询
        List<App> apps = appMapper.listAppByCursor(tag, searchText, lastId, pageSize);

        // 封装为 Page 返回
        Page<App> page = new Page<>();
        page.setRecords(apps);
        page.setPageSize(pageSize);
        // 游标分页不需要 totalRow，前端通过 records.size() < pageSize 判断是否还有更多
        return page;
    }
}

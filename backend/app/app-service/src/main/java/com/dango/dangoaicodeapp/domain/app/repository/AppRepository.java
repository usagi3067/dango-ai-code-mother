package com.dango.dangoaicodeapp.domain.app.repository;

import com.dango.dangoaicodeapp.model.entity.App;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;
import java.util.Optional;

/**
 * 应用仓储接口（领域层定义）
 */
public interface AppRepository {
    Optional<App> findById(Long id);
    App save(App app);
    boolean updateById(App app);
    boolean deleteById(Long id);
    Page<App> findPage(Page<App> page, QueryWrapper queryWrapper);
    long count(QueryWrapper queryWrapper);
    List<App> findAll(QueryWrapper queryWrapper);
}

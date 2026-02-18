package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.app.repository.AppRepository;
import com.dango.dangoaicodeapp.infrastructure.mapper.AppMapper;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 应用仓储实现（基础设施层）
 */
@Repository
@RequiredArgsConstructor
public class AppRepositoryImpl implements AppRepository {

    private final AppMapper appMapper;

    @Override
    public Optional<App> findById(Long id) {
        return Optional.ofNullable(appMapper.selectOneById(id));
    }

    @Override
    public App save(App app) {
        appMapper.insert(app);
        return app;
    }

    @Override
    public boolean updateById(App app) {
        return appMapper.update(app) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return appMapper.deleteById(id) > 0;
    }

    @Override
    public Page<App> findPage(Page<App> page, QueryWrapper queryWrapper) {
        return appMapper.paginate(page, queryWrapper);
    }

    @Override
    public long count(QueryWrapper queryWrapper) {
        return appMapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public List<App> findAll(QueryWrapper queryWrapper) {
        return appMapper.selectListByQuery(queryWrapper);
    }
}

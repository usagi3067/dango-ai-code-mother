package com.dango.dangoaicodeuser.domain.user.repository;

import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口（领域层定义，基础设施层实现）
 */
public interface UserRepository {

    /**
     * 根据 ID 查询用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据账号查询用户
     */
    Optional<User> findByAccount(String userAccount);

    /**
     * 保存用户（新增或更新）
     */
    User save(User user);

    /**
     * 删除用户
     */
    boolean deleteById(Long id);

    /**
     * 检查账号是否存在
     */
    boolean existsByAccount(String userAccount);

    /**
     * 分页查询
     */
    Page<User> findPage(Page<User> page, QueryWrapper queryWrapper);

    /**
     * 统计数量
     */
    long count(QueryWrapper queryWrapper);

    /**
     * 根据 ID 列表批量查询用户
     */
    List<User> findByIds(Collection<Long> ids);
}

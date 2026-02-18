package com.dango.dangoaicodeuser.application.service;

import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.Optional;

/**
 * 用户应用服务接口
 * 协调领域服务，处理事务
 */
public interface UserApplicationService {

    /**
     * 用户注册
     */
    Long register(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     */
    User login(String userAccount, String userPassword);

    /**
     * 用户登出
     */
    boolean logout();

    /**
     * 根据 ID 获取用户
     */
    Optional<User> getById(Long id);

    /**
     * 创建用户（管理员）
     */
    Long createUser(User user, String defaultPassword);

    /**
     * 更新用户
     */
    boolean updateUser(User user);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 分页查询用户
     */
    Page<User> pageUsers(Page<User> page, QueryWrapper queryWrapper);

    /**
     * 构建查询条件
     */
    QueryWrapper buildQueryWrapper(Long id, String userAccount, String userName,
                                   String userProfile, String userRole,
                                   String sortField, String sortOrder);
}

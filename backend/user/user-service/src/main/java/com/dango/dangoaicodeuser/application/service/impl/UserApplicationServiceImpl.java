package com.dango.dangoaicodeuser.application.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.dango.dangoaicodeuser.application.service.UserApplicationService;
import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.domain.user.repository.UserRepository;
import com.dango.dangoaicodeuser.domain.user.service.UserDomainService;
import com.dango.dangoaicodeuser.infrastructure.mapper.PermissionMapper;
import com.dango.dangoaicodeuser.infrastructure.mapper.RoleMapper;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户应用服务实现
 */
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    @Transactional
    public Long register(String userAccount, String userPassword, String checkPassword) {
        User user = userDomainService.register(userAccount, userPassword, checkPassword);
        return user.getId();
    }

    @Override
    public User login(String userAccount, String userPassword) {
        // 1. 领域服务验证
        User user = userDomainService.authenticate(userAccount, userPassword);

        // 2. Sa-Token 登录
        StpUtil.login(user.getId());

        // 3. 缓存角色和权限
        SaSession session = StpUtil.getSession();
        session.set("roleList", roleMapper.selectRoleCodesByUserId(user.getId()));
        session.set("permissionList", permissionMapper.selectPermissionCodesByUserId(user.getId()));

        return user;
    }

    @Override
    public boolean logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
        return true;
    }

    @Override
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public Long createUser(User user, String defaultPassword) {
        user.encryptPassword(defaultPassword);
        User saved = userRepository.save(user);
        return saved.getId();
    }

    @Override
    @Transactional
    public boolean updateUser(User user) {
        if (user.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    @Override
    public Page<User> pageUsers(Page<User> page, QueryWrapper queryWrapper) {
        return userRepository.findPage(page, queryWrapper);
    }

    @Override
    public QueryWrapper buildQueryWrapper(Long id, String userAccount, String userName,
                                          String userProfile, String userRole,
                                          String sortField, String sortOrder) {
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
}

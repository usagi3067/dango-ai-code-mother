package com.dango.dangoaicodeuser.infrastructure.repository;

import com.dango.dangoaicodeuser.domain.user.repository.AuthzRepository;
import com.dango.dangoaicodeuser.infrastructure.mapper.PermissionMapper;
import com.dango.dangoaicodeuser.infrastructure.mapper.RoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthzRepositoryImpl implements AuthzRepository {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> listPermissionCodesByUserId(Long userId) {
        return permissionMapper.selectPermissionCodesByUserId(userId);
    }
}

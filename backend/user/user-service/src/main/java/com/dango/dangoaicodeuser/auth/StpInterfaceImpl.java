package com.dango.dangoaicodeuser.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.dango.dangoaicodeuser.mapper.PermissionMapper;
import com.dango.dangoaicodeuser.mapper.RoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionMapper.selectPermissionCodesByUserId(Long.parseLong(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleMapper.selectRoleCodesByUserId(Long.parseLong(loginId.toString()));
    }
}

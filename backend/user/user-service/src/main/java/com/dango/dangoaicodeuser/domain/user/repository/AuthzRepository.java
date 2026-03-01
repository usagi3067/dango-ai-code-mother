package com.dango.dangoaicodeuser.domain.user.repository;

import java.util.List;

/**
 * 权限数据访问抽象
 */
public interface AuthzRepository {

    /**
     * 查询用户角色编码
     */
    List<String> listRoleCodesByUserId(Long userId);

    /**
     * 查询用户权限编码
     */
    List<String> listPermissionCodesByUserId(Long userId);
}

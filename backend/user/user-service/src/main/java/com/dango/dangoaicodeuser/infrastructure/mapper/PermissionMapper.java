package com.dango.dangoaicodeuser.infrastructure.mapper;

import com.dango.dangoaicodeuser.model.entity.Permission;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT DISTINCT p.permissionCode FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.permissionId " +
            "JOIN user_role ur ON rp.roleId = ur.roleId " +
            "WHERE ur.userId = #{userId} AND p.isDelete = 0")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}

package com.dango.dangoaicodeuser.mapper;

import com.dango.dangoaicodeuser.model.entity.Permission;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT DISTINCT p.permission_code FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.permission_id " +
            "JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.isDelete = 0")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}

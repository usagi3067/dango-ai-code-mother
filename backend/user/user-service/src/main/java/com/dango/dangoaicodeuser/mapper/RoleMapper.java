package com.dango.dangoaicodeuser.mapper;

import com.dango.dangoaicodeuser.model.entity.Role;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.roleCode FROM role r " +
            "JOIN user_role ur ON r.id = ur.roleId " +
            "WHERE ur.userId = #{userId} AND r.isDelete = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}

package com.dango.dangoaicodeuser.mapper;

import com.dango.dangoaicodeuser.model.entity.Role;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.role_code FROM role r " +
            "JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.isDelete = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}

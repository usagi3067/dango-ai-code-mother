package com.dango.dangoaicodeuser.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;

@Data
@Table("role_permission")
public class RolePermission implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long roleId;

    private Long permissionId;
}

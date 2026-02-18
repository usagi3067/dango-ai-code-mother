package com.dango.dangoaicodeuser.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Table("role")
public class Role implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String roleCode;

    private String roleName;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;
}

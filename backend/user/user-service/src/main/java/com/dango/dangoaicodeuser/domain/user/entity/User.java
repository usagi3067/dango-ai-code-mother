package com.dango.dangoaicodeuser.domain.user.entity;

import com.dango.dangoaicodeuser.domain.user.valueobject.UserRoleEnum;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体（聚合根）- 充血模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(10);

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("userAccount")
    private String userAccount;

    @Column("userPassword")
    private String userPassword;

    @Column("userName")
    private String userName;

    @Column("userAvatar")
    private String userAvatar;

    @Column("userProfile")
    private String userProfile;

    @Column("userRole")
    private String userRole;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

    // ========== 业务方法（充血模型核心）==========

    /**
     * 加密密码
     */
    public void encryptPassword(String rawPassword) {
        this.userPassword = PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, this.userPassword);
    }

    /**
     * 更新用户资料
     */
    public void updateProfile(String userName, String userAvatar, String userProfile) {
        if (userName != null) this.userName = userName;
        if (userAvatar != null) this.userAvatar = userAvatar;
        if (userProfile != null) this.userProfile = userProfile;
        this.editTime = LocalDateTime.now();
    }

    /**
     * 修改密码（验证旧密码后加密新密码）
     */
    public void changePassword(String rawOldPassword, String newPassword) {
        if (!validatePassword(rawOldPassword)) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        validatePasswordFormat(newPassword);
        this.userPassword = PASSWORD_ENCODER.encode(newPassword);
        this.editTime = LocalDateTime.now();
    }

    /**
     * 初始化新用户
     */
    public static User createNewUser(String userAccount, String rawPassword) {
        User user = new User();
        user.setUserAccount(userAccount);
        user.encryptPassword(rawPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        return user;
    }

    /**
     * 校验账号格式
     */
    public static void validateAccount(String userAccount) {
        if (userAccount == null || userAccount.length() < 4) {
            throw new IllegalArgumentException("用户账号过短");
        }
    }

    /**
     * 校验密码格式
     */
    public static void validatePasswordFormat(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("用户密码过短");
        }
    }
}

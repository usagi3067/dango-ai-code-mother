package com.dango.dangoaicodeapp.domain.app.entity;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用 实体类。
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app")
public class App implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用名称
     */
    @Column("appName")
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    @Column("initPrompt")
    private String initPrompt;

    /**
     * 应用标签
     */
    private String tag;

    /**
     * 代码生成类型（枚举）
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 部署标识
     */
    @Column("deployKey")
    private String deployKey;

    /**
     * 部署时间
     */
    @Column("deployedTime")
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority = 0;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

    /**
     * 是否启用数据库
     */
    @Column("hasDatabase")
    private Boolean hasDatabase;

    // ========== 业务方法（充血模型核心）==========

    /**
     * 校验当前用户是否为应用所有者，不是则抛异常
     */
    public void checkOwnership(Long userId) {
        if (this.userId == null || !this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        }
    }

    /**
     * 标记应用已部署
     */
    public void markDeployed(String deployKey) {
        this.deployKey = deployKey;
        this.deployedTime = LocalDateTime.now();
    }

    /**
     * 启用数据库（含前置校验）
     */
    public void enableDatabase() {
        if (Boolean.TRUE.equals(this.hasDatabase)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该应用已启用数据库");
        }
        if (!CodeGenTypeEnum.VUE_PROJECT.getValue().equals(this.codeGenType)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅支持 Vue 项目启用数据库");
        }
        this.hasDatabase = true;
        this.editTime = LocalDateTime.now();
    }

    /**
     * 更新应用基本信息
     */
    public void updateInfo(String appName, String tag) {
        boolean changed = false;
        if (appName != null) { this.appName = appName; changed = true; }
        if (tag != null) { this.tag = tag; changed = true; }
        if (changed) {
            this.editTime = LocalDateTime.now();
        }
    }

    /**
     * 更新应用封面
     */
    public void updateCover(String cover) {
        this.cover = cover;
    }

    /**
     * 获取项目目录名称
     */
    public String getProjectDirName() {
        return this.codeGenType + "_" + this.id;
    }

    /**
     * 创建新应用（工厂方法）
     */
    public static App createNew(Long userId, String initPrompt,
                                String appName, String tag, String codeGenType) {
        validateInitPrompt(initPrompt);
        LocalDateTime now = LocalDateTime.now();
        App app = new App();
        app.setUserId(userId);
        app.setInitPrompt(initPrompt);
        app.setAppName(appName);
        app.setTag(tag);
        app.setCodeGenType(codeGenType != null && !codeGenType.isBlank()
                ? codeGenType : CodeGenTypeEnum.VUE_PROJECT.getValue());
        app.setEditTime(now);
        app.setCreateTime(now);
        app.setUpdateTime(now);
        return app;
    }

    /**
     * 校验初始化 prompt
     */
    public static void validateInitPrompt(String initPrompt) {
        if (initPrompt == null || initPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        }
    }

}

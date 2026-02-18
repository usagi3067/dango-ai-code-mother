use dango_ai_code_mother;

-- =============================================
-- RBAC 权限模型建表脚本
-- =============================================

-- 角色表
CREATE TABLE IF NOT EXISTS role
(
    id         bigint auto_increment comment 'id' primary key,
    roleCode   varchar(64)                        not null comment '角色编码',
    roleName   varchar(128)                       not null comment '角色名称',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_roleCode (roleCode)
) comment '角色' collate = utf8mb4_unicode_ci;

-- 权限表
CREATE TABLE IF NOT EXISTS permission
(
    id             bigint auto_increment comment 'id' primary key,
    permissionCode varchar(128)                       not null comment '权限编码',
    permissionName varchar(128)                       not null comment '权限名称',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_permissionCode (permissionCode)
) comment '权限' collate = utf8mb4_unicode_ci;

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_role
(
    id     bigint auto_increment comment 'id' primary key,
    userId bigint not null comment '用户id',
    roleId bigint not null comment '角色id',
    UNIQUE KEY uk_userId_roleId (userId, roleId),
    INDEX idx_userId (userId),
    INDEX idx_roleId (roleId)
) comment '用户角色关联' collate = utf8mb4_unicode_ci;

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_permission
(
    id           bigint auto_increment comment 'id' primary key,
    roleId       bigint not null comment '角色id',
    permissionId bigint not null comment '权限id',
    UNIQUE KEY uk_roleId_permissionId (roleId, permissionId),
    INDEX idx_roleId (roleId),
    INDEX idx_permissionId (permissionId)
) comment '角色权限关联' collate = utf8mb4_unicode_ci;

-- =============================================
-- 初始数据
-- =============================================

-- 插入角色
INSERT INTO role (roleCode, roleName) VALUES ('admin', '管理员');
INSERT INTO role (roleCode, roleName) VALUES ('user', '普通用户');

-- 插入权限
INSERT INTO permission (permissionCode, permissionName) VALUES ('app:create', '创建应用');
INSERT INTO permission (permissionCode, permissionName) VALUES ('app:edit', '编辑应用');
INSERT INTO permission (permissionCode, permissionName) VALUES ('app:delete', '删除应用');
INSERT INTO permission (permissionCode, permissionName) VALUES ('app:view', '查看应用');
INSERT INTO permission (permissionCode, permissionName) VALUES ('chat:send', '发送消息');
INSERT INTO permission (permissionCode, permissionName) VALUES ('chat:view', '查看消息');
INSERT INTO permission (permissionCode, permissionName) VALUES ('user:manage', '用户管理');
INSERT INTO permission (permissionCode, permissionName) VALUES ('user:profile:edit', '编辑个人资料');

-- admin 拥有所有权限
INSERT INTO role_permission (roleId, permissionId)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.roleCode = 'admin';

-- user 拥有基础权限
INSERT INTO role_permission (roleId, permissionId)
SELECT r.id, p.id
FROM role r
         CROSS JOIN permission p
WHERE r.roleCode = 'user'
  AND p.permissionCode IN ('app:create', 'app:edit', 'app:view', 'chat:send', 'chat:view', 'user:profile:edit');

-- =============================================
-- 迁移现有用户角色数据
-- =============================================
INSERT INTO user_role (userId, roleId)
SELECT u.id, r.id
FROM user u
         INNER JOIN role r ON r.roleCode = u.userRole
WHERE u.isDelete = 0;

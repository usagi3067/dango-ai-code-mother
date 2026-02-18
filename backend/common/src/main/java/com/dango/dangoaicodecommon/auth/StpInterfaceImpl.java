package com.dango.dangoaicodecommon.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据提供（从 Session 缓存读取）
 * <p>
 * 角色和权限在用户登录时由 user-service 写入 Sa-Token Session，
 * 所有微服务共享同一 Redis，因此都能读取到。
 *
 * @author dango
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        return session.get("permissionList", Collections.emptyList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        return session.get("roleList", Collections.emptyList());
    }
}

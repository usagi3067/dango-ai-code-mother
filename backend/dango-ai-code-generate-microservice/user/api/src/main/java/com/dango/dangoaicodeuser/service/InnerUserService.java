package com.dango.dangoaicodeuser.service;

import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.dango.dangoaicodeuser.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户内部服务接口
 * 定义供其他微服务内部调用的方法，不对外暴露
 *
 * @author dango
 */
public interface InnerUserService {

    /**
     * 根据 ID 列表批量查询用户
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    List<User> listByIds(Collection<? extends Serializable> ids);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    User getById(Serializable id);

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    UserVO getUserVO(User user);

    /**
     * 获取当前登录用户（静态方法，避免跨服务调用）
     * 由于 HttpServletRequest 对象不好在网络中传递，因此采用静态方法
     *
     * @param request HTTP 请求
     * @return 当前登录用户
     */
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}

package com.dango.dangoaicodeuser.service;

import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodeuser.model.dto.user.UserQueryRequest;
import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.vo.LoginUserVO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import static com.dango.dangoaicodeuser.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务层（本服务内部使用）
 * 对其他微服务提供的接口请使用 {@link InnerUserService}
 *
 * @author dango
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 密码加密
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @param user 用户实体
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HTTP 请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HTTP 请求
     * @return 是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户查询条件
     *
     * @param userQueryRequest 查询请求
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 根据用户列表获取用户信息列表
     *
     * @param records 用户列表
     * @return 用户 VO 列表
     */
    List<UserVO> getUserVOList(List<User> records);

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

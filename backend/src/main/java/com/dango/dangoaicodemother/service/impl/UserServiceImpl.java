package com.dango.dangoaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodemother.exception.BusinessException;
import com.dango.dangoaicodemother.exception.ErrorCode;
import com.dango.dangoaicodemother.model.enums.UserRoleEnum;
import com.dango.dangoaicodemother.model.vo.LoginUserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.dango.dangoaicodemother.model.entity.User;
import com.dango.dangoaicodemother.mapper.UserMapper;
import com.dango.dangoaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static com.dango.dangoaicodemother.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author dango
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{
    
    // BCrypt 密码编码器（cost=10，即 2^10=1024 轮迭代）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密（使用 BCrypt）
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 加密密码（使用 BCrypt）
     * @param userPassword 原始密码
     * @return BCrypt 哈希后的密码（包含随机盐值）
     */
    public String getEncryptPassword(String userPassword) {
        return passwordEncoder.encode(userPassword);
    }
    
    /**
     * 验证密码
     * @param rawPassword 用户输入的原始密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return 密码是否匹配
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 根据账号查询用户
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 验证密码（使用 BCrypt）
        if (!verifyPassword(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }


    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

}

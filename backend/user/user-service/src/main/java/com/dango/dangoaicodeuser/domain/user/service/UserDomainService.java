package com.dango.dangoaicodeuser.domain.user.service;

import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.domain.user.repository.UserRepository;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务
 * 处理跨实体或需要仓储的业务逻辑
 */
@Service
public class UserDomainService {

    @Resource
    private UserRepository userRepository;

    /**
     * 注册新用户
     */
    public User register(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        User.validateAccount(userAccount);
        User.validatePassword(userPassword);

        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 检查账号是否重复
        if (userRepository.existsByAccount(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 3. 创建用户（使用充血模型的工厂方法）
        User user = User.createNewUser(userAccount, userPassword);

        // 4. 保存
        return userRepository.save(user);
    }

    /**
     * 用户登录验证
     */
    public User authenticate(String userAccount, String userPassword) {
        // 1. 校验参数
        User.validateAccount(userAccount);
        User.validatePassword(userPassword);

        // 2. 查询用户
        User user = userRepository.findByAccount(userAccount)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误"));

        // 3. 验证密码（使用充血模型的方法）
        if (!user.validatePassword(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        return user;
    }

    /**
     * 检查账号是否可用
     */
    public boolean isAccountAvailable(String userAccount) {
        return !userRepository.existsByAccount(userAccount);
    }
}

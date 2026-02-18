package com.dango.dangoaicodeuser.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodeuser.mapper.PermissionMapper;
import com.dango.dangoaicodeuser.mapper.RoleMapper;
import com.dango.dangoaicodeuser.mapper.UserMapper;
import com.dango.dangoaicodeuser.model.dto.user.UserQueryRequest;
import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.enums.UserRoleEnum;
import com.dango.dangoaicodeuser.model.vo.LoginUserVO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务层实现（本服务内部使用）
 *
 * @author dango
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @jakarta.annotation.Resource
    private RoleMapper roleMapper;

    @jakarta.annotation.Resource
    private PermissionMapper permissionMapper;

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
    @Override
    public String getEncryptPassword(String userPassword) {
        return passwordEncoder.encode(userPassword);
    }

    /**
     * 验证密码
     * @param rawPassword 用户输入的原始密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return 密码是否匹配
     */
    @Override
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
    public LoginUserVO userLogin(String userAccount, String userPassword) {
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
        // 4. Sa-Token 登录
        StpUtil.login(user.getId());
        // 5. 缓存角色和权限到 Sa-Token Session（供所有微服务使用）
        SaSession session = StpUtil.getSession();
        session.set("roleList", roleMapper.selectRoleCodesByUserId(user.getId()));
        session.set("permissionList", permissionMapper.selectPermissionCodesByUserId(user.getId()));
        // 6. 获得脱敏后的用户信息，并设置 Token
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        loginUserVO.setTokenName(StpUtil.getTokenName());
        loginUserVO.setTokenValue(StpUtil.getTokenValue());
        return loginUserVO;
    }



    @Override
    public boolean userLogout() {
        StpUtil.checkLogin();
        StpUtil.logout();
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }


    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

}

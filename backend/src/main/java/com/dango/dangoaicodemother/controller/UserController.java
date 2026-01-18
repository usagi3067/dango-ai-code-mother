package com.dango.dangoaicodemother.controller;

import com.dango.dangoaicodemother.common.BaseResponse;
import com.dango.dangoaicodemother.common.ResultUtils;
import com.dango.dangoaicodemother.exception.ErrorCode;
import com.dango.dangoaicodemother.exception.ThrowUtils;
import com.dango.dangoaicodemother.model.dto.user.UserLoginRequest;
import com.dango.dangoaicodemother.model.dto.user.UserRegisterRequest;
import com.dango.dangoaicodemother.model.entity.User;
import com.dango.dangoaicodemother.model.vo.LoginUserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.dango.dangoaicodemother.service.UserService;

/**
 * 用户 控制层。
 *
 * @author dango
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

}

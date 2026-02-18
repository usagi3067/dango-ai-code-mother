package com.dango.dangoaicodeuser.interfaces.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.DeleteRequest;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodeuser.application.service.UserApplicationService;
import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.interfaces.assembler.UserAssembler;
import com.dango.dangoaicodeuser.model.dto.user.*;
import com.dango.dangoaicodeuser.model.vo.LoginUserVO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private UserAssembler userAssembler;

    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = userApplicationService.register(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword()
        );
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.login(request.getUserAccount(), request.getUserPassword());
        LoginUserVO vo = userAssembler.toLoginVO(user);
        vo.setTokenName(StpUtil.getTokenName());
        vo.setTokenValue(StpUtil.getTokenValue());
        return ResultUtils.success(vo);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userApplicationService.getById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        return ResultUtils.success(userAssembler.toLoginVO(user));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout() {
        return ResultUtils.success(userApplicationService.logout());
    }

    @PostMapping("/add")
    @SaCheckRole("admin")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(request, user);
        Long userId = userApplicationService.createUser(user, "12345678");
        return ResultUtils.success(userId);
    }

    @GetMapping("/get")
    @SaCheckRole("admin")
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return ResultUtils.success(userAssembler.toVO(user));
    }

    @PostMapping("/delete")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.deleteUser(request.getId()));
    }

    @PostMapping("/update")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(request, user);
        return ResultUtils.success(userApplicationService.updateUser(user));
    }

    @PostMapping("/list/page/vo")
    @SaCheckRole("admin")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<User> page = Page.of(request.getPageNum(), request.getPageSize());
        var queryWrapper = userApplicationService.buildQueryWrapper(
                request.getId(), request.getUserAccount(), request.getUserName(),
                request.getUserProfile(), request.getUserRole(),
                request.getSortField(), request.getSortOrder()
        );
        Page<User> userPage = userApplicationService.pageUsers(page, queryWrapper);
        Page<UserVO> voPage = new Page<>(request.getPageNum(), request.getPageSize(), userPage.getTotalRow());
        voPage.setRecords(userAssembler.toVOList(userPage.getRecords()));
        return ResultUtils.success(voPage);
    }
}

package com.dango.dangoaicodeuser.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodeuser.application.service.UserProfileApplicationService;
import com.dango.dangoaicodeuser.model.dto.user.ChangePasswordRequest;
import com.dango.dangoaicodeuser.model.dto.user.UserProfileUpdateRequest;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user/profile")
public class UserProfileController {

    @Resource
    private UserProfileApplicationService userProfileApplicationService;

    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyProfile(@RequestBody UserProfileUpdateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(userProfileApplicationService.updateMyProfile(userId, request));
    }

    @PostMapping("/upload-avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        String url = userProfileApplicationService.uploadAvatar(userId, file);
        return ResultUtils.success(url);
    }

    @PostMapping("/change-password")
    public BaseResponse<Boolean> changePassword(@RequestBody ChangePasswordRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(userProfileApplicationService.changePassword(userId, request));
    }
}

package com.dango.dangoaicodeuser.application.service.impl;

import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.manager.CosManager;
import com.dango.dangoaicodeuser.application.service.UserProfileApplicationService;
import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.domain.user.repository.UserRepository;
import com.dango.dangoaicodeuser.model.dto.user.ChangePasswordRequest;
import com.dango.dangoaicodeuser.model.dto.user.UserProfileUpdateRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserProfileApplicationServiceImpl implements UserProfileApplicationService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    @Resource
    private UserRepository userRepository;

    @Resource
    private CosManager cosManager;

    @Override
    @Transactional
    public boolean updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));

        String userName = request.getUserName();
        if (userName != null) {
            if (userName.isBlank() || userName.length() < 2 || userName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度需在 2-20 字符之间");
            }
        }

        String userProfile = request.getUserProfile();
        if (userProfile != null && userProfile.length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介不能超过 200 字符");
        }

        user.updateProfile(request.getUserName(), request.getUserAvatar(), request.getUserProfile());
        userRepository.save(user);
        return true;
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 jpg/png/gif/webp 格式");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String key = String.format("/avatar/%s/%s%s", userId, UUID.randomUUID(), suffix);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("avatar_", suffix);
            file.transferTo(tempFile);
            String url = cosManager.uploadFile(key, tempFile);
            if (url == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
            }
            return url;
        } catch (IOException e) {
            log.error("头像上传异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, ChangePasswordRequest request) {
        if (request.getOldPassword() == null || request.getNewPassword() == null || request.getCheckPassword() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (!request.getNewPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));

        try {
            user.changePassword(request.getOldPassword(), request.getNewPassword());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, e.getMessage());
        }

        userRepository.save(user);
        return true;
    }
}
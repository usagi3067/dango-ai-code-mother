package com.dango.dangoaicodeuser.application.service;

import com.dango.dangoaicodeuser.model.dto.user.ChangePasswordRequest;
import com.dango.dangoaicodeuser.model.dto.user.UserProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileApplicationService {
    boolean updateMyProfile(Long userId, UserProfileUpdateRequest request);
    String uploadAvatar(Long userId, MultipartFile file);
    boolean changePassword(Long userId, ChangePasswordRequest request);
}

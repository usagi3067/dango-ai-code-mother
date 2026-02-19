package com.dango.dangoaicodeuser.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserProfileUpdateRequest implements Serializable {
    private String userName;
    private String userAvatar;
    private String userProfile;
    private static final long serialVersionUID = 1L;
}

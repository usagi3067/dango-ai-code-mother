package com.dango.dangoaicodeuser.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.model.vo.LoginUserVO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户对象转换器
 */
@Component
public class UserAssembler {

    public UserVO toVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    public LoginUserVO toLoginVO(User user) {
        if (user == null) return null;
        LoginUserVO vo = new LoginUserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    public List<UserVO> toVOList(List<User> users) {
        if (users == null) return List.of();
        return users.stream().map(this::toVO).collect(Collectors.toList());
    }
}

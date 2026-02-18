package com.dango.dangoaicodeuser.interfaces.facade;

import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.domain.user.repository.UserRepository;
import com.dango.dangoaicodeuser.dto.UserDTO;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.InnerUserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内部服务实现（防腐层/门面）
 * 通过 Dubbo 暴露给其他微服务调用
 * 负责将内部领域模型转换为外部 DTO
 *
 * @author dango
 */
@Service
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserRepository userRepository;

    @Override
    public List<UserDTO> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository.findByIds(ids).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getById(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public UserVO toUserVO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userDTO, userVO);
        return userVO;
    }

    /**
     * 将领域层 User 实体转换为 UserDTO（用于 Dubbo 传输）
     */
    private UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .userAccount(user.getUserAccount())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .userProfile(user.getUserProfile())
                .userRole(user.getUserRole())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}

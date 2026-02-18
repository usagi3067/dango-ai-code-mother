package com.dango.dangoaicodeuser.service.impl;

import com.dango.dangoaicodeuser.infrastructure.mapper.UserMapper;
import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.InnerUserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内部服务实现（对其他微服务提供）
 * 通过 Dubbo 暴露给其他微服务调用
 *
 * @author dango
 */
@Service
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> longIds = ids.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
        List<com.dango.dangoaicodeuser.domain.user.entity.User> domainUsers =
                userMapper.selectListByIds(longIds);
        return domainUsers.stream()
                .map(this::toApiUser)
                .collect(Collectors.toList());
    }

    @Override
    public User getById(Serializable id) {
        if (id == null) {
            return null;
        }
        com.dango.dangoaicodeuser.domain.user.entity.User domainUser =
                userMapper.selectOneById(Long.valueOf(id.toString()));
        return domainUser != null ? toApiUser(domainUser) : null;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 将领域层 User 转换为 API 层 User（用于 Dubbo 传输）
     */
    private User toApiUser(com.dango.dangoaicodeuser.domain.user.entity.User domainUser) {
        User apiUser = new User();
        BeanUtils.copyProperties(domainUser, apiUser);
        return apiUser;
    }
}

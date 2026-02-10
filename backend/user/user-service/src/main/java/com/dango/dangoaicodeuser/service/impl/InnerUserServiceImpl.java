package com.dango.dangoaicodeuser.service.impl;

import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.vo.UserVO;
import com.dango.dangoaicodeuser.service.InnerUserService;
import com.dango.dangoaicodeuser.service.UserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

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
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }
}

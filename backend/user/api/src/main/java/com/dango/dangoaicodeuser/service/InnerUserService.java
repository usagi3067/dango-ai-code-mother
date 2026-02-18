package com.dango.dangoaicodeuser.service;

import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.model.vo.UserVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 用户内部服务接口
 * 定义供其他微服务内部调用的方法，不对外暴露
 *
 * @author dango
 */
public interface InnerUserService {

    /**
     * 根据 ID 列表批量查询用户
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    List<User> listByIds(Collection<? extends Serializable> ids);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    User getById(Serializable id);

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    UserVO getUserVO(User user);
}

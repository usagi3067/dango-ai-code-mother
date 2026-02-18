package com.dango.dangoaicodeuser.service;

import com.dango.dangoaicodeuser.dto.UserDTO;
import com.dango.dangoaicodeuser.model.vo.UserVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户内部服务接口
 * 定义供其他微服务内部调用的方法，通过 Dubbo 暴露
 *
 * @author dango
 */
public interface InnerUserService {

    /**
     * 根据 ID 列表批量查询用户
     *
     * @param ids 用户 ID 列表
     * @return 用户 DTO 列表
     */
    List<UserDTO> listByIds(Collection<Long> ids);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户 DTO
     */
    UserDTO getById(Long id);

    /**
     * 将 UserDTO 转换为脱敏的 UserVO
     *
     * @param userDTO 用户 DTO
     * @return 用户 VO
     */
    UserVO toUserVO(UserDTO userDTO);
}

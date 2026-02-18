package com.dango.dangoaicodeuser.infrastructure.repository;

import com.dango.dangoaicodeuser.domain.user.entity.User;
import com.dango.dangoaicodeuser.domain.user.repository.UserRepository;
import com.dango.dangoaicodeuser.infrastructure.mapper.UserMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现（基础设施层）
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    @Resource
    private UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectOneById(id));
    }

    @Override
    public Optional<User> findByAccount(String userAccount) {
        QueryWrapper query = QueryWrapper.create()
                .eq("userAccount", userAccount);
        return Optional.ofNullable(userMapper.selectOneByQuery(query));
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }

    @Override
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public boolean existsByAccount(String userAccount) {
        QueryWrapper query = QueryWrapper.create()
                .eq("userAccount", userAccount);
        return userMapper.selectCountByQuery(query) > 0;
    }

    @Override
    public Page<User> findPage(Page<User> page, QueryWrapper queryWrapper) {
        return userMapper.paginate(page, queryWrapper);
    }

    @Override
    public long count(QueryWrapper queryWrapper) {
        return userMapper.selectCountByQuery(queryWrapper);
    }
}

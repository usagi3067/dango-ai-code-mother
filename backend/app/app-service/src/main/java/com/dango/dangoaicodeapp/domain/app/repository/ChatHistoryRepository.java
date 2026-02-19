package com.dango.dangoaicodeapp.domain.app.repository;

import com.dango.dangoaicodeapp.domain.app.entity.ChatHistory;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;
import java.util.Optional;

/**
 * 聊天历史仓储接口（领域层定义）
 */
public interface ChatHistoryRepository {
    Optional<ChatHistory> findById(Long id);
    ChatHistory save(ChatHistory chatHistory);
    List<ChatHistory> findByAppId(Long appId, int limit);
    boolean deleteByAppId(Long appId);
    Page<ChatHistory> findPage(Page<ChatHistory> page, QueryWrapper queryWrapper);
    boolean removeByQuery(QueryWrapper queryWrapper);
    List<ChatHistory> findAll(QueryWrapper queryWrapper);
}

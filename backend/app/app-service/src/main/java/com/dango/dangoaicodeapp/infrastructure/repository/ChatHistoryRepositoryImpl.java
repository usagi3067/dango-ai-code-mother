package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.app.repository.ChatHistoryRepository;
import com.dango.dangoaicodeapp.infrastructure.mapper.ChatHistoryMapper;
import com.dango.dangoaicodeapp.domain.app.entity.ChatHistory;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.dango.dangoaicodeapp.domain.app.entity.table.ChatHistoryTableDef.CHAT_HISTORY;

/**
 * 聊天历史仓储实现（基础设施层）
 */
@Repository
@RequiredArgsConstructor
public class ChatHistoryRepositoryImpl implements ChatHistoryRepository {

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public Optional<ChatHistory> findById(Long id) {
        return Optional.ofNullable(chatHistoryMapper.selectOneById(id));
    }

    @Override
    public ChatHistory save(ChatHistory chatHistory) {
        chatHistoryMapper.insert(chatHistory);
        return chatHistory;
    }

    @Override
    public List<ChatHistory> findByAppId(Long appId, int limit) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId))
                .orderBy(CHAT_HISTORY.CREATE_TIME, false)
                .limit(limit);
        return chatHistoryMapper.selectListByQuery(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(CHAT_HISTORY.APP_ID.eq(appId));
        return chatHistoryMapper.deleteByQuery(queryWrapper) > 0;
    }

    @Override
    public Page<ChatHistory> findPage(Page<ChatHistory> page, QueryWrapper queryWrapper) {
        return chatHistoryMapper.paginate(page, queryWrapper);
    }

    @Override
    public boolean removeByQuery(QueryWrapper queryWrapper) {
        return chatHistoryMapper.deleteByQuery(queryWrapper) > 0;
    }

    @Override
    public List<ChatHistory> findAll(QueryWrapper queryWrapper) {
        return chatHistoryMapper.selectListByQuery(queryWrapper);
    }
}

package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodeapp.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 对话历史 服务层。
 *
 * @author dango
 */
public interface ChatHistoryService {

    /**
     * 保存用户消息
     *
     * @param appId   应用ID
     * @param userId  用户ID
     * @param message 消息内容
     * @return 是否保存成功
     */
    boolean saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存AI消息
     *
     * @param appId   应用ID
     * @param userId  用户ID
     * @param message 消息内容
     * @return 是否保存成功
     */
    boolean saveAiMessage(Long appId, Long userId, String message);

    /**
     * 获取应用的对话历史（游标分页）
     *
     * @param appId     应用ID
     * @param lastId    游标ID，用于向前加载更早的消息
     * @param size      每页数量
     * @param userId    当前登录用户ID
     * @return 对话历史VO分页
     */
    Page<ChatHistoryVO> listByAppId(Long appId, Long lastId, int size, long userId);

    /**
     * 删除应用的所有对话历史
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 管理员分页查询对话历史
     *
     * @param request 查询请求
     * @return 对话历史VO分页
     */
    Page<ChatHistoryVO> adminListChatHistory(ChatHistoryQueryRequest request);

    /**
     * 加载对话历史到会话记忆中
     *
     * @param appId      应用ID
     * @param chatMemory 会话记忆
     * @param maxCount   最大数量
     * @return 加载的消息数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}

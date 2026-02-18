package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodeapp.model.entity.ChatHistory;
import com.dango.dangoaicodeapp.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author dango
 */
public interface ChatHistoryService extends IService<ChatHistory> {

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
     * 获取查询条件包装器（管理员用）
     *
     * @param request 查询请求
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest request);

    /**
     * 实体转VO
     *
     * @param chatHistory 对话历史实体
     * @return 对话历史VO
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 实体列表转VO列表
     *
     * @param chatHistoryList 对话历史实体列表
     * @return 对话历史VO列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 加载对话历史到会话记忆中
     * @param appId 应用ID
     * @param chatMemory 会话记忆
     * @param maxCount 最大数量
     * @return
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}

package com.dango.dangoaicodeapp.application.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodeapp.domain.app.repository.AppRepository;
import com.dango.dangoaicodeapp.domain.app.repository.ChatHistoryRepository;
import com.dango.dangoaicodeapp.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.app.entity.ChatHistory;
import com.dango.dangoaicodeapp.model.enums.MessageTypeEnum;
import com.dango.dangoaicodeapp.model.vo.ChatHistoryVO;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author dango
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Resource
    private AppRepository appRepository;

    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @Override
    public boolean saveUserMessage(Long appId, Long userId, String message) {
        return saveMessage(appId, userId, message, MessageTypeEnum.USER.getValue());
    }

    @Override
    public boolean saveAiMessage(Long appId, Long userId, String message) {
        return saveMessage(appId, userId, message, MessageTypeEnum.AI.getValue());
    }

    /**
     * 保存消息的通用方法
     *
     * @param appId       应用ID
     * @param userId      用户ID
     * @param message     消息内容
     * @param messageType 消息类型
     * @return 是否保存成功
     */
    private boolean saveMessage(Long appId, Long userId, String message, String messageType) {
        ChatHistory chatHistory;
        if (MessageTypeEnum.USER.getValue().equals(messageType)) {
            chatHistory = ChatHistory.createUserMessage(appId, userId, message);
        } else {
            chatHistory = ChatHistory.createAiMessage(appId, userId, message);
        }
        chatHistoryRepository.save(chatHistory);
        return true;
    }

    @Override
    public Page<ChatHistoryVO> listByAppId(Long appId, Long lastId, int size, long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        // 限制每页数量
        if (size <= 0 || size > 20) {
            size = 10;
        }

        // 查询应用信息
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 权限校验：仅应用创建者或管理员可以查看
        if (!StpUtil.hasRole("admin")) {
            app.checkOwnership(userId);
        }

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);

        // 游标分页：如果提供了 lastId，则查询 id 小于 lastId 的记录
        if (lastId != null && lastId > 0) {
            queryWrapper.lt("id", lastId);
        }

        // 按 id 降序排列
        queryWrapper.orderBy("id", false);

        // 执行分页查询
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findPage(Page.of(1, size), queryWrapper);

        // 转换为 VO 分页，保持返回格式统一
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(1, size, chatHistoryPage.getTotalRow());
        chatHistoryVOPage.setRecords(getChatHistoryVOList(chatHistoryPage.getRecords()));

        return chatHistoryVOPage;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        return chatHistoryRepository.deleteByAppId(appId);
    }

    @Override
    public Page<ChatHistoryVO> adminListChatHistory(ChatHistoryQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();
        // 构建查询条件
        QueryWrapper queryWrapper = getQueryWrapper(request);
        // 分页查询
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findPage(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        chatHistoryVOPage.setRecords(getChatHistoryVOList(chatHistoryPage.getRecords()));
        return chatHistoryVOPage;
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = chatHistoryRepository.findAll(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            historyList = CollUtil.reverse(historyList);
            int loadedCount = 0;
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (history.isUserMessage()) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (history.isAiMessage()) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取查询条件包装器（管理员用）
     */
    private QueryWrapper getQueryWrapper(ChatHistoryQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long appId = request.getAppId();
        Long lastId = request.getLastId();
        Long userId = request.getUserId();
        String messageType = request.getMessageType();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create();

        if (appId != null && appId > 0) {
            queryWrapper.eq("appId", appId);
        }
        if (lastId != null && lastId > 0) {
            queryWrapper.lt("id", lastId);
        }
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }
        if (StrUtil.isNotBlank(messageType)) {
            queryWrapper.eq("messageType", messageType);
        }
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("id", false);
        }

        return queryWrapper;
    }

    /**
     * 实体转VO
     */
    private ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);
        return chatHistoryVO;
    }

    /**
     * 实体列表转VO列表
     */
    private List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        return chatHistoryList.stream()
                .map(this::getChatHistoryVO)
                .collect(Collectors.toList());
    }

}

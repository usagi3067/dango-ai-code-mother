package com.dango.dangoaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodemother.exception.BusinessException;
import com.dango.dangoaicodemother.exception.ErrorCode;
import com.dango.dangoaicodemother.exception.ThrowUtils;
import com.dango.dangoaicodemother.mapper.ChatHistoryMapper;
import com.dango.dangoaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodemother.model.entity.App;
import com.dango.dangoaicodemother.model.entity.ChatHistory;
import com.dango.dangoaicodemother.model.entity.User;
import com.dango.dangoaicodemother.model.enums.MessageTypeEnum;
import com.dango.dangoaicodemother.model.enums.UserRoleEnum;
import com.dango.dangoaicodemother.model.vo.ChatHistoryVO;
import com.dango.dangoaicodemother.service.AppService;
import com.dango.dangoaicodemother.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

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
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 构建对话历史实体
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .build();

        // 保存到数据库
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存对话历史失败");
        return true;
    }

    @Override
    public Page<ChatHistoryVO> listByAppId(Long appId, Long lastId, int size, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 限制每页数量
        if (size <= 0 || size > 20) {
            size = 10;
        }

        // 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 权限校验：仅应用创建者或管理员可以查看
        boolean isOwner = app.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的对话历史");
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

        /**
         * 执行分页查询
         * 
         * 这里使用 Page.of(1, size) 而不是 list() + limit 的原因：
         * 
         * 1. page() vs list() 的区别：
         *    - list(queryWrapper): 只返回 List<T>，需要手动添加 limit，无法获取总记录数
         *    - page(Page, queryWrapper): 返回 Page<T>，自动处理分页，包含 totalRow 等元数据
         * 
         * 2. 为什么 pageNum 固定为 1：
         *    - 游标分页的核心是通过 lastId 来定位数据，而不是通过页码
         *    - 每次查询都是"从 lastId 往前取 size 条"，本质上永远是"第一页"
         *    - 传统分页: SELECT * FROM t LIMIT (pageNum-1)*size, size
         *    - 游标分页: SELECT * FROM t WHERE id < lastId ORDER BY id DESC LIMIT size
         * 
         * 3. 游标分页的优势：
         *    - 性能更好：避免了 OFFSET 带来的深分页性能问题
         *    - 数据一致性：不会因为新增数据导致重复或遗漏
         *    - 适合聊天场景：用户向上滚动加载更早的消息
         */
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(1, size), queryWrapper);

        // 转换为 VO 分页，保持返回格式统一
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(1, size, chatHistoryPage.getTotalRow());
        chatHistoryVOPage.setRecords(getChatHistoryVOList(chatHistoryPage.getRecords()));

        return chatHistoryVOPage;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");

        // 构建删除条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);

        // 执行删除（逻辑删除）
        return this.remove(queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest request) {
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

        // 按应用 ID 过滤
        if (appId != null && appId > 0) {
            queryWrapper.eq("appId", appId);
        }

        // 游标分页
        if (lastId != null && lastId > 0) {
            queryWrapper.lt("id", lastId);
        }

        // 按用户 ID 过滤
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }

        // 按消息类型过滤
        if (StrUtil.isNotBlank(messageType)) {
            queryWrapper.eq("messageType", messageType);
        }

        // 排序：默认按 id 降序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("id", false);
        }

        return queryWrapper;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);
        return chatHistoryVO;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        return chatHistoryList.stream()
                .map(this::getChatHistoryVO)
                .collect(Collectors.toList());
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            // 按创建时间倒序（最新的在前）查询最近的 maxCount 条记录
            // 偏移量为 1 是为了跳过当前用户刚刚发送、已经保存到数据库但不需要重复加入记忆的消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 数据库查询结果是[新 -> 旧]，而 ChatMemory 需要[旧 -> 新]的顺序来构建上下文
            // 因此需要将列表反转，确保对话逻辑的正确性
            historyList = CollUtil.reverse(historyList);
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}

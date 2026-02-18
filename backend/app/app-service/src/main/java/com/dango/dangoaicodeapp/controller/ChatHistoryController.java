package com.dango.dangoaicodeapp.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.dango.dangoaicodeapp.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodeapp.model.entity.ChatHistory;
import com.dango.dangoaicodeapp.model.vo.ChatHistoryVO;
import com.dango.dangoaicodeapp.service.ChatHistoryService;
import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodeuser.model.entity.User;
import com.dango.dangoaicodeuser.service.InnerUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话历史 控制层。
 *
 * @author dango
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @DubboReference
    private InnerUserService innerUserService;

    /**
     * 获取当前登录用户
     */
    private User getLoginUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = innerUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 获取应用的对话历史（游标分页）
     *
     * @param appId   应用 ID（路径参数）
     * @param lastId  游标 ID，用于向前加载更早的消息（可选）
     * @param size    每页数量，默认 10，最大 20（可选）
     * @return 对话历史 VO 分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryByAppId(
            @PathVariable Long appId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = getLoginUser();
        // 调用服务获取对话历史
        Page<ChatHistoryVO> chatHistoryVOPage = chatHistoryService.listByAppId(appId, lastId, size, loginUser);
        return ResultUtils.success(chatHistoryVOPage);
    }

    /**
     * 管理员分页查询对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史 VO 分页
     */
    @PostMapping("/admin/list/page/vo")
    @SaCheckRole("admin")
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryByPageForAdmin(
            @RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 构建查询条件
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        // 分页查询
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryService.getChatHistoryVOList(chatHistoryPage.getRecords());
        chatHistoryVOPage.setRecords(chatHistoryVOList);
        return ResultUtils.success(chatHistoryVOPage);
    }
}

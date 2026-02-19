package com.dango.dangoaicodeapp.interfaces.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.dango.dangoaicodeapp.model.dto.chathistory.ChatHistoryQueryRequest;
import com.dango.dangoaicodeapp.model.vo.ChatHistoryVO;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        Page<ChatHistoryVO> chatHistoryVOPage = chatHistoryService.listByAppId(appId, lastId, size, StpUtil.getLoginIdAsLong());
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
        return ResultUtils.success(chatHistoryService.adminListChatHistory(chatHistoryQueryRequest));
    }
}

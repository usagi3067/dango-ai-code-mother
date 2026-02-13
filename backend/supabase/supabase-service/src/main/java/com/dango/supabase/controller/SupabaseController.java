package com.dango.supabase.controller;

import com.dango.dangoaicodecommon.common.BaseResponse;
import com.dango.dangoaicodecommon.common.ResultUtils;
import com.dango.supabase.dto.SupabaseConfigDTO;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.dto.TableSummaryDTO;
import com.dango.supabase.service.SupabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Supabase 控制器（用于测试和调试）
 *
 * @author dango
 */
@Tag(name = "Supabase 管理")
@RestController
@RequestMapping("/supabase")
@RequiredArgsConstructor
public class SupabaseController {

    private final SupabaseService supabaseService;

    @Operation(summary = "创建 Schema")
    @PostMapping("/schema/{appId}")
    public BaseResponse<SupabaseConfigDTO> createSchema(@PathVariable Long appId) {
        SupabaseConfigDTO config = supabaseService.createSchema(appId);
        return ResultUtils.success(config);
    }

    @Operation(summary = "删除 Schema")
    @DeleteMapping("/schema/{appId}")
    public BaseResponse<Void> deleteSchema(@PathVariable Long appId) {
        supabaseService.deleteSchema(appId);
        return ResultUtils.success(null);
    }

    @Operation(summary = "获取表结构")
    @GetMapping("/schema/{appId}")
    public BaseResponse<List<TableSchemaDTO>> getSchema(@PathVariable Long appId) {
        List<TableSchemaDTO> schema = supabaseService.getSchema(appId);
        return ResultUtils.success(schema);
    }

    @Operation(summary = "获取表摘要")
    @GetMapping("/tables/{appId}")
    public BaseResponse<List<TableSummaryDTO>> getTableSummary(@PathVariable Long appId) {
        List<TableSummaryDTO> summary = supabaseService.getTableSummary(appId);
        return ResultUtils.success(summary);
    }

    @Operation(summary = "执行 SQL")
    @PostMapping("/sql/{appId}")
    public BaseResponse<String> executeSql(@PathVariable Long appId, @RequestBody String sql) {
        String result = supabaseService.executeSql(appId, sql);
        return ResultUtils.success(result);
    }

    @Operation(summary = "获取 Supabase 配置")
    @GetMapping("/config/{appId}")
    public BaseResponse<SupabaseConfigDTO> getConfig(@PathVariable Long appId) {
        SupabaseConfigDTO config = supabaseService.getSupabaseConfig(appId);
        return ResultUtils.success(config);
    }
}

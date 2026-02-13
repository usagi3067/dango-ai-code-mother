package com.dango.supabase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Supabase 配置信息 DTO（用于前端客户端配置）
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupabaseConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Supabase 项目 URL
     */
    private String url;

    /**
     * Anon Key（前端使用）
     */
    private String anonKey;

    /**
     * 应用专属 Schema 名称
     */
    private String schemaName;
}

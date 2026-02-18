package com.dango.dangoaicodeapp.infrastructure.esdao;

import com.dango.dangoaicodeapp.domain.app.entity.App;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * App ES 文档实体
 *
 * @author dango
 */
@Document(indexName = "app")
@Data
public class AppEsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String appName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String initPrompt;

    @Field(type = FieldType.Keyword)
    private String tag;

    @Field(type = FieldType.Keyword, index = false)
    private String cover;

    @Field(type = FieldType.Keyword)
    private String codeGenType;

    @Field(type = FieldType.Keyword)
    private String deployKey;

    @Field(type = FieldType.Integer)
    private Integer priority;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime deployedTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime editTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Integer)
    private Integer isDelete;

    /**
     * App -> AppEsDTO
     */
    public static AppEsDTO fromApp(App app) {
        if (app == null) {
            return null;
        }
        AppEsDTO dto = new AppEsDTO();
        BeanUtils.copyProperties(app, dto);
        return dto;
    }

    /**
     * AppEsDTO -> App
     */
    public static App toApp(AppEsDTO dto) {
        if (dto == null) {
            return null;
        }
        App app = new App();
        BeanUtils.copyProperties(dto, app);
        return app;
    }
}

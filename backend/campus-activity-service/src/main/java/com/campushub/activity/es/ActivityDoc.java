package com.campushub.activity.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ES 活动索引文档（关键词搜索用）；服务器需装 IK 分词插件，ik_max_word/ik_smart 才生效
 */
@Data
@Document(indexName = "campus_activity")
public class ActivityDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Keyword, index = false)
    private String location;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime startTime;

    /** 1报名中 2进行中 3已结束 4已取消 */
    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    /** 搜索命中的高亮片段（仅查询时回填给前端，不参与 ES 映射） */
    @Transient
    private Map<String, List<String>> highlights;
}






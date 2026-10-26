package com.campushub.community.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionFormDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最长 128 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 标签 id 列表，可为空 */
    private List<Long> tagIds;
}






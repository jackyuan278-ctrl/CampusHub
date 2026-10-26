package com.campushub.community.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnswerFormDTO {

    @NotBlank(message = "回答内容不能为空")
    private String content;
}






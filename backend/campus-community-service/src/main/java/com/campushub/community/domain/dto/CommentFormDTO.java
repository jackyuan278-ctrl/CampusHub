package com.campushub.community.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentFormDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长 500 字")
    private String content;
}






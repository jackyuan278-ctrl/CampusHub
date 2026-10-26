package com.campushub.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.community.domain.dto.CommentFormDTO;
import com.campushub.community.domain.po.Comment;
import com.campushub.community.domain.vo.CommentVO;

import java.util.List;

/**
 * 评论服务：挂在回答下的评论。
 */
public interface ICommentService extends IService<Comment> {

    /**
     * 发表评论：插入 tb_comment；answer.comment_count + 1；question.comment_count + 1
     */
    Long create(CommentFormDTO form, Long answerId, Long userId);

    /**
     * 某回答的评论列表：按时间正序，组装作者信息（批量查询防 N+1）
     */
    List<CommentVO> listByAnswerId(Long answerId);
}






package com.campushub.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.community.domain.dto.AnswerFormDTO;
import com.campushub.community.domain.po.Answer;
import com.campushub.community.domain.vo.AnswerVO;

import java.util.List;

/**
 * 回答服务。点赞与问题共用 tb_like（target_type=2），热度事件同样实时 ZINCRBY。
 */
public interface IAnswerService extends IService<Answer> {

    /**
     * 回答问题：插入 tb_answer；question.answer_count + 1；ZINCRBY hot:questions +5
     */
    Long create(AnswerFormDTO form, Long questionId, Long userId);

    /**
     * 某问题的回答列表：按点赞数倒序，组装作者信息（批量查询防 N+1）与 liked 状态
     */
    List<AnswerVO> listByQuestionId(Long questionId, Long userId);

    /**
     * 点赞回答（幂等）：tb_like 唯一键 (user_id, target_id, target_type=2) 兜底，重复点赞抛"请勿重复点赞"；
     * SADD like:user:{userId}:2；answer.like_count + 1
     */
    void like(Long id, Long userId);

    /**
     * 取消点赞：删除 tb_like + SREM；like_count - 1（不小于 0）
     */
    void unlike(Long id, Long userId);
}






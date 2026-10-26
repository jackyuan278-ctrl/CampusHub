package com.campushub.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.community.domain.dto.PageDTO;
import com.campushub.community.domain.dto.QuestionFormDTO;
import com.campushub.community.domain.dto.QuestionPageQuery;
import com.campushub.community.domain.po.Question;
import com.campushub.community.domain.vo.QuestionVO;

import java.util.List;

/**
 * 问答社区-问题服务。
 *
 * Redis 深挖区（缓存三大问题 + ZSet 热点榜）：
 * - hot:questions            ZSet：热度分 = 浏览x1 + 点赞x3 + 回答x5，浏览/点赞/回答事件实时 ZINCRBY
 * - cache:hot:questions      String：热点榜 JSON 缓存，TTL 随机 5~10 分钟（防雪崩）
 * - cache:question:{id}      String：问题详情缓存（Cache Aside）
 * - like:user:{userId}:{targetType} Set：我点过赞的目标 id（快速判断 liked）
 * - question:viewed:{id}     Set：24h 内浏览过该问题的用户（浏览去重，随 TTL 过期）
 */
public interface IQuestionService extends IService<Question> {

    /**
     * 发布问题：插入 tb_question；tagIds 非空时批量插入 tb_question_tag，并 tag.question_count + 1
     */
    Long publish(QuestionFormDTO form, Long userId);

    /**
     * 分页查询：keyword 模糊匹配标题；tagId 过滤（关联 tb_question_tag）；
     * sort=hot 按 like_count desc, view_count desc 排序；sort=new 按 create_time desc；
     * 组装作者昵称/头像、标签名（批量查询防 N+1），liked 取 like:user:{userId}:1
     */
    PageDTO<QuestionVO> queryPage(QuestionPageQuery query, Long userId);

    /**
     * 问题详情（含浏览计数）：
     * 1. 登录用户 24h 内首次浏览：SADD question:viewed:{id} 成功才计数（question:view:{id} INCR + ZINCRBY hot:questions +1）；
     *    未登录用户直接计数（简化处理）
     * 2. 详情走 Cache Aside：命中 cache:question:{id} 反序列化返回；未命中查库组装后写缓存
     * 3. 组装 tags、作者、liked
     */
    QuestionVO queryDetail(Long id, Long userId);

    /**
     * 热点榜（缓存三大问题深挖区）：
     * 1. 先查 cache:hot:questions；空结果也缓存短 TTL（穿透防护）
     * 2. 未命中加互斥锁重建（击穿防护），抢锁失败直接返回旧缓存值
     * 3. ZREVRANGE hot:questions 0 top-1，批量查库组装
     */
    List<QuestionVO> queryHot(int top);

    /**
     * 点赞问题（幂等）：tb_like 唯一键 (user_id, target_id, target_type=1) 兜底，重复点赞抛"请勿重复点赞"；
     * SADD like:user:{userId}:1；question.like_count + 1；ZINCRBY hot:questions +3
     */
    void like(Long id, Long userId);

    /**
     * 取消点赞：删除 tb_like + SREM like:user:{userId}:1；like_count - 1（不小于 0）；ZINCRBY hot:questions -3
     */
    void unlike(Long id, Long userId);
}






package com.campushub.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.constant.LikeTarget;
import com.campushub.common.constant.RedisKeys;
import com.campushub.community.domain.dto.AnswerFormDTO;
import com.campushub.community.domain.po.LikeRecord;
import com.campushub.community.domain.po.Question;
import com.campushub.community.domain.po.User;
import com.campushub.community.mapper.*;
import com.campushub.community.domain.po.Answer;
import com.campushub.community.service.IAnswerService;
import com.campushub.community.domain.vo.AnswerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实现留白：核心业务由你实现（IAnswerService 的 javadoc 即契约）。
 */
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl extends ServiceImpl<AnswerMapper, Answer> implements IAnswerService {

    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final LikeMapper likeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AnswerFormDTO form, Long questionId, Long userId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null  || question.getStatus()!=1) {
            throw new BizException(400,"问题不存在");
        }
        String content = form.getContent();
        Answer answer = new Answer();
        answer.setCreateTime(LocalDateTime.now());
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setContent(content);
        answer.setLikeCount(0);
        answer.setCommentCount(0);
        answer.setStatus(1);
        save(answer);
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class).eq(Question::getId, questionId).setIncrBy(Question::getAnswerCount,1));
        stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, questionId.toString(), 5);
        stringRedisTemplate.delete(RedisKeys.questionCache(questionId));
        return answer.getId();
    }

    @Override
    public List<AnswerVO> listByQuestionId(Long questionId, Long userId) {
        if (questionId == null ) {
            throw new BizException(400,"问题不存在");
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getStatus() != 1) {
            throw new BizException(400,"问题不存在");
        }
        List<Answer> list = lambdaQuery().eq(Answer::getQuestionId, questionId).eq(Answer::getStatus,1)
                .orderByDesc(Answer::getLikeCount)
                .orderByDesc(Answer::getCreateTime)
                .list();
        if (list.isEmpty()) {
            return List.of();
        }
        List<AnswerVO> answerVOList = new ArrayList<>(list.size());
        // 游客不查 Redis；登录用户一次 SMEMBERS 拿全，替代循环里 N 次 SISMEMBER
        Set<String> members = Set.of();
        if (userId != null) {
            Set<String> likedIds = stringRedisTemplate.opsForSet().members(RedisKeys.likeSet(userId, LikeTarget.ANSWER));
            members = likedIds == null ? Set.of() : likedIds;
        }
        List<Long> authorIds = list.stream().map(Answer::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        for (Answer answer:list) {
            Long authorId = answer.getUserId();
            User author = userMap.get(authorId);
            AnswerVO answerVO = BeanUtil.copyProperties(answer, AnswerVO.class);
            if (author != null) {
                answerVO.setAuthorAvatar(author.getAvatarUrl());
                answerVO.setAuthorNickname(author.getNickname());
            }
            if (userId == null) {
                answerVO.setLiked(null);
            }else {
                boolean liked = members.contains(answer.getId().toString());
                answerVO.setLiked(liked);
            }
            answerVOList.add(answerVO);
        }
        return answerVOList;
    }

    @Override
    public void like(Long id, Long userId) {
        Boolean exist = stringRedisTemplate.opsForSet().isMember(RedisKeys.likeSet(userId, LikeTarget.ANSWER), id.toString());
        if (Boolean.TRUE.equals(exist)) {
            throw new BizException(400,"请勿重复点赞");
        }
        Answer answer = getById(id);
        if (answer == null || answer.getStatus() != 1) {
            throw new BizException(400,"请求错误");
        }
        LikeRecord likeRecord = LikeRecord.builder()
                .targetType(LikeTarget.ANSWER)
                .createTime(LocalDateTime.now())
                .targetId(id)
                .userId(userId).build();
        try {
            likeMapper.insert(likeRecord);
        } catch (DuplicateKeyException e) {
            throw new BizException(400, "请勿重复点赞");
        }
        lambdaUpdate().setSql("like_count = like_count + 1").eq(Answer::getId, id).update();
        stringRedisTemplate.opsForSet().add(RedisKeys.likeSet(userId, LikeTarget.ANSWER), id.toString());

    }

    @Override
    public void unlike(Long id, Long userId) {
        Boolean exist = stringRedisTemplate.opsForSet().isMember(RedisKeys.likeSet(userId, LikeTarget.ANSWER), id.toString());
        if (!Boolean.TRUE.equals(exist)) {
            return;
        }

        int deleted = likeMapper.delete(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, id)
                .eq(LikeRecord::getTargetType, LikeTarget.ANSWER));
        if (deleted ==0) {
            return;}
        lambdaUpdate().setSql("like_count = like_count - 1").eq(Answer::getId, id).gt(Answer::getLikeCount, 0).update();
        stringRedisTemplate.opsForSet().remove(RedisKeys.likeSet(userId, LikeTarget.ANSWER), id.toString());
    }
}






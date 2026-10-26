package com.campushub.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.constant.LikeTarget;
import com.campushub.common.constant.RedisKeys;
import com.campushub.community.domain.dto.PageDTO;
import com.campushub.community.domain.dto.QuestionFormDTO;
import com.campushub.community.domain.dto.QuestionPageQuery;
import com.campushub.community.domain.po.*;
import com.campushub.community.mapper.LikeMapper;
import com.campushub.community.mapper.QuestionMapper;
import com.campushub.community.mapper.QuestionTagMapper;
import com.campushub.community.mapper.TagMapper;
import com.campushub.community.mapper.UserMapper;
import com.campushub.community.service.IQuestionService;
import com.campushub.community.domain.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 实现留白：核心业务由你实现（IQuestionService 的 javadoc 即契约）。
 * 依赖已注入：baseMapper（QuestionMapper）/ userMapper / questionTagMapper / tagMapper / likeMapper
 * / stringRedisTemplate（Redis）/ objectMapper（缓存 JSON 序列化）
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    private final UserMapper userMapper;
    private final QuestionTagMapper questionTagMapper;
    private final TagMapper tagMapper;
    private final LikeMapper likeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final QuestionMapper questionMapper;



    @Override
    @Transactional(rollbackFor = BizException.class)
    public Long publish(QuestionFormDTO form, Long userId) {
        Question question = BeanUtil.copyProperties(form, Question.class);
        question.setAnswerCount(0);
        question.setViewCount(0);
        question.setLikeCount(0);
        question.setCommentCount(0);
        question.setUserId(userId);
        question.setCreateTime(LocalDateTime.now());
        this.save(question);
        Long questionId = question.getId();
        if(!CollectionUtils.isEmpty(form.getTagIds())){
            List<Long> tagIds = form.getTagIds();
            List<QuestionTag> questionTags = new ArrayList<>();
            for (Long tagId : tagIds) {
                QuestionTag questionTag = new QuestionTag();
                questionTag.setQuestionId(questionId);
                questionTag.setTagId(tagId);
                questionTag.setCreateTime(LocalDateTime.now());
                questionTags.add(questionTag);
                tagMapper.update(null, Wrappers.<Tag>lambdaUpdate()
                        .setSql("question_count = question_count + 1")
                        .eq(Tag::getId, tagId));
            }
            questionTagMapper.insert(questionTags);
        }
        return questionId;
    }

    @Override
    public PageDTO<QuestionVO> queryPage(QuestionPageQuery query, Long userId) {
        // ① tagId 过滤（可选）：关联表查出本页候选的问题 id
        Long tagId = query.getTagId();
        List<Long> questionIds = null;
        if (tagId != null) {
            questionIds = tagMapper.selectByTagId(tagId).stream()
                    .map(QuestionTag::getQuestionId).toList();
            if (questionIds.isEmpty()) {
                return new PageDTO<>(0L, List.of());
            }
        }
        // ② 条件链：过滤项都带条件位，看用户传没传，不看集合是不是 null
        String keyword = query.getKeyword();
        LambdaQueryChainWrapper<Question> chain = lambdaQuery()
                .eq(Question::getStatus, 1)
                .like(StringUtils.hasText(keyword), Question::getTitle, keyword)
                .in(questionIds != null, Question::getId, questionIds);
        if ("new".equals(query.getSort())) {
            chain.orderByDesc(Question::getCreateTime);
        } else if ("hot".equals(query.getSort())) {
            chain.orderByDesc(Question::getLikeCount).orderByDesc(Question::getViewCount);
        } else {
            throw new BizException("未知排序错误");
        }
        // ③ 分页：total 由 MP 的 count 查询给出
        Page<Question> page = chain.page(query.toMpPage());
        List<Question> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageDTO<>(page.getTotal(), List.of());
        }
        // ④ 批量补作者：本页 distinct 作者 1 次 SELECT
        List<Long> authorIds = records.stream().map(Question::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        // ⑤ 批量补标签：关联表 1 次 + 标签表 1 次，防 N+1
        List<QuestionTag> relations = questionTagMapper.selectList(
                Wrappers.<QuestionTag>lambdaQuery()
                        .in(QuestionTag::getQuestionId, records.stream().map(Question::getId).toList()));
        Map<Long, String> tagNameMap = relations.isEmpty() ? Map.of()
                : tagMapper.selectByIds(relations.stream().map(QuestionTag::getTagId).distinct().toList())
                        .stream().collect(Collectors.toMap(Tag::getId, Tag::getName));
        Map<Long, List<String>> tagsByQuestion = new HashMap<>();
        for (QuestionTag relation : relations) {
            String name = tagNameMap.get(relation.getTagId());
            if (name != null) {
                tagsByQuestion.computeIfAbsent(relation.getQuestionId(), k -> new ArrayList<>()).add(name);
            }
        }
        // ⑥ liked：一次 SMEMBERS 拿全部点赞，替代 N 次 SISMEMBER；游客不查
        Set<String> likedIds = Set.of();
        if (userId != null) {
            Set<String> members = stringRedisTemplate.opsForSet().members(RedisKeys.likeSet(userId, LikeTarget.QUESTION));
            likedIds = members == null ? Set.of() : members;
        }
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (Question question : records) {
            QuestionVO vo = BeanUtil.copyProperties(question, QuestionVO.class);
            User author = userMap.get(question.getUserId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            vo.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
            if (userId != null) {
                vo.setLiked(likedIds.contains(question.getId().toString()));
            }
            voList.add(vo);
        }
        return new PageDTO<>(page.getTotal(), voList);
    }

    @Override
    public QuestionVO queryDetail(Long id, Long userId) {
        // ① 该不该计数：游客直接计；登录用户 NX 抢 24h 坑位，抢到才算首次
        boolean shouldCount = userId == null
                || BooleanUtil.isTrue(stringRedisTemplate.opsForValue()
                        .setIfAbsent(RedisKeys.questionViewed(id, userId), "1", 24, TimeUnit.HOURS));
        // ② 要计数：先更 DB（相对自增），再删缓存 —— Cache Aside 的"更新=删不写"
        if (shouldCount) {
            lambdaUpdate().setSql("view_count = view_count + 1").eq(Question::getId, id).update();
            stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, id.toString(), 1);
            stringRedisTemplate.delete(RedisKeys.questionCache(id));
        }
        // ③ 读缓存
        String cacheKey = RedisKeys.questionCache(id);
        QuestionVO vo = null;
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            try {
                vo = JSONUtil.toBean(json, QuestionVO.class);
            } catch (Exception ignored) {
                // 脏数据当未命中，回源重建
            }
        }
        // ④ 未命中：回源 DB 组装 → 带 TTL 写回
        if (vo == null) {
            Question question = getById(id);
            if (question == null || question.getStatus() != 1) {
                throw new BizException("问题不存在");
            }
            vo = BeanUtil.copyProperties(question, QuestionVO.class);
            User author = userMapper.selectById(question.getUserId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            List<QuestionTag> relations = questionTagMapper.selectList(
                    Wrappers.<QuestionTag>lambdaQuery().eq(QuestionTag::getQuestionId, id));
            vo.setTags(relations.isEmpty() ? List.of()
                    : tagMapper.selectByIds(relations.stream().map(QuestionTag::getTagId).toList())
                            .stream().map(Tag::getName).toList());
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(vo), 30, TimeUnit.MINUTES);
        }
        // ⑤ liked 每人不同，必须在缓存之外现算
        vo.setLiked(userId != null && Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString())));
        return vo;
    }

    @Override
    public List<QuestionVO> queryHot(int top) {
        String cacheKey = RedisKeys.HOT_QUESTIONS_CACHE;
        String lockKey = RedisKeys.HOT_QUESTIONS_LOCK;
        // 1. 读取缓存
        String jsonStr = stringRedisTemplate.opsForValue().get(cacheKey);
        if (jsonStr != null) {
            // 缓存命中，转List返回
            return JSONUtil.toList(jsonStr, QuestionVO.class);
        }
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (!BooleanUtil.isTrue(locked)) {
            String s = stringRedisTemplate.opsForValue().get(cacheKey);
            return s!=null?JSONUtil.toList(s,QuestionVO.class):List.of();
        }
        List<QuestionVO> resultList;
        try {
            Set<String> idStrSet = stringRedisTemplate.opsForZSet().reverseRange(RedisKeys.HOT_QUESTIONS, 0, top - 1);
            if (CollectionUtils.isEmpty(idStrSet)) {
                resultList = List.of();
                stringRedisTemplate.opsForValue().set(cacheKey,JSONUtil.toJsonStr(resultList), 30, TimeUnit.SECONDS);
                return resultList;
            }
            List<Long> ids = idStrSet.stream().map(Long::valueOf).toList();
            List<Question> dbquestions = questionMapper.selectByIds(ids);
            Map<Long, Question> questionMap = dbquestions.stream().collect(Collectors.toMap(Question::getId, q -> q));
            List<Long> authorIds = dbquestions.stream().map(Question::getUserId).distinct().toList();
            Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                    : userMapper.selectBatchIds(authorIds).stream()
                            .collect(Collectors.toMap(User::getId, u -> u));
            resultList = ids.stream()
                    .map(id -> {
                        Question question = questionMap.get(id);
                        if (question == null || question.getStatus() != 1) {
                            return null;
                        }
                        QuestionVO vo = BeanUtil.copyProperties(question, QuestionVO.class);
                        User author = userMap.get(question.getUserId());
                        if (author != null) {
                            vo.setAuthorNickname(author.getNickname());
                            vo.setAuthorAvatar(author.getAvatarUrl());
                        }
                        return vo;
                    }).filter(Objects::nonNull)
                    .toList();
            long randomTime = RandomUtil.randomLong(5, 11);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(resultList), randomTime, TimeUnit.MINUTES);

        }catch (Exception e){
            throw new BizException(e.getMessage());
        }finally {
            stringRedisTemplate.delete(lockKey);
        }
        return resultList;
    }

    @Override
    public void like(Long id, Long userId) {
        Boolean exist = stringRedisTemplate.opsForSet().isMember(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
        if (Boolean.TRUE.equals(exist)) {
            throw new BizException(400,"请勿重复短暂");
        }
        Question question = getById(id);
        if (question == null || question.getStatus() != 1) {
            throw new BizException(400,"请求错误");
        }
        LikeRecord likeRecord = LikeRecord.builder()
                .targetType(LikeTarget.QUESTION)
                .createTime(LocalDateTime.now())
                .targetId(id)
                .userId(userId).build();
        try {
            likeMapper.insert(likeRecord);
        } catch (DuplicateKeyException e) {
            throw new BizException(400, "请勿重复点赞");
        }
        lambdaUpdate().setSql("like_count = like_count + 1").eq(Question::getId, id).update();
        stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, id.toString(), 3);
        stringRedisTemplate.opsForSet().add(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
    }

    @Override
    public void unlike(Long id, Long userId) {
        Boolean exist = stringRedisTemplate.opsForSet().isMember(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
        if (!Boolean.TRUE.equals(exist)) {
            return;
        }

        int deleted = likeMapper.delete(Wrappers.<LikeRecord>lambdaQuery()
                    .eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetId, id)
                    .eq(LikeRecord::getTargetType, LikeTarget.QUESTION));
        if (deleted ==0) {
                return;}
        lambdaUpdate().setSql("like_count = like_count - 1").eq(Question::getId, id).gt(Question::getLikeCount, 0).update();
        stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, id.toString(), -3);
        stringRedisTemplate.opsForSet().remove(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
    }
}






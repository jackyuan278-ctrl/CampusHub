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
import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.BizException;
import com.campushub.common.constant.LikeTarget;
import com.campushub.common.constant.RedisKeys;
import com.campushub.community.client.UserLookup;
import com.campushub.community.domain.dto.PageDTO;
import com.campushub.community.domain.dto.QuestionFormDTO;
import com.campushub.community.domain.dto.QuestionPageQuery;
import com.campushub.community.domain.po.*;
import com.campushub.community.mapper.LikeMapper;
import com.campushub.community.mapper.QuestionMapper;
import com.campushub.community.mapper.QuestionTagMapper;
import com.campushub.community.mapper.TagMapper;
import com.campushub.community.service.IQuestionService;
import com.campushub.community.domain.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 依赖已注入：baseMapper（QuestionMapper）/ userLookup（作者信息，Feign 调用户服务）
 * / questionTagMapper / tagMapper / likeMapper / stringRedisTemplate（Redis）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    private final UserLookup userLookup;
    private final QuestionTagMapper questionTagMapper;
    private final TagMapper tagMapper;
    private final LikeMapper likeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final QuestionMapper questionMapper;

    /** 热榜单次最多取的条数，挡住 top=999999 这种把整表拉出来的请求 */
    private static final int MAX_HOT_SIZE = 50;



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
        // 新问题可能立刻进热榜（DB 兜底补齐时会用到），先让快照失效
        stringRedisTemplate.delete(RedisKeys.HOT_QUESTIONS_CACHE);
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
        // sort 不传/传空一律按最新：排序是展示偏好，不该因为少传参数就把列表打成错误
        String sort = StringUtils.hasText(query.getSort()) ? query.getSort() : "new";
        if ("new".equals(sort)) {
            chain.orderByDesc(Question::getCreateTime);
        } else if ("hot".equals(sort)) {
            chain.orderByDesc(Question::getLikeCount)
                    .orderByDesc(Question::getViewCount)
                    .orderByDesc(Question::getCreateTime);
        } else {
            throw new BizException("未知排序错误");
        }
        // ③ 分页：total 由 MP 的 count 查询给出
        Page<Question> page = chain.page(query.toMpPage());
        List<Question> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageDTO<>(page.getTotal(), List.of());
        }
        // ④ 批量补作者：本页 distinct 作者 1 次 Feign 调用（查不到时返回空 Map，昵称走兜底）
        List<Long> authorIds = records.stream().map(Question::getUserId).distinct().toList();
        Map<Long, UserBriefDTO> userMap = userLookup.findByIds(authorIds);
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
            UserBriefDTO author = userMap.get(question.getUserId());
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
            // 单个作者也走批量接口：少一个分支，多一跳网络对详情页可以接受
            UserBriefDTO author = userLookup.findByIds(Collections.singletonList(question.getUserId()))
                    .get(question.getUserId());
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
        int size = top <= 0 ? 10 : Math.min(top, MAX_HOT_SIZE);
        String cacheKey = RedisKeys.HOT_QUESTIONS_CACHE;
        String lockKey = RedisKeys.HOT_QUESTIONS_LOCK;
        // ① 缓存：空快照一律当未命中 —— 否则一次"账本缺货"写下的 [] 会把首页社区块锁死 5~11 分钟
        List<QuestionVO> cached = readHotCache(cacheKey);
        if (!cached.isEmpty()) {
            return cached;
        }
        // ② 重建：抢到锁的线程重建，抢不到的退化读库，保证任何时刻都有内容可返回
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (!BooleanUtil.isTrue(locked)) {
            List<QuestionVO> retry = readHotCache(cacheKey);
            return retry.isEmpty() ? assemble(candidatesFromDb(size)) : retry;
        }
        List<QuestionVO> resultList;
        try {
            resultList = assemble(pickHotQuestions(size));
            if (resultList.isEmpty()) {
                // 真空库：只压 30 秒防穿透，不要按 5~11 分钟缓存，避免长期空白
                stringRedisTemplate.opsForValue().set(cacheKey, "[]", 30, TimeUnit.SECONDS);
            } else {
                long randomTime = RandomUtil.randomLong(5, 11);
                stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(resultList), randomTime, TimeUnit.MINUTES);
            }
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return resultList;
    }

    /** 热榜候选：先按热度账本 ZSet 取，有效条目不足时按 DB 热度补齐并写回账本（热榜自愈） */
    private List<Question> pickHotQuestions(int size) {
        List<Long> ledgerIds = new ArrayList<>(size);
        Set<String> idStrSet = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisKeys.HOT_QUESTIONS, 0, size * 3L - 1);
        if (idStrSet != null) {
            for (String idStr : idStrSet) {
                try {
                    ledgerIds.add(Long.valueOf(idStr));
                } catch (NumberFormatException ignored) {
                    // 脏 member 直接跳过，不让它拖垮整个热榜
                }
            }
        }
        List<Question> ordered = new ArrayList<>(size);
        if (!ledgerIds.isEmpty()) {
            Map<Long, Question> hitMap = questionMapper.selectByIds(ledgerIds).stream()
                    .filter(q -> q.getStatus() != null && q.getStatus() == 1)
                    .collect(Collectors.toMap(Question::getId, q -> q));
            // 账本里指向已删/已下架问题的 member 顺手清掉，否则每次重建都要白查一遍、还会挤占 top 名额
            Object[] deadMembers = ledgerIds.stream()
                    .filter(id -> !hitMap.containsKey(id))
                    .map(String::valueOf)
                    .toArray();
            if (deadMembers.length > 0) {
                stringRedisTemplate.opsForZSet().remove(RedisKeys.HOT_QUESTIONS, deadMembers);
            }
            for (Long id : ledgerIds) {
                if (ordered.size() >= size) {
                    break;
                }
                Question hit = hitMap.get(id);
                if (hit != null) {
                    ordered.add(hit);
                }
            }
        }
        // 账本缺货：问题被删/下架、或题库是直接 INSERT 进来的（没走过发布接口，自然没进 ZSet）
        if (ordered.size() < size) {
            Set<Long> picked = ordered.stream().map(Question::getId).collect(Collectors.toSet());
            List<Question> candidate = new ArrayList<>(ordered);
            for (Question question : candidatesFromDb(size)) {
                if (candidate.size() >= size) {
                    break;
                }
                if (picked.contains(question.getId())) {
                    continue;
                }
                candidate.add(question);
                // 写回账本，下次直接命中 ZSet，不用再回表补齐
                if (stringRedisTemplate.opsForZSet().score(RedisKeys.HOT_QUESTIONS, question.getId().toString()) == null) {
                    stringRedisTemplate.opsForZSet()
                            .add(RedisKeys.HOT_QUESTIONS, question.getId().toString(), dbHeat(question));
                }
            }
            return candidate;
        }
        return ordered;
    }

    /** 按 DB 真实热度取候选（浏览*1 + 点赞*3，与 ZSet 记账口径一致） */
    private List<Question> candidatesFromDb(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return questionMapper.selectList(Wrappers.<Question>lambdaQuery()
                .eq(Question::getStatus, 1)
                .orderByDesc(Question::getLikeCount)
                .orderByDesc(Question::getViewCount)
                .orderByDesc(Question::getCreateTime)
                .last("LIMIT " + limit));
    }

    /** 组装展示层：作者 / 标签各一次批量查询，避免 N+1 */
    private List<QuestionVO> assemble(List<Question> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return List.of();
        }
        List<Long> authorIds = questions.stream().map(Question::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserBriefDTO> userMap = userLookup.findByIds(authorIds);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<QuestionTag> relations = questionTagMapper.selectList(
                Wrappers.<QuestionTag>lambdaQuery().in(QuestionTag::getQuestionId, questionIds));
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
        List<QuestionVO> list = new ArrayList<>(questions.size());
        for (Question question : questions) {
            QuestionVO vo = BeanUtil.copyProperties(question, QuestionVO.class);
            UserBriefDTO author = userMap.get(question.getUserId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            vo.setTags(tagsByQuestion.getOrDefault(question.getId(), List.of()));
            vo.setLiked(false);
            list.add(vo);
        }
        return list;
    }

    /** 读热榜快照：JSON 脏了就当未命中，绝不让缓存把接口打成 500 */
    private List<QuestionVO> readHotCache(String cacheKey) {
        String jsonStr = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(jsonStr)) {
            return List.of();
        }
        try {
            List<QuestionVO> list = JSONUtil.toList(jsonStr, QuestionVO.class);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            stringRedisTemplate.delete(cacheKey);
            return List.of();
        }
    }

    /** 热度分：浏览 *1 + 点赞 *3（回答由 AnswerServiceImpl 额外 +5） */
    private double dbHeat(Question question) {
        int view = question.getViewCount() == null ? 0 : question.getViewCount();
        int like = question.getLikeCount() == null ? 0 : question.getLikeCount();
        return view + like * 3.0;
    }

    /**
     * 点赞的唯一判据是 tb_like_record 的唯一键，不是 Redis 里的 likeSet。
     * 事务只保证「点赞记录」与「like_count」同生同死；Redis 全程只当加速用，失败只记 warn。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long id, Long userId) {
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
        try {
            stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, id.toString(), 3);
            stringRedisTemplate.opsForSet().add(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
            // 热度变了，热榜快照必须失效，否则首页要等 5~11 分钟才看到排名变化
            stringRedisTemplate.delete(RedisKeys.HOT_QUESTIONS_CACHE);
        } catch (Exception e) {
            log.warn("问题点赞的缓存更新失败，点赞本身已生效，questionId={} userId={}", id, userId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long id, Long userId) {
        int deleted = likeMapper.delete(Wrappers.<LikeRecord>lambdaQuery()
                    .eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetId, id)
                    .eq(LikeRecord::getTargetType, LikeTarget.QUESTION));
        if (deleted == 0) {
                return;}
        lambdaUpdate().setSql("like_count = like_count - 1").eq(Question::getId, id).gt(Question::getLikeCount, 0).update();
        try {
            stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_QUESTIONS, id.toString(), -3);
            stringRedisTemplate.opsForSet().remove(RedisKeys.likeSet(userId, LikeTarget.QUESTION), id.toString());
            stringRedisTemplate.delete(RedisKeys.HOT_QUESTIONS_CACHE);
        } catch (Exception e) {
            log.warn("取消问题点赞的缓存更新失败，取消本身已生效，questionId={} userId={}", id, userId, e);
        }
    }
}






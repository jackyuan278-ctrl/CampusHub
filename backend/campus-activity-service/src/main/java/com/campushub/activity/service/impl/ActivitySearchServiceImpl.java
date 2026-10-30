package com.campushub.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.activity.domain.dto.PageDTO;
import com.campushub.activity.domain.po.Activity;
import com.campushub.activity.es.ActivityDoc;
import com.campushub.activity.mapper.ActivityMapper;
import com.campushub.activity.service.IActivitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动关键词搜索。ES 是加速层，不是唯一数据源：
 * 检索异常（未部署 / 索引丢失 / 连接超时）时降级为 MySQL 模糊查询，
 * 保证搜索功能始终可用，ES 可以随时下线而不影响业务。
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ActivitySearchServiceImpl implements IActivitySearchService {

    /** 兜底单页上限：like '%xx%' 走不了索引，限制单页大小避免一次扫太多行 */
    private static final int MAX_DB_PAGE_SIZE = 50;

    /** 已取消的活动不参与搜索，与 ES 侧的 mustNot(status=4) 保持一致 */
    private static final int STATUS_CANCELLED = 4;

    private final ObjectProvider<ElasticsearchOperations> operationsProvider;
    private final ActivityMapper activityMapper;

    @Override
    public PageDTO<ActivityDoc> search(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.isBlank()) {
            return new PageDTO<>(0L, List.of());
        }
        ElasticsearchOperations operations = operationsProvider.getIfAvailable();
        if (operations == null) {
            log.debug("ES 未启用，直接走 MySQL 检索（keyword={}）", keyword);
            return searchFromDb(keyword, page, pageSize);
        }
        try {
            return searchFromEs(operations, keyword, page, pageSize);
        } catch (Exception e) {
            // ES 未启动、索引不存在、连接超时、查询语法错误 —— 一律降级，不让搜索变成 500
            log.warn("ES 检索不可用，降级为 MySQL 模糊查询（keyword={}）：{}", keyword, e.getMessage());
            return searchFromDb(keyword, page, pageSize);
        }
    }

    /** ES 检索：标题权重 2，正文权重 1，带高亮 */
    private PageDTO<ActivityDoc> searchFromEs(ElasticsearchOperations operations, String keyword, int page, int pageSize) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm.query(keyword).fields("title^2", "description")))
                        .mustNot(m -> m.term(t -> t.field("status").value(STATUS_CANCELLED)))))
                .withPageable(PageRequest.of(page - 1, pageSize))
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(List.of(new HighlightField("title"), new HighlightField("description"))),
                        ActivityDoc.class))
                .build();
        SearchHits<ActivityDoc> hits = operations.search(query, ActivityDoc.class);
        List<ActivityDoc> list = new ArrayList<>();
        for (SearchHit<ActivityDoc> hit : hits) {
            ActivityDoc doc = hit.getContent();
            doc.setHighlights(hit.getHighlightFields());
            list.add(doc);
        }
        return new PageDTO<>(hits.getTotalHits(), list);
    }

    /** MySQL 兜底：标题或正文模糊匹配，按发布时间倒序，不返回高亮 */
    private PageDTO<ActivityDoc> searchFromDb(String keyword, int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_DB_PAGE_SIZE);
        int current = Math.max(page, 1);
        LambdaQueryWrapper<Activity> lq = new LambdaQueryWrapper<>();
        lq.ne(Activity::getStatus, STATUS_CANCELLED)
                .and(w -> w.like(Activity::getTitle, keyword)
                        .or().like(Activity::getDescription, keyword))
                .orderByDesc(Activity::getCreateTime);
        IPage<Activity> result = activityMapper.selectPage(new Page<>(current, size), lq);
        List<ActivityDoc> list = result.getRecords().stream()
                .map(ActivitySearchServiceImpl::toDoc)
                .collect(Collectors.toList());
        return new PageDTO<>(result.getTotal(), list);
    }

    private static ActivityDoc toDoc(Activity a) {
        ActivityDoc doc = new ActivityDoc();
        doc.setId(a.getId());
        doc.setTitle(a.getTitle());
        doc.setDescription(a.getDescription());
        doc.setLocation(a.getLocation());
        doc.setStartTime(a.getStartTime());
        doc.setStatus(a.getStatus());
        doc.setCoverUrl(a.getCoverUrl());
        return doc;
    }
}

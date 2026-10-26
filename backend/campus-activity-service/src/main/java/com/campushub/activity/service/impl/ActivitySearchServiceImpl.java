package com.campushub.activity.service.impl;

import com.campushub.activity.domain.dto.PageDTO;
import com.campushub.activity.es.ActivityDoc;
import com.campushub.activity.service.IActivitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.NoSuchIndexException;
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

@RequiredArgsConstructor
@Service
@Slf4j
public class ActivitySearchServiceImpl implements IActivitySearchService {
    private final ElasticsearchOperations operations;
    @Override
    public PageDTO<ActivityDoc> search(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.isBlank()) {
            return new PageDTO<>(0L, List.of());
        }
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm.query(keyword).fields("title^2", "description")))
                        .mustNot(m -> m.term(t -> t.field("status").value(4)))))
                .withPageable(PageRequest.of(page - 1, pageSize))
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(List.of(new HighlightField("title"), new HighlightField("description"))),
                        ActivityDoc.class))
                .build();
        SearchHits<ActivityDoc> hits;
        try {
            hits = operations.search(query, ActivityDoc.class);
        } catch (NoSuchIndexException e) {
            // 索引尚未创建（还没发布过任何活动）：返回空页而不是 500
            return new PageDTO<>(0L, List.of());
        }
        List<ActivityDoc> list = new ArrayList<>();
        for (SearchHit<ActivityDoc> hit : hits) {
            ActivityDoc doc = hit.getContent();
            doc.setHighlights(hit.getHighlightFields());
            list.add(doc);
        }
        return new PageDTO<>(hits.getTotalHits(), list);
    }
}






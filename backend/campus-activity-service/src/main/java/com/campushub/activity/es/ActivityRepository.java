package com.campushub.activity.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ActivityRepository extends ElasticsearchRepository<ActivityDoc, Long> {
}






package com.dango.dangoaicodeapp.infrastructure.esdao;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * App ES 数据访问层
 *
 * @author dango
 */
public interface AppEsDao extends ElasticsearchRepository<AppEsDTO, Long> {
}

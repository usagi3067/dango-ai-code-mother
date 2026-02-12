package com.dango.dangoaicodeapp.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.dango.dangoaicodeapp.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.model.es.AppEsDTO;
import com.dango.dangoaicodeapp.service.AppSearchService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ES 搜索服务实现
 *
 * @author dango
 */
@Service("esSearchService")
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class AppEsSearchServiceImpl implements AppSearchService {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<App> searchApps(AppQueryRequest request) {
        String tag = request.getTag();
        String searchText = request.getSearchText();
        Long lastId = request.getLastId();
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 12;

        // 构造查询条件
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 过滤已删除
        boolQuery.filter(TermQuery.of(t -> t.field("isDelete").value(0))._toQuery());

        // 标签筛选
        if (StringUtils.isNotBlank(tag)) {
            boolQuery.filter(TermQuery.of(t -> t.field("tag").value(tag))._toQuery());
        }

        // 游标分页：id < lastId
        if (lastId != null) {
            boolQuery.filter(RangeQuery.of(r -> r.number(n -> n.field("id").lt((double) lastId)))._toQuery());
        }

        // 关键词搜索
        if (StringUtils.isNotBlank(searchText)) {
            BoolQuery.Builder searchQuery = new BoolQuery.Builder();

            // appName 精确匹配（权重最高）
            searchQuery.should(TermQuery.of(t -> t
                    .field("appName.keyword")
                    .value(searchText)
                    .boost(3.0f)
            )._toQuery());

            // appName 分词匹配
            searchQuery.should(MatchQuery.of(m -> m
                    .field("appName")
                    .query(searchText)
                    .boost(2.0f)
            )._toQuery());

            // initPrompt 分词匹配
            searchQuery.should(MatchQuery.of(m -> m
                    .field("initPrompt")
                    .query(searchText)
                    .boost(1.0f)
            )._toQuery());

            searchQuery.minimumShouldMatch("1");
            boolQuery.must(searchQuery.build()._toQuery());
        }

        // 构造查询
        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withSort(Sort.by(Sort.Direction.DESC, "id"))
                .withPageable(PageRequest.of(0, pageSize))
                .build();

        SearchHits<AppEsDTO> hits = elasticsearchOperations.search(query, AppEsDTO.class);

        // 转换结果
        List<App> apps = hits.getSearchHits().stream()
                .map(hit -> AppEsDTO.toApp(hit.getContent()))
                .collect(Collectors.toList());

        Page<App> page = new Page<>();
        page.setRecords(apps);
        page.setPageSize(pageSize);
        return page;
    }
}

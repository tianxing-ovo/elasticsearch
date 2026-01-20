package com.ltx.service.impl;

import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.entity.PageResult;
import com.ltx.entity.SearchRequestBody;
import com.ltx.mapper.HotelMapper;
import com.ltx.service.HotelService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author tianxing
 */
@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * 搜索酒店
     *
     * @param searchRequestBody 搜索请求体
     * @return 分页结果
     */
    @Override
    public PageResult search(SearchRequestBody searchRequestBody) {
        String location = searchRequestBody.getLocation();
        // 布尔查询
        Query boolQuery = buildBoolQuery(searchRequestBody);
        String sortBy = searchRequestBody.getSortBy();
        if ("default".equals(sortBy)) {
            sortBy = "price";
        }
        // 投放了广告的酒店排前面 - 使用 function_score 查询
        Query filterQuery = Query.of(q -> q.term(t -> t.field("isAd").value(true)));
        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs.query(boolQuery).functions(f -> f.filter(filterQuery).weight(1000.0)).boostMode(FunctionBoostMode.Multiply));
        Query finalQuery = Query.of(q -> q.functionScore(functionScoreQuery));
        // 构建查询
        final String finalSortBy = sortBy;
        NativeQueryBuilder queryBuilder = NativeQuery.builder().withQuery(finalQuery).withPageable(PageRequest.of(searchRequestBody.getPage() - 1, searchRequestBody.getSize()));
        // 添加普通排序
        if (StringUtils.isNotBlank(finalSortBy)) {
            queryBuilder.withSort(s -> s.field(f -> f.field(finalSortBy).order(SortOrder.Asc)));
        }
        // 按距离排序
        if (StringUtils.isNotBlank(location)) {
            String[] latLon = location.split(",");
            if (latLon.length == 2) {
                double lat = Double.parseDouble(latLon[0].trim());
                double lon = Double.parseDouble(latLon[1].trim());
                queryBuilder.withSort(s -> s.geoDistance(g -> g.field("location").location(l -> l.latlon(ll -> ll.lat(lat).lon(lon))).order(SortOrder.Asc).unit(DistanceUnit.Kilometers).distanceType(GeoDistanceType.Arc)));
            }
        }
        SearchHits<HotelDoc> searchHits = elasticsearchOperations.search(queryBuilder.build(), HotelDoc.class);
        // 查询距离
        List<HotelDoc> list = searchHits.getSearchHits().stream().map(searchHit -> {
            HotelDoc hotelDoc = searchHit.getContent();
            List<Object> sortValueList = searchHit.getSortValues();
            if (!sortValueList.isEmpty()) {
                hotelDoc.setDistance(sortValueList.get(0));
            }
            return hotelDoc;
        }).collect(Collectors.toList());
        return new PageResult(searchHits.getTotalHits(), list);
    }

    /**
     * 查询城市/星级/品牌可选值
     *
     * @param searchRequestBody 搜索请求体
     * @return 城市/星级/品牌可选值
     */
    @Override
    public Map<String, List<String>> filters(SearchRequestBody searchRequestBody) {
        Map<String, List<String>> map = new HashMap<>();
        // 布尔查询
        Query boolQuery = buildBoolQuery(searchRequestBody);
        // 构建聚合查询
        NativeQuery query = NativeQuery.builder().withQuery(boolQuery).withPageable(PageRequest.of(0, 1)).withAggregation("cityAgg", Aggregation.of(a -> a.terms(t -> t.field("city").size(100)))).withAggregation("starAgg", Aggregation.of(a -> a.terms(t -> t.field("starName").size(100)))).withAggregation("brandAgg", Aggregation.of(a -> a.terms(t -> t.field("brand").size(100)))).build();
        SearchHits<HotelDoc> searchHits = elasticsearchOperations.search(query, HotelDoc.class);
        // 解析聚合结果
        if (searchHits.hasAggregations()) {
            ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
            if (aggregations != null) {
                map.put("city", extractTerms(aggregations, "cityAgg"));
                map.put("starName", extractTerms(aggregations, "starAgg"));
                map.put("brand", extractTerms(aggregations, "brandAgg"));
            }
        }
        return map;
    }

    /**
     * 从聚合结果中提取 terms
     *
     * @param aggregations 聚合结果
     * @param aggName      聚合名称
     * @return terms 列表
     */
    private List<String> extractTerms(ElasticsearchAggregations aggregations, String aggName) {
        Aggregate aggregate = Objects.requireNonNull(aggregations.get(aggName)).aggregation().getAggregate();
        StringTermsAggregate termsAgg = aggregate.sterms();
        return termsAgg.buckets().array().stream().map(StringTermsBucket::key).map(FieldValue::stringValue).collect(Collectors.toList());
    }

    /**
     * 构建布尔查询
     *
     * @param searchRequestBody 搜索请求体
     * @return 查询对象
     */
    private Query buildBoolQuery(SearchRequestBody searchRequestBody) {
        String key = searchRequestBody.getKey();
        String city = searchRequestBody.getCity();
        String brand = searchRequestBody.getBrand();
        String starName = searchRequestBody.getStarName();
        Integer minPrice = searchRequestBody.getMinPrice();
        Integer maxPrice = searchRequestBody.getMaxPrice();
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        // 关键字搜索
        if (StringUtils.isBlank(key)) {
            boolBuilder.must(Query.of(q -> q.matchAll(m -> m)));
        } else {
            boolBuilder.must(Query.of(q -> q.match(m -> m.field("all").query(key))));
        }
        // 按城市过滤
        if (StringUtils.isNotBlank(city)) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("city").value(city))));
        }
        // 按品牌过滤
        if (StringUtils.isNotBlank(brand)) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("brand").value(brand))));
        }
        // 按星级过滤
        if (StringUtils.isNotBlank(starName)) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("starName").value(starName))));
        }
        // 按价格过滤
        if (minPrice != null && maxPrice != null) {
            boolBuilder.filter(Query.of(q -> q.range(r -> r.number(n -> n.field("price").gte((double) minPrice).lte((double) maxPrice)))));
        }
        return Query.of(q -> q.bool(boolBuilder.build()));
    }
}
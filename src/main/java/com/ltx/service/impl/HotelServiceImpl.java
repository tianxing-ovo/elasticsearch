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
import com.ltx.constant.Constant;
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
        String sortBy = searchRequestBody.getSortBy();
        String sortOrder = searchRequestBody.getSortOrder();
        Integer pageNumber = searchRequestBody.getPageNumber();
        Integer pageSize = searchRequestBody.getPageSize();
        SortOrder order = "asc".equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
        // 布尔查询
        Query boolQuery = buildBoolQuery(searchRequestBody);
        // 函数评分查询: 对 isAd = true 的酒店加权
        Query filterQuery = Query.of(q -> q.term(t -> t.field("isAd").value(true)));
        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs.query(boolQuery)
                .functions(f -> f.filter(filterQuery)
                        .weight(1000.0))
                .boostMode(FunctionBoostMode.Multiply));
        Query finalQuery = Query.of(q -> q.functionScore(functionScoreQuery));
        // 分页查询
        NativeQueryBuilder queryBuilder = NativeQuery.builder().withQuery(finalQuery)
                .withPageable(PageRequest.of(pageNumber - 1, pageSize));
        // 按分数排序(广告置顶)
        queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        // 按评价排序
        if ("score".equals(sortBy)) {
            queryBuilder.withSort(s -> s.field(f -> f.field("score").order(order)));
        }
        // 按价格排序
        if ("price".equals(sortBy)) {
            queryBuilder.withSort(s -> s.field(f -> f.field("price").order(order)));
        }
        // 解析用户位置
        double userLat = 0, userLon = 0;
        boolean hasLocation = false;
        if (StringUtils.isNotBlank(location)) {
            String[] latLon = location.split(",");
            if (latLon.length == 2) {
                userLat = Double.parseDouble(latLon[0].trim());
                userLon = Double.parseDouble(latLon[1].trim());
                hasLocation = true;
            }
        }
        // 按距离排序(仅当用户选择距离排序时)
        if (hasLocation && "distance".equals(sortBy)) {
            double lat = userLat, lon = userLon;
            queryBuilder.withSort(s -> s.geoDistance(g -> g.field("location")
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                    .order(order).unit(DistanceUnit.Kilometers)
                    .distanceType(GeoDistanceType.Arc)));
        }
        // 执行Elasticsearch搜索查询
        SearchHits<HotelDoc> searchHits = elasticsearchOperations.search(queryBuilder.build(), HotelDoc.class);
        // 使用Haversine公式计算距离
        final double finalUserLat = userLat;
        final double finalUserLon = userLon;
        final boolean finalHasLocation = hasLocation;
        // 给HotelDoc对象设置距离属性
        List<HotelDoc> hotelDocList = searchHits.getSearchHits().stream().map(searchHit -> {
            HotelDoc hotelDoc = searchHit.getContent();
            if (finalHasLocation && StringUtils.isNotBlank(hotelDoc.getLocation())) {
                String[] hotelLatLon = hotelDoc.getLocation().split(",");
                if (hotelLatLon.length == 2) {
                    double hotelLat = Double.parseDouble(hotelLatLon[0].trim());
                    double hotelLon = Double.parseDouble(hotelLatLon[1].trim());
                    // 计算用户到酒店的距离
                    double distance = calculateDistance(finalUserLat, finalUserLon, hotelLat, hotelLon);
                    hotelDoc.setDistance(distance);
                }
            }
            return hotelDoc;
        }).toList();
        return new PageResult(searchHits.getTotalHits(), hotelDocList);
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
        NativeQuery query = NativeQuery.builder().withQuery(boolQuery).withPageable(PageRequest.of(0, 1))
                .withAggregation("cityAgg", Aggregation.of(a -> a.terms(t -> t.field("city").size(100))))
                .withAggregation("starAgg", Aggregation.of(a -> a.terms(t -> t.field("starName").size(100))))
                .withAggregation("brandAgg", Aggregation.of(a -> a.terms(t -> t.field("brand").size(100)))).build();
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
        return termsAgg.buckets().array().stream().map(StringTermsBucket::key).map(FieldValue::stringValue).toList();
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
            boolBuilder.filter(Query.of(
                    q -> q.range(r -> r.number(n -> n.field("price").gte((double) minPrice).lte((double) maxPrice)))));
        }
        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 使用Haversine公式计算两点之间的距离(单位:公里)
     *
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离(公里)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Constant.R * c;
    }
}
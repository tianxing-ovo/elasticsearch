package com.ltx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.entity.PageResult;
import com.ltx.entity.SearchRequestBody;
import com.ltx.mapper.HotelMapper;
import com.ltx.service.HotelService;
import com.ltx.util.DocumentUtil;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Resource
    private DocumentUtil documentUtil;

    /**
     * 搜索
     */
    @Override
    public PageResult search(SearchRequestBody searchRequestBody) {
        String location = searchRequestBody.getLocation();
        // 布尔查询
        BoolQueryBuilder boolQueryBuilder = buildBoolQuery(searchRequestBody);
        String sortBy = searchRequestBody.getSortBy();
        if ("default".equals(sortBy)) {
            sortBy = "price";
        }
        // 投放了广告的酒店排前面
        QueryBuilder filter = QueryBuilders.termQuery("isAD", true);
        NativeSearchQueryBuilder nativeSearchQueryBuilder = documentUtil.buildQuery(QueryBuilders.functionScoreQuery(boolQueryBuilder, new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(filter, ScoreFunctionBuilders.weightFactorFunction(1000))}).boostMode(CombineFunction.MULTIPLY), searchRequestBody.getPage() - 1, searchRequestBody.getSize(), sortBy, null);
        // 按距离排序
        if (StringUtils.isNotBlank(location)) {
            nativeSearchQueryBuilder.withSorts(SortBuilders
                    .geoDistanceSort("location", new GeoPoint(location))
                    .order(SortOrder.ASC)
                    .unit(DistanceUnit.KILOMETERS));
        }
        SearchHits<HotelDoc> searchHits = documentUtil.query(nativeSearchQueryBuilder.build());
        // 查询距离
        List<HotelDoc> list = searchHits.getSearchHits().stream().map(searchHit -> {
            HotelDoc hotelDoc = searchHit.getContent();
            List<Object> sortValueList = searchHit.getSortValues();
            if (sortValueList.size() > 0) {
                hotelDoc.setDistance(sortValueList.get(0));
            }
            return hotelDoc;
        }).collect(Collectors.toList());
        return new PageResult(searchHits.getTotalHits(), list);
    }

    /**
     * 查询城市/星级/品牌可选值
     */
    @Override
    public Map<String, List<String>> filters(SearchRequestBody searchRequestBody) {
        Map<String, List<String>> map = new HashMap<>();
        // 布尔查询
        BoolQueryBuilder boolQueryBuilder = buildBoolQuery(searchRequestBody);
        TermsAggregationBuilder cityAgg = AggregationBuilders.terms("cityAgg").field("city");
        TermsAggregationBuilder starNameAgg = AggregationBuilders.terms("starAgg").field("starName");
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brand");
        SearchHits<HotelDoc> searchHits = documentUtil.query(new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withAggregations(cityAgg, starNameAgg, brandAgg).build());
        AggregationsContainer<?> aggregations = searchHits.getAggregations();
        if (aggregations != null) {
            Aggregations aggregation = (Aggregations) aggregations.aggregations();
            ParsedStringTerms cityStringTerms = aggregation.get("cityAgg");
            ParsedStringTerms starNameStringTerms = aggregation.get("starAgg");
            ParsedStringTerms brandStringTerms = aggregation.get("brandAgg");
            map.put("city", cityStringTerms.getBuckets().stream().map(bucket -> bucket.getKey().toString()).collect(Collectors.toList()));
            map.put("starName", starNameStringTerms.getBuckets().stream().map(bucket -> bucket.getKey().toString()).collect(Collectors.toList()));
            map.put("brand", brandStringTerms.getBuckets().stream().map(bucket -> bucket.getKey().toString()).collect(Collectors.toList()));
        }
        return map;
    }


    /**
     * 构建布尔查询
     */
    private BoolQueryBuilder buildBoolQuery(SearchRequestBody searchRequestBody) {
        String key = searchRequestBody.getKey();
        String city = searchRequestBody.getCity();
        String brand = searchRequestBody.getBrand();
        String starName = searchRequestBody.getStarName();
        Integer minPrice = searchRequestBody.getMinPrice();
        Integer maxPrice = searchRequestBody.getMaxPrice();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 关键字搜索
        if (StringUtils.isBlank(key)) {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        } else {
            boolQueryBuilder.must(QueryBuilders.matchQuery("all", key));
        }
        // 按城市过滤
        if (StringUtils.isNotBlank(city)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("city", city));
        }
        // 按品牌过滤
        if (StringUtils.isNotBlank(brand)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("brand", brand));
        }
        // 按星级过滤
        if (StringUtils.isNotBlank(starName)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("starName", starName));
        }
        // 按价格过滤
        if (minPrice != null && maxPrice != null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }
        return boolQueryBuilder;
    }
}

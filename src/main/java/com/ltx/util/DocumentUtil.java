package com.ltx.util;

import com.ltx.constant.HotelConstant;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.repository.HotelDocRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import static org.springframework.data.domain.Sort.Order;


/**
 * 文档工具类
 */
@Component
public class DocumentUtil {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private HotelDocRepository hotelDocRepository;

    /**
     * 新增所有文档
     */
    public void insertAllDocument(List<Hotel> hotelList) {
        List<HotelDoc> hotelDocList = hotelList.stream().map(HotelDoc::new).collect(Collectors.toList());
        hotelDocRepository.saveAll(hotelDocList);
    }

    /**
     * 新增指定id的文档
     */
    public void insertDocument(Hotel hotel) {
        HotelDoc hotelDoc = new HotelDoc(hotel);
        hotelDocRepository.save(hotelDoc);
    }


    /**
     * 构建查询条件: 会影响文档的相关度分数
     *
     * @param queryBuilder 查询条件
     * @param page         页号
     * @param size         每页的文档数量
     * @param field        排序字段
     * @param name         高亮字段
     */
    public NativeSearchQueryBuilder buildQuery(QueryBuilder queryBuilder, int page, int size, String field, String name) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder().withQuery(queryBuilder);
        nativeSearchQueryBuilder.withPageable(PageRequest.of(page, size));
        if (StringUtils.isNotEmpty(field)) {
            nativeSearchQueryBuilder.withSort(Sort.by(Order.asc(field)));
        }
        if (StringUtils.isNotEmpty(name)) {
            nativeSearchQueryBuilder.withHighlightBuilder(new HighlightBuilder().field(name).requireFieldMatch(false));
        }
        return nativeSearchQueryBuilder;
    }

    /**
     * 构建查询条件: 会影响文档的相关度分数
     */
    public NativeSearchQuery buildQuery(QueryBuilder queryBuilder) {
        return buildQuery(queryBuilder, 0, 10, null, null).build();
    }

    /**
     * 构建过滤条件: 不会影响文档的相关度分数
     */
    public NativeSearchQuery buildFilter(QueryBuilder filterBuilder) {
        return new NativeSearchQueryBuilder().withFilter(filterBuilder).withPageable(PageRequest.of(0, 10)).build();
    }

    /**
     * 查询文档
     */
    public SearchHits<HotelDoc> query(NativeSearchQuery nativeSearchQuery) {
        return elasticsearchOperations.search(nativeSearchQuery, HotelDoc.class);
    }

    /**
     * matchAll查询
     */
    public List<HotelDoc> matchAllQuery() {
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.matchAllQuery()));
        return map(searchHits);
    }

    /**
     * match查询
     */
    public List<HotelDoc> matchQuery(String name, Object text) {
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.matchQuery(name, text)));
        return map(searchHits);
    }

    /**
     * multiMatch查询
     */
    public List<HotelDoc> multiMatchQuery(Object text, String... fieldNames) {
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.multiMatchQuery(text, fieldNames)));
        return map(searchHits);
    }

    /**
     * term查询
     */
    public List<HotelDoc> termQuery(String name, Object value) {
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.termQuery(name, value)));
        return map(searchHits);
    }

    /**
     * range查询
     */
    public List<HotelDoc> rangeQuery(String name, Object from, Object to) {
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.rangeQuery(name).gte(from).lte(to)));
        return map(searchHits);
    }

    /**
     * bool查询
     */
    public List<HotelDoc> boolQuery(QueryBuilder must, QueryBuilder should, QueryBuilder mustNot, QueryBuilder filter) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (Objects.nonNull(must)) {
            boolQueryBuilder.must(must);
        }
        if (Objects.nonNull(should)) {
            boolQueryBuilder.should(should);
        }
        if (Objects.nonNull(mustNot)) {
            boolQueryBuilder.mustNot(mustNot);
        }
        if (Objects.nonNull(filter)) {
            boolQueryBuilder.filter(filter);
        }
        SearchHits<HotelDoc> searchHits = query(buildQuery(boolQueryBuilder));
        return map(searchHits);
    }

    /**
     * functionScore查询
     */
    public List<HotelDoc> functionScoreQuery() {
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("all", "外滩");
        QueryBuilder filter = QueryBuilders.termQuery("brand", "如家");
        WeightBuilder weightBuilder = ScoreFunctionBuilders.weightFactorFunction(10);
        FilterFunctionBuilder[] filterFunctionBuilders = {new FilterFunctionBuilder(filter, weightBuilder)};
        SearchHits<HotelDoc> searchHits = query(buildQuery(QueryBuilders.functionScoreQuery(matchQueryBuilder, filterFunctionBuilders).boostMode(CombineFunction.SUM)));
        return map(searchHits);
    }

    /**
     * geoDistance查询
     */
    public List<HotelDoc> geoDistanceQuery(String name, double lat, double lon, String distance) {
        SearchHits<HotelDoc> searchHits = query(buildFilter(QueryBuilders.geoDistanceQuery(name).point(lat, lon).distance(distance)));
        return map(searchHits);
    }

    /**
     * 根据id查询文档
     */
    public HotelDoc findById(Long id) {
        Optional<HotelDoc> optional = hotelDocRepository.findById(id);
        return optional.orElseGet(HotelDoc::new);
    }

    /**
     * 删除文档
     *
     * @param id 文档id
     */
    public void delete(Long id) {
        hotelDocRepository.deleteById(id);
        System.out.println("删除成功");
    }

    /**
     * 更新指定id的文档
     *
     * @param id       文档id
     * @param fieldMap key-字段名 value-字段值
     */
    public void update(Long id, Map<String, Object> fieldMap) {
        // 获取索引坐标
        IndexCoordinates indexCoordinates = IndexCoordinates.of(HotelConstant.INDEX_NAME);
        Document document = Document.from(fieldMap);
        UpdateQuery updateQuery = UpdateQuery.builder(id.toString()).withDocument(document).build();
        UpdateResponse updateResponse = elasticsearchOperations.update(updateQuery, indexCoordinates);
        System.out.println(updateResponse.getResult());
    }

    /**
     * 获取内容
     */
    public List<HotelDoc> map(SearchHits<HotelDoc> searchHits) {
        return searchHits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

}

package com.ltx.util;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ltx.constant.HotelConstant;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.repository.HotelDocRepository;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 文档工具类
 *
 * @author tianxing
 */
@SuppressWarnings("unused")
@Component
public class DocumentUtil {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private HotelDocRepository hotelDocRepository;

    /**
     * 新增所有文档
     *
     * @param hotelList 酒店列表
     */
    public void insertAllDocument(List<Hotel> hotelList) {
        // 将酒店列表转换为酒店文档列表
        List<HotelDoc> hotelDocList = hotelList.stream().map(HotelDoc::new).toList();
        // 批量保存到Elasticsearch
        hotelDocRepository.saveAll(hotelDocList);
    }

    /**
     * 新增指定id的文档
     *
     * @param hotel 酒店
     */
    public void insertDocument(Hotel hotel) {
        HotelDoc hotelDoc = new HotelDoc(hotel);
        hotelDocRepository.save(hotelDoc);
    }

    /**
     * 构建查询条件: 会影响文档的相关度分数
     *
     * @param query 查询对象
     * @param page  页号
     * @param size  每页的文档数量
     * @param field 排序字段
     * @param name  高亮字段
     * @return NativeQuery 查询对象
     */
    public NativeQuery buildQuery(Query query, int page, int size, String field, String name) {
        NativeQueryBuilder builder = NativeQuery.builder().withQuery(query).withPageable(PageRequest.of(page, size));
        if (StringUtils.isNotEmpty(field)) {
            builder.withSort(s -> s.field(f -> f.field(field).order(SortOrder.Asc)));
        }
        if (StringUtils.isNotEmpty(name)) {
            HighlightField highlightField = new HighlightField(name);
            HighlightParameters params = HighlightParameters.builder().withRequireFieldMatch(false).build();
            Highlight highlight = new Highlight(params, Collections.singletonList(highlightField));
            builder.withHighlightQuery(new HighlightQuery(highlight, HotelDoc.class));
        }
        return builder.build();
    }

    /**
     * 构建查询条件: 会影响文档的相关度分数
     *
     * @param query 查询对象
     * @return NativeQuery 查询对象
     */
    public NativeQuery buildQuery(Query query) {
        return buildQuery(query, 0, 10, null, null);
    }

    /**
     * 构建过滤条件: 不会影响文档的相关度分数
     *
     * @param filterQuery 过滤查询对象
     * @return NativeQuery 查询对象
     */
    public NativeQuery buildFilter(Query filterQuery) {
        Query boolQuery = Query.of(q -> q.bool(b -> b.filter(filterQuery)));
        return NativeQuery.builder().withQuery(boolQuery)
                .withPageable(PageRequest.of(0, 10)).build();
    }

    /**
     * 查询文档
     *
     * @param nativeQuery 查询对象
     * @return 搜索结果
     */
    public SearchHits<HotelDoc> query(NativeQuery nativeQuery) {
        return elasticsearchOperations.search(nativeQuery, HotelDoc.class);
    }

    /**
     * 查询所有文档
     *
     * @return 酒店文档列表
     */
    public List<HotelDoc> matchAllQuery() {
        Query query = Query.of(q -> q.matchAll(m -> m));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 全文匹配查询: 对查询文本进行分词后匹配指定字段
     *
     * @param name 字段名
     * @param text 查询文本
     * @return 酒店文档列表
     */
    public List<HotelDoc> matchQuery(String name, String text) {
        Query query = Query.of(q -> q.match(m -> m.field(name).query(text)));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 多字段匹配查询: 在多个字段中搜索同一个文本
     *
     * @param text       查询文本
     * @param fieldNames 字段名列表
     * @return 酒店文档列表
     */
    public List<HotelDoc> multiMatchQuery(String text, String... fieldNames) {
        Query query = Query.of(q -> q.multiMatch(m -> m.fields(List.of(fieldNames)).query(text)));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 精确匹配查询: 不分词精确匹配字段值
     *
     * @param name  字段名
     * @param value 字段值
     * @return 酒店文档列表
     */
    public List<HotelDoc> termQuery(String name, String value) {
        Query query = Query.of(q -> q.term(t -> t.field(name).value(value)));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 范围查询: 匹配指定字段在指定范围内的文档
     *
     * @param name 字段名
     * @param from 起始值
     * @param to   结束值
     * @return 酒店文档列表
     */
    public List<HotelDoc> rangeQuery(String name, double from, double to) {
        Query query = Query.of(q -> q.range(r -> r.number(n -> n.field(name).gte(from).lte(to))));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 布尔查询: 组合多个查询条件(must/should/mustNot/filter)
     *
     * @param must    必须匹配的查询
     * @param should  可选匹配的查询
     * @param mustNot 必须不匹配的查询
     * @param filter  过滤查询
     * @return 酒店文档列表
     */
    public List<HotelDoc> boolQuery(Query must, Query should, Query mustNot, Query filter) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (Objects.nonNull(must)) {
            boolBuilder.must(must);
        }
        if (Objects.nonNull(should)) {
            boolBuilder.should(should);
        }
        if (Objects.nonNull(mustNot)) {
            boolBuilder.mustNot(mustNot);
        }
        if (Objects.nonNull(filter)) {
            boolBuilder.filter(filter);
        }
        Query query = Query.of(q -> q.bool(boolBuilder.build()));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 函数评分查询：搜索关键字并对指定品牌加权
     *
     * @param keyword    搜索关键字
     * @param boostBrand 需要加权的品牌
     * @param weight     权重值
     * @return 酒店文档列表
     */
    public List<HotelDoc> functionScoreQuery(String keyword, String boostBrand, double weight) {
        Query baseQuery;
        if (StringUtils.isBlank(keyword)) {
            // 匹配所有文档
            baseQuery = Query.of(q -> q.matchAll(m -> m));
        } else {
            // 对"all"字段进行匹配
            baseQuery = Query.of(q -> q.match(m -> m.field("all").query(keyword)));
        }
        // 过滤条件: 对指定品牌加权
        Query filterQuery = Query.of(q -> q.term(t -> t.field("brand").value(boostBrand)));
        // 构建函数评分查询
        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs
                .query(baseQuery)
                .functions(f -> f.filter(filterQuery).weight(weight))
                .boostMode(FunctionBoostMode.Sum));
        Query query = Query.of(q -> q.functionScore(functionScoreQuery));
        SearchHits<HotelDoc> searchHits = query(buildQuery(query));
        return map(searchHits);
    }

    /**
     * 地理距离查询: 匹配指定字段在指定距离范围内的文档
     *
     * @param name     字段名
     * @param lat      纬度
     * @param lon      经度
     * @param distance 距离
     * @return 酒店文档列表
     */
    public List<HotelDoc> geoDistanceQuery(String name, double lat, double lon, String distance) {
        Query query = Query.of(q -> q.geoDistance(g -> g.field(name)
                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                .distance(distance).distanceType(GeoDistanceType.Arc)));
        SearchHits<HotelDoc> searchHits = query(buildFilter(query));
        return map(searchHits);
    }

    /**
     * 根据id查询酒店文档
     *
     * @param id 文档id
     * @return 酒店文档
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
     * 将搜索结果转换为酒店文档列表
     *
     * @param searchHits 搜索结果
     * @return 酒店文档列表
     */
    public List<HotelDoc> map(SearchHits<HotelDoc> searchHits) {
        return searchHits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }
}

package com.ltx;

import com.ltx.entity.HotelDoc;
import com.ltx.repository.HotelDocRepository;
import com.ltx.service.HotelService;
import com.ltx.util.DocumentUtil;
import com.ltx.util.IndexUtil;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import javax.annotation.Resource;
import java.util.HashMap;

@SpringBootTest
public class HotelTest {

    @Resource
    private DocumentUtil documentUtil;

    @Resource
    private IndexUtil indexUtil;

    @Resource
    private HotelService hotelService;

    @Resource
    private HotelDocRepository hotelDocRepository;

    private final Long id = 36934L;


    @Test
    public void test() {
        TermsAggregationBuilder cityAgg = AggregationBuilders.terms("cityAgg").field("city");
        TermsAggregationBuilder starNameAgg = AggregationBuilders.terms("starAgg").field("starName");
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brand");
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withAggregations(
                cityAgg, starNameAgg, brandAgg
        );
        SearchHits<HotelDoc> searchHits = documentUtil.query(queryBuilder.build());
        AggregationsContainer<?> aggregations = searchHits.getAggregations();
        Aggregations aggregation = (Aggregations) aggregations.aggregations();
        ParsedStringTerms parsedStringTerms = aggregation.get("cityAgg");
        for (Terms.Bucket bucket : parsedStringTerms.getBuckets()) {
            String key = (String) bucket.getKey(); // 获取聚合的键
            long docCount = bucket.getDocCount(); // 获取符合条件的文档数量

            // 在这里进行你的处理逻辑，例如输出或者存储结果
            System.out.println("Term: " + key + ", Doc Count: " + docCount);

        }



    }

    /**
     * 创建索引
     */
    @Test
    public void createIndex() {
        indexUtil.createIndex(HotelDoc.class);
    }

    /**
     * 查询所有文档
     */
    @Test
    public void findAllDocument() {
        System.out.println(documentUtil.matchAllQuery());
    }

    /**
     * 查询指定id的文档
     */
    @Test
    public void findDocumentById() {
        System.out.println(documentUtil.findById(36934L));
    }

    /**
     * 新增所有文档
     */
    @Test
    public void insertAllDocument() {
        documentUtil.insertAllDocument(hotelService.query().list());
    }

    /**
     * 新增指定id文档
     */
    @Test
    public void insertDocument() {
        documentUtil.insertDocument(hotelService.getById(id));
    }

    /**
     * 更新指定id文档
     */
    @Test
    public void updateDocument() {
        HashMap<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("price", 336);
        fieldMap.put("city", "上海");
        documentUtil.update(id, fieldMap);
    }

    /**
     * 删除指定id文档
     */
    @Test
    public void deleteDocument() {
        documentUtil.delete(id);
    }
}

package com.ltx;

import com.ltx.constant.Constant;
import com.ltx.entity.HotelDoc;
import com.ltx.repository.HotelDocRepository;
import com.ltx.service.HotelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;

import java.util.List;
import java.util.Map;

/**
 * 酒店文档测试类
 */
@Slf4j
@SpringBootTest
public class HotelTest {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;
    @Resource
    private HotelDocRepository hotelDocRepository;
    @Resource
    private HotelService hotelService;
    private final Long id = 36934L;

    /**
     * 批量新增或更新文档
     */
    @Test
    public void saveAll() {
        // 将酒店列表转换为酒店文档列表
        List<HotelDoc> hotelDocList = hotelService.query().list().stream().map(HotelDoc::new).toList();
        // 批量保存到Elasticsearch
        hotelDocRepository.saveAll(hotelDocList);
    }

    /**
     * 新增或更新文档
     */
    @Test
    public void save() {
        HotelDoc hotelDoc = new HotelDoc(hotelService.getById(id));
        hotelDocRepository.save(hotelDoc);
    }

    /**
     * 查询指定id的文档
     */
    @Test
    public void findById() {
        System.out.println(hotelDocRepository.findById(id).orElse(null));
    }

    /**
     * 删除指定id的文档
     */
    @Test
    public void deleteById() {
        hotelDocRepository.deleteById(id);
        log.info("删除文档成功, id: {}", id);
    }

    /**
     * 更新指定id的文档
     */
    @Test
    public void update() {
        // 获取索引坐标
        IndexCoordinates indexCoordinates = IndexCoordinates.of(Constant.INDEX_NAME);
        // isAd字段设为true
        Document document = Document.from(Map.of("isAd", true));
        UpdateQuery updateQuery = UpdateQuery.builder(id.toString()).withDocument(document).build();
        UpdateResponse updateResponse = elasticsearchOperations.update(updateQuery, indexCoordinates);
        log.info("更新文档成功, id: {}, 结果: {}", id, updateResponse.getResult());
    }

    /**
     * 重建索引
     */
    @Test
    public void recreateIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(HotelDoc.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
            log.info("旧索引已删除");
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping());
        log.info("新索引已根据注解重建");
        saveAll();
    }
}
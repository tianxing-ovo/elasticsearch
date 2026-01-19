package com.ltx;

import com.ltx.entity.HotelDoc;
import com.ltx.service.HotelService;
import com.ltx.util.DocumentUtil;
import com.ltx.util.IndexUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

@SpringBootTest
public class HotelTest {

    @Resource
    private DocumentUtil documentUtil;
    @Resource
    private IndexUtil indexUtil;
    @Resource
    private HotelService hotelService;
    private final Long id = 36934L;

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

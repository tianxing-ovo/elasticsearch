package com.ltx;

import com.ltx.entity.HotelDoc;
import com.ltx.service.HotelService;
import com.ltx.util.IndexUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.HashMap;

@SpringBootTest
public class HotelTest {

    @Resource
    private HotelService hotelService;

    @Resource
    private IndexUtil indexUtil;

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
        System.out.println(hotelService.findAll());
    }

    /**
     * 查询指定id的文档
     */
    @Test
    public void findDocumentById() {
        System.out.println(hotelService.findById(36934L));
    }

    /**
     * 新增所有文档
     */
    @Test
    public void insertAllDocument() {
        hotelService.insertAllDocument();
    }

    /**
     * 新增指定id文档
     */
    @Test
    public void insertDocument() {
        hotelService.insertDocument(id);
    }

    /**
     * 更新指定id文档
     */
    @Test
    public void updateDocument() {
        HashMap<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("price", 336);
        fieldMap.put("city", "上海");
        hotelService.update(id, fieldMap);
    }

    /**
     * 删除指定id文档
     */
    @Test
    public void deleteDocument() {
        hotelService.delete(id);
    }
}

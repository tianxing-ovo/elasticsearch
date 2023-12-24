package com.ltx;

import com.ltx.entity.HotelDoc;
import com.ltx.service.HotelService;
import com.ltx.util.IndexUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class HotelTest {

    @Resource
    private HotelService hotelService;

    @Resource
    private IndexUtil indexUtil;

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
        Long id = 1L;
        hotelService.insertDocument(id);
    }
}

package com.ltx.controller;

import com.ltx.entity.PageResult;
import com.ltx.entity.SearchRequestBody;
import com.ltx.service.HotelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

/**
 * @author tianxing
 */
@RestController
@RequestMapping("/hotel")
@CrossOrigin("http://localhost:88")
public class HotelController {

    @Resource
    private HotelService hotelService;

    /**
     * 搜索酒店
     *
     * @param searchRequestBody 搜索请求体
     * @return 酒店列表
     */
    @PostMapping("/list")
    public PageResult search(@RequestBody SearchRequestBody searchRequestBody) {
        return hotelService.search(searchRequestBody);
    }

    /**
     * 获取酒店筛选条件
     *
     * @param searchRequestBody 搜索请求体
     * @return 酒店筛选条件
     */
    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody SearchRequestBody searchRequestBody) {
        return hotelService.filters(searchRequestBody);
    }
}

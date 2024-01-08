package com.ltx.controller;

import com.ltx.entity.PageResult;
import com.ltx.entity.SearchRequestBody;
import com.ltx.service.HotelService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
@CrossOrigin("http://localhost:88")
public class HotelController {

    @Resource
    private HotelService hotelService;

    @PostMapping("/list")
    public PageResult search(@RequestBody SearchRequestBody searchRequestBody) {
        return hotelService.search(searchRequestBody);
    }

    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody SearchRequestBody searchRequestBody) {
        return hotelService.filters(searchRequestBody);
    }


}

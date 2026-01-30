package com.ltx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ltx.entity.Hotel;
import com.ltx.entity.PageResult;
import com.ltx.entity.SearchRequestBody;

import java.util.List;
import java.util.Map;

/**
 * @author tianxing
 */
public interface HotelService extends IService<Hotel> {

    PageResult search(SearchRequestBody searchRequestBody);

    Map<String, List<String>> filters(SearchRequestBody searchRequestBody);

    List<String> getSuggestions(String prefix);
}
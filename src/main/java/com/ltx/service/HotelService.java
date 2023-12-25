package com.ltx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;

import java.util.List;
import java.util.Map;


public interface HotelService extends IService<Hotel> {

    void insertAllDocument();

    void insertDocument(Long id);

    List<HotelDoc> findAll();

    HotelDoc findById(Long id);

    void update(Long id, Map<String, Object> fieldMap);

    void delete(Long id);

}
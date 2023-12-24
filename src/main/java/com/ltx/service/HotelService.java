package com.ltx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;

import java.util.List;


public interface HotelService extends IService<Hotel> {

    void insertAllDocument();

    void insertDocument(Long id);

    List<HotelDoc> findAll();

}
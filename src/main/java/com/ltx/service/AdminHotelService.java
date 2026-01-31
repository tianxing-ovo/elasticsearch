package com.ltx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ltx.entity.Hotel;

/**
 * 酒店后台管理服务
 *
 * @author tianxing
 */
public interface AdminHotelService extends IService<Hotel> {

    void addHotel(Hotel hotel);

    void updateHotel(Hotel hotel);

    void deleteHotel(Long id);

    Hotel getHotelById(Long id);

    void setHotelAd(Long id, Boolean isAd);
}

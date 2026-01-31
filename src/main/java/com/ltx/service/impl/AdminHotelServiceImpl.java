package com.ltx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ltx.entity.Hotel;
import com.ltx.mapper.HotelMapper;
import com.ltx.service.AdminHotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author tianxing
 */
@Slf4j
@Service
public class AdminHotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements AdminHotelService {

    /**
     * 新增酒店
     *
     * @param hotel 酒店
     */
    @Override
    public void addHotel(Hotel hotel) {
        this.save(hotel);
        log.info("酒店已保存, id={}", hotel.getId());
    }

    /**
     * 更新酒店
     *
     * @param hotel 酒店
     */
    @Override
    public void updateHotel(Hotel hotel) {
        this.updateById(hotel);
        log.info("酒店已更新, id={}", hotel.getId());
    }

    /**
     * 删除酒店
     *
     * @param id 酒店ID
     */
    @Override
    public void deleteHotel(Long id) {
        this.removeById(id);
        log.info("酒店已删除, id={}", id);
    }
    /**
     * 根据ID查询酒店
     *
     * @param id 酒店ID
     * @return 酒店
     */
    @Override
    public Hotel getHotelById(Long id) {
        return this.getById(id);
    }

    /**
     * 设置酒店广告状态
     *
     * @param id    酒店ID
     * @param isAd  是否为广告
     */
    @Override
    public void setHotelAd(Long id, Boolean isAd) {
        Hotel hotel = this.getById(id);
        if (hotel == null) {
            log.warn("酒店不存在, id={}", id);
            return;
        }
        hotel.setIsAd(isAd);
        this.updateById(hotel);
        log.info("酒店广告状态已更新, id={}, isAd={}", id, isAd);
    }
}

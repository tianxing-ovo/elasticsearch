package com.ltx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ltx.constant.MqConstant;
import com.ltx.entity.Hotel;
import com.ltx.mapper.HotelMapper;
import com.ltx.service.AdminHotelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * @author tianxing
 */
@Slf4j
@Service
public class AdminHotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements AdminHotelService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 新增酒店
     *
     * @param hotel 酒店
     * @return 是否新增成功
     */
    @Override
    public boolean save(Hotel hotel) {
        boolean success = super.save(hotel);
        if (success) {
            log.info("新增酒店成功, id={}", hotel.getId());
            rabbitTemplate.convertAndSend(MqConstant.HOTEL_EXCHANGE, MqConstant.HOTEL_INSERT_KEY, hotel.getId());
        }
        return success;
    }

    /**
     * 更新酒店
     *
     * @param hotel 酒店
     * @return 是否更新成功
     */
    @Override
    public boolean updateById(Hotel hotel) {
        boolean success = super.updateById(hotel);
        if (success) {
            log.info("更新酒店成功, id={}", hotel.getId());
            rabbitTemplate.convertAndSend(MqConstant.HOTEL_EXCHANGE, MqConstant.HOTEL_INSERT_KEY, hotel.getId());
        }
        return success;
    }

    /**
     * 删除酒店
     *
     * @param id 酒店ID
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(Serializable id) {
        boolean success = super.removeById(id);
        if (success) {
            log.info("删除酒店成功, id={}", id);
            rabbitTemplate.convertAndSend(MqConstant.HOTEL_EXCHANGE, MqConstant.HOTEL_DELETE_KEY, id);
        }
        return success;
    }

    /**
     * 设置酒店广告状态
     *
     * @param id   酒店ID
     * @param isAd 是否为广告
     */
    @Override
    public void setHotelAd(Long id, Boolean isAd) {
        boolean success = lambdaUpdate().eq(Hotel::getId, id).set(Hotel::getIsAd, isAd).update();
        if (success) {
            log.info("酒店广告状态已更新, id={}, isAd={}", id, isAd);
            rabbitTemplate.convertAndSend(MqConstant.HOTEL_EXCHANGE, MqConstant.HOTEL_INSERT_KEY, id);
        } else {
            log.warn("设置广告状态失败, 酒店不存在, id={}", id);
            throw new RuntimeException("酒店不存在");
        }
    }
}

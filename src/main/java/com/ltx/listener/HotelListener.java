package com.ltx.listener;

import com.ltx.constant.MqConstant;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.repository.HotelDocRepository;
import com.ltx.service.AdminHotelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 监听酒店数据变化
 *
 * @author tianxing
 */
@Slf4j
@Component
public class HotelListener {

    @Resource
    private AdminHotelService adminHotelService;

    @Resource
    private HotelDocRepository hotelDocRepository;

    /**
     * 监听酒店新增或修改
     *
     * @param id 酒店ID
     */
    @RabbitListener(queues = MqConstant.HOTEL_INSERT_QUEUE)
    public void listenHotelInsertOrUpdate(Long id) {
        // 根据ID查询最新酒店数据
        Hotel hotel = adminHotelService.getById(id);
        if (hotel == null) {
            log.error("没有找到酒店信息, id={}", id);
            return;
        }
        // 转换为HotelDoc对象
        HotelDoc hotelDoc = new HotelDoc(hotel);
        // 保存到ES
        hotelDocRepository.save(hotelDoc);
        log.info("ES同步成功, id={}", id);
    }

    /**
     * 监听酒店删除
     *
     * @param id 酒店ID
     */
    @RabbitListener(queues = MqConstant.HOTEL_DELETE_QUEUE)
    public void listenHotelDelete(Long id) {
        hotelDocRepository.deleteById(id);
        log.info("ES删除成功, id={}", id);
    }
}

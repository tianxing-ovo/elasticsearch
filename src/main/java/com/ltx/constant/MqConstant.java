package com.ltx.constant;

/**
 * RabbitMQ常量
 *
 * @author tianxing
 */
public interface MqConstant {
    /**
     * 交换机
     */
    String HOTEL_EXCHANGE = "hotel.topic";

    /**
     * 监听新增和修改的队列
     */
    String HOTEL_INSERT_QUEUE = "hotel.insert.queue";

    /**
     * 监听删除的队列
     */
    String HOTEL_DELETE_QUEUE = "hotel.delete.queue";

    /**
     * 新增或修改的RoutingKey
     */
    String HOTEL_INSERT_KEY = "hotel.insert";

    /**
     * 删除的RoutingKey
     */
    String HOTEL_DELETE_KEY = "hotel.delete";
}

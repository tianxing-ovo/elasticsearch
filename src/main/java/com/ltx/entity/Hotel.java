package com.ltx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 数据库实体类
 *
 * @author tianxing
 */
@Data
public class Hotel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private Integer price;
    private Integer score;
    private String brand;
    private String city;
    private String starName;
    private String business;
    private String latitude;
    private String longitude;
    private String pic;
    private Boolean isAd;
}

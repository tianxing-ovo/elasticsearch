package com.ltx.entity;

import com.ltx.constant.HotelConstant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;

/**
 * ES实体类
 *
 * @author tianxing
 */
@Document(indexName = HotelConstant.INDEX_NAME)
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class HotelDoc {
    @Field(type = FieldType.Keyword)
    private Long id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String name;
    @Field(type = FieldType.Keyword, index = false)
    private String address;
    @Field(type = FieldType.Integer)
    private Integer price;
    @Field(type = FieldType.Integer)
    private Integer score;
    @Field(type = FieldType.Keyword, copyTo = "all")
    private String brand;
    @Field(type = FieldType.Keyword)
    private String city;
    @Field(type = FieldType.Keyword)
    private String starName;
    @Field(type = FieldType.Keyword, copyTo = "all")
    private String business;
    @GeoPointField
    private String location;
    @Field(type = FieldType.Keyword, index = false)
    private String pic;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String all;

    private Object distance;
    // 是否投了广告
    private Boolean isAD;

    public HotelDoc(Hotel hotel) {
        BeanUtils.copyProperties(hotel, this, "latitude", "longitude");
        this.location = hotel.getLatitude() + ", " + hotel.getLongitude();
    }
}

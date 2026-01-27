package com.ltx.entity;

import lombok.Data;

/**
 * 搜索请求体
 *
 * @author tianxing
 */
@Data
public class SearchRequestBody {
    private String key;
    private Integer pageNumber = 1;
    private Integer pageSize = 10;
    private String sortBy;
    private String sortOrder = "asc";
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;
}

package com.ltx.entity;

import lombok.Data;

@Data
public class SearchRequestBody {
    private String key;
    private Integer page = 1;
    private Integer size = 10;
    private String sortBy;
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;
}

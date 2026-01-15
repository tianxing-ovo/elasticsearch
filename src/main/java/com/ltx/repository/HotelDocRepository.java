package com.ltx.repository;

import com.ltx.entity.HotelDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 操作hotel索引
 *
 * @author tianxing
 */
public interface HotelDocRepository extends ElasticsearchRepository<HotelDoc, Long> {
}

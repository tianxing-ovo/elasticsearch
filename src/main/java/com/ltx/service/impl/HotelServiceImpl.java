package com.ltx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ltx.constant.HotelConstant;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.mapper.HotelMapper;
import com.ltx.repository.HotelDocRepository;
import com.ltx.service.HotelService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Resource
    private HotelDocRepository hotelDocRepository;

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 新增所有文档
     */
    @Override
    public void insertAllDocument() {
        List<HotelDoc> hotelDocList = query().list().stream().map(HotelDoc::new).collect(Collectors.toList());
        hotelDocRepository.saveAll(hotelDocList);
    }

    /**
     * 新增指定id的文档
     */
    @Override
    public void insertDocument(Long id) {
        HotelDoc hotelDoc = new HotelDoc(baseMapper.selectById(id));
        hotelDocRepository.save(hotelDoc);
    }


    /**
     * 查询所有文档
     */
    @Override
    public List<HotelDoc> findAll() {
        List<HotelDoc> list = new ArrayList<>();
        for (HotelDoc hotelDoc : hotelDocRepository.findAll()) {
            list.add(hotelDoc);
        }
        return list;
    }

    /**
     * 根据id查询文档
     */
    @Override
    public HotelDoc findById(Long id) {
        Optional<HotelDoc> optional = hotelDocRepository.findById(id);
        return optional.orElseGet(HotelDoc::new);
    }


    /**
     * 更新指定id的文档
     *
     * @param id       文档id
     * @param fieldMap key-字段名 value-字段值
     */
    @Override
    public void update(Long id, Map<String, Object> fieldMap) {
        // 获取索引坐标
        IndexCoordinates indexCoordinates = IndexCoordinates.of(HotelConstant.INDEX_NAME);
        Document document = Document.from(fieldMap);
        UpdateQuery updateQuery = UpdateQuery.builder(id.toString()).withDocument(document).build();
        UpdateResponse updateResponse = elasticsearchOperations.update(updateQuery, indexCoordinates);
        System.out.println(updateResponse.getResult());
    }

    /**
     * 删除文档
     *
     * @param id 文档id
     */
    public void delete(Long id) {
        hotelDocRepository.deleteById(id);
        System.out.println("删除成功");
    }
}

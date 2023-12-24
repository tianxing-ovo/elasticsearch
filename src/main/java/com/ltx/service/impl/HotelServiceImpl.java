package com.ltx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ltx.entity.Hotel;
import com.ltx.entity.HotelDoc;
import com.ltx.mapper.HotelMapper;
import com.ltx.repository.HotelDocRepository;
import com.ltx.service.HotelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Resource
    private HotelDocRepository hotelDocRepository;

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
}

package com.ltx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ltx.entity.Hotel;
import com.ltx.service.AdminHotelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 酒店后台管理
 *
 * @author tianxing
 */
@RestController
@RequestMapping("/admin/hotel")
public class AdminHotelController {

    @Resource
    private AdminHotelService adminHotelService;

    /**
     * 分页查询酒店列表
     *
     * @param pageNumber 页码
     * @param pageSize   每页大小
     * @return 酒店列表
     */
    @GetMapping("/list")
    public Page<Hotel> list(@RequestParam(defaultValue = "1") Integer pageNumber,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminHotelService.page(new Page<>(pageNumber, pageSize));
    }

    /**
     * 根据ID查询酒店详情
     *
     * @param id 酒店ID
     * @return 酒店
     */
    @GetMapping("/{id}")
    public Hotel getById(@PathVariable Long id) {
        return adminHotelService.getHotelById(id);
    }

    /**
     * 新增酒店
     *
     * @param hotel 酒店
     * @return 操作结果
     */
    @PostMapping
    public String add(@RequestBody Hotel hotel) {
        adminHotelService.addHotel(hotel);
        return "新增成功";
    }

    /**
     * 更新酒店
     *
     * @param hotel 酒店信息
     * @return 操作结果
     */
    @PutMapping
    public String update(@RequestBody Hotel hotel) {
        adminHotelService.updateHotel(hotel);
        return "更新成功";
    }

    /**
     * 删除酒店
     *
     * @param id 酒店ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        adminHotelService.deleteHotel(id);
        return "删除成功";
    }

    /**
     * 设置酒店广告状态
     *
     * @param id   酒店ID
     * @param isAd 是否为广告
     * @return 操作结果
     */
    @PutMapping("/{id}/ad")
    public String setAd(@PathVariable Long id, @RequestParam Boolean isAd) {
        adminHotelService.setHotelAd(id, isAd);
        return isAd ? "已设为广告" : "已取消广告";
    }
}

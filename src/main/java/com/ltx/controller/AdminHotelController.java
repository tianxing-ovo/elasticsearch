package com.ltx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ltx.entity.Hotel;
import com.ltx.entity.Result;
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
    public Result<Page<Hotel>> list(@RequestParam(defaultValue = "1") Integer pageNumber,
                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminHotelService.page(new Page<>(pageNumber, pageSize)));
    }

    /**
     * 根据ID查询酒店详情
     *
     * @param id 酒店ID
     * @return {@link Result<Hotel>}
     */
    @GetMapping("/{id}")
    public Result<Hotel> getById(@PathVariable Long id) {
        return Result.success(adminHotelService.getById(id));
    }

    /**
     * 新增酒店
     *
     * @param hotel 酒店
     * @return {@link Result<Boolean>}
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Hotel hotel) {
        return Result.success(adminHotelService.save(hotel));
    }

    /**
     * 更新酒店
     *
     * @param hotel 酒店信息
     * @return 操作结果
     */
    @PutMapping
    public Result<Boolean> updateById(@RequestBody Hotel hotel) {
        return Result.success(adminHotelService.updateById(hotel));
    }

    /**
     * 删除酒店
     *
     * @param id 酒店ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeById(@PathVariable Long id) {
        return Result.success(adminHotelService.removeById(id));
    }

    /**
     * 设置酒店广告状态
     *
     * @param id   酒店ID
     * @param isAd 是否为广告
     * @return {@link Result<Void>}
     */
    @PutMapping("/{id}/ad")
    public Result<Void> setAd(@PathVariable Long id, @RequestParam Boolean isAd) {
        adminHotelService.setHotelAd(id, isAd);
        return Result.success();
    }
}

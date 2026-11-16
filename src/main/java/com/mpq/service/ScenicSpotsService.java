package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicSpots;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

/**
 * <p>
 * 景点信息表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-03-20
 */
public interface ScenicSpotsService extends IService<ScenicSpots> {

    Page<ScenicSpots> getScenicSpotsList(Page<ScenicSpots> page);

    Result addScenicSpots(ScenicSpots scenicSpots);

    Result updateScenicSpots(ScenicSpots scenicSpots);

    Result deleteScenicSpots(Integer id);

    ScenicSpots getScenicSpotsById(Integer id);

    Page<ScenicSpots> getByConditional(Page<ScenicSpots> page, BigDecimal ticketPrice,String sortBy );

    Page<ScenicSpots> getHotScenicSpots(Page<ScenicSpots> page);
    void avgScore(Integer scenicId);

    Page<ScenicSpots> searchSpots(Page<ScenicSpots> page, String keyword);

    Page<ScenicSpots> getScenicSpotsListByLocaltion(Page<ScenicSpots> pageResult, String location);

    Result updateViews(Integer id);
}

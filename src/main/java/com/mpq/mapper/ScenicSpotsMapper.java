package com.mpq.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.ScenicSpots;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 景点信息表 Mapper 接口
 * </p>
 *
 * @author mpq
 * @since 2026-03-20
 */
@Mapper
public interface ScenicSpotsMapper extends BaseMapper<ScenicSpots> {

    int updateScenicSpots(ScenicSpots scenicSpots);

    Page<ScenicSpots> searchSpots(Page<ScenicSpots> page, @Param("keyword") String keyword);}

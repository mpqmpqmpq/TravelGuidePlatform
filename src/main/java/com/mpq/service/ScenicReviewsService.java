package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicReviews;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.vo.ScenicReviewsAndUserVo;

/**
 * <p>
 * 景点评论表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-03-21
 */
public interface ScenicReviewsService extends IService<ScenicReviews> {

    Page<ScenicReviewsAndUserVo> getScenicReviews(Page<ScenicReviews> page, Integer scenicId);

    Result addScenicReviews(ScenicReviews scenicReviews);

    Result deleteScenicReviews(Integer id);
}

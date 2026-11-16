package com.mpq.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.ScenicReviews;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.vo.ScenicReviewsAndUserVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 景点评论表 Mapper 接口
 * </p>
 *
 * @author mpq
 * @since 2026-03-21
 */
@Mapper
public interface ScenicReviewsMapper extends BaseMapper<ScenicReviews> {

    Page<ScenicReviewsAndUserVo> getListAndUser(Page<ScenicReviews> page, Integer scenicId);
}

package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.Like;
import com.mpq.pojo.vo.LikeWithArticleVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    Page<LikeWithArticleVo> selectLikeWithArticle(Page<LikeWithArticleVo> page, Integer userId);
}

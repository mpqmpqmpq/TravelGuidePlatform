package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.BookMarks;
import com.mpq.pojo.vo.ArticleCancelVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookMarksMapper extends BaseMapper<BookMarks> {
    List<ArticleCancelVo> selectArticleCancel(Integer userId);
}

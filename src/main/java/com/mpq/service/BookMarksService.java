package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.BookMarks;
import com.mpq.pojo.vo.ArticleCancelVo;

import java.util.List;

public interface BookMarksService extends IService<BookMarks> {
    Result addBookMarks(Integer userId, Integer articleId);

    boolean cancelBookMarks(Integer id);

    List<ArticleCancelVo> listBookMarks(Integer userId);

    void deleteBookMarksByArticleId(Integer articleId);


}

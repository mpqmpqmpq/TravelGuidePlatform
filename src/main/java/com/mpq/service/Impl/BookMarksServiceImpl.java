package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.BookMarksMapper;
import com.mpq.pojo.BookMarks;
import com.mpq.pojo.vo.ArticleCancelVo;
import com.mpq.service.BookMarksService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookMarksServiceImpl extends ServiceImpl<BookMarksMapper, BookMarks> implements BookMarksService {
    private final BookMarksMapper bookMarksMapper;

    @Override
    public Result addBookMarks(Integer userId, Integer articleId) {
        BookMarks bookMarks = new BookMarks();
        bookMarks.setUserId(userId);
        bookMarks.setArticleId(articleId);
        bookMarks.setCreatedAt(LocalDateTime.now());
        int insert = bookMarksMapper.insert(bookMarks);
        if (insert > 0) {
            return Result.success("收藏成功");
        }
        return Result.failure(500, "收藏失败，服务器内部错误");
    }

    @Override
    public boolean cancelBookMarks(Integer id) {
        if (id != null) {
            bookMarksMapper.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<ArticleCancelVo> listBookMarks(Integer userId) {
        List<ArticleCancelVo> articleCancelVos = bookMarksMapper.selectArticleCancel(userId);
        return articleCancelVos;
    }

    @Transactional
    @Override
    public void deleteBookMarksByArticleId(Integer articleId) {
        LambdaQueryWrapper<BookMarks> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookMarks::getArticleId, articleId);
        bookMarksMapper.delete(queryWrapper);
    }
}

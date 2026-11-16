package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.pojo.vo.ArticleCancelVo;
import com.mpq.service.BookMarksService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookMarks")
@RequiredArgsConstructor
@Slf4j
public class BookMarksController {
    private final BookMarksService bookMarksService;

    /**
     * 收藏文章
     */
   @PostMapping("/add")
    public Result addBookMarks(@RequestParam  Integer articleId) {
       Integer userId = UserContextHolder.getUserId();
       log.info("articleId:{}",articleId);
      return bookMarksService.addBookMarks(userId, articleId);
    }

    /**
     * 取消收藏文章
     */
    @DeleteMapping("/cancel/{id}")
    public Result cancelBookMarks(@PathVariable  Integer id) {
        boolean b = bookMarksService.cancelBookMarks(id);
        if (!b) {
            return Result.failure(500, "取消收藏失败");
        }
        return Result.success("取消收藏成功");
    }

    /**
     * 获取收藏文章列表
     */
    @GetMapping("/list")
    public Result listBookMarks() {
        Integer userId = UserContextHolder.getUserId();
        List<ArticleCancelVo> articleCancelVos = bookMarksService.listBookMarks(userId);
        return Result.success("获取收藏文章列表成功", articleCancelVos);
    }
}

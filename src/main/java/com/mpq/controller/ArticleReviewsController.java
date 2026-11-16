package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.vo.ArticleReviewWithUserVO;
import com.mpq.service.ArticleReviewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/articleReviews")
@RestController
@Slf4j
public class ArticleReviewsController {
    private final ArticleReviewsService articleReviewsService;

    /**
     * 获取文章审核列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    public Result getArticleReviews( @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize
    ) {
        Page<ArticleReview> page = new Page<>(pageNum, pageSize);
        return articleReviewsService.getArticleReviews(page);
    }

    /**
     * 获取文章审核列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/listWithUser")
    public Result getArticleReviewsWithUser(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Integer status) {
        Page<ArticleReviewWithUserVO> page = new Page<>(pageNum, pageSize);
        return articleReviewsService.getArticleReviewsWithUser(page, status);
    }

    /**
     * 获取文章审核列表
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getArticleReviewsById(@PathVariable Integer id) {
        ArticleReview articleReview = articleReviewsService.getById(id);
        return Result.success("200",articleReview);
    }
    /**
     * 审核攻略
     * @param articleReview
     * @return
     */
    @PostMapping("/review")
    public Result ReviewArticles(@RequestBody ArticleReview articleReview){
      return   articleReviewsService.ReviewArticles(articleReview);
    }

}

package com.mpq.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ArticleTags;
import com.mpq.service.ArticleTagsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article-tags")
@RequiredArgsConstructor
public class ArticleTagsController {
    private final ArticleTagsService articleTagsService;
    @PostMapping("/save/{articleId}")
    public Result saveArticleTags(@PathVariable Integer articleId, @RequestBody List<Integer> tagIds) {
      return   articleTagsService.saveArticleTags(articleId, tagIds);

    }
    @DeleteMapping("/delete/{articleId}")
    public Result deleteByArticleId(@PathVariable Integer articleId) {
       return articleTagsService.deleteByArticleId(articleId);

    }
    @GetMapping("/byArticle/{articleId}")
    public Result<List<ArticleTags>> getTagsByArticleId(@PathVariable Integer articleId) {
        return Result.success("查询成功", articleTagsService.listByArticleId(articleId));
    }
    @GetMapping("/getTagsList")
    public Result getTagsList(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int size
    ) {
        Page<ArticleTags> page = new Page<>(pageNum, size);
        Page  resultPage = articleTagsService.getTagsList(page);
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success("查询成功", result);
    }

}
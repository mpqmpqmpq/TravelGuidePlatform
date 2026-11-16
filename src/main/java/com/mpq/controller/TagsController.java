package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Tags;
import com.mpq.service.TagsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagsController {
    private final TagsService tagsService;

    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Tags> page = new Page<>(pageNum, size);
        Page<Tags> resultPage = tagsService.getTags(page);
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        return Result.success("查询成功", result);
    }

    @PostMapping("/findOrCreate")
    public Result<List<Tags>> findOrCreateTags(@RequestBody List<String> tagNames) {
        return Result.success("标签创建成功", tagsService.findOrCreateTags(tagNames));
    }

    @PostMapping("/saveWithArticle/{articleId}")
    public Result saveWithArticle(@PathVariable Integer articleId, @RequestBody List<String> tagNames) {
        return tagsService.saveWithArticle(articleId, tagNames);
    }



    @DeleteMapping("/delete/{tagId}")
    public Result delete(@PathVariable Integer tagId) {
        return tagsService.deleteTag(tagId);
    }

    @DeleteMapping("/deleteByIds")
    public Result deleteByIds(@RequestBody List<Integer> tagIds) {
        return tagsService.deleteByIds(tagIds);
    }

}
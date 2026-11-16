package com.mpq.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicReviews;
import com.mpq.pojo.vo.ScenicReviewsAndUserVo;
import com.mpq.service.ScenicReviewsService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 景点评论表
 * </p>
 *
 * @author mpq
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/scenic-reviews")
@RequiredArgsConstructor
public class ScenicReviewsController {
    private final ScenicReviewsService scenicReviewsService;

    @RequestMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") Integer scenicId
    ){
        Page<ScenicReviews> page = new Page<>(pageNum, size);
        Page<ScenicReviewsAndUserVo> pageResult = scenicReviewsService.getScenicReviews(page, scenicId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return Result.success("查询成功", result);
    }
    @PostMapping("/add")
    public Result addScenicReviews(@RequestBody ScenicReviews scenicReviews){


        Integer userId = UserContextHolder.getUserId();
        scenicReviews.setUserId(userId);
        scenicReviews.setCreatedAt(LocalDateTime.now());

       return scenicReviewsService.addScenicReviews(scenicReviews);
    }
    @DeleteMapping("/delete/{id}")
    public Result deleteScenicReviews(@PathVariable Integer id){

        return scenicReviewsService.deleteScenicReviews(id);
    }
}

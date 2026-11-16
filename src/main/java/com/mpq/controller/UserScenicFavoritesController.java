package com.mpq.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.UserScenicFavorites;
import com.mpq.service.UserScenicFavoritesService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户收藏景点表 前端控制器
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/user-scenic-favorites")
@RequiredArgsConstructor
public class UserScenicFavoritesController {
    private final UserScenicFavoritesService userScenicFavoritesService;

    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserScenicFavorites> page = new Page<>(pageNum, size);
        Integer userId = UserContextHolder.getUserId();
        page = userScenicFavoritesService.getByUserId(page, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success("获取成功", result);
    }

    @PostMapping("/add")
    public Result add( @RequestBody UserScenicFavorites userScenicFavorites) {
        Integer userId = UserContextHolder.getUserId();
        userScenicFavorites.setUserId(userId);
        userScenicFavorites.setCreatedAt(LocalDateTime.now());

        return userScenicFavoritesService.add(userScenicFavorites);

    }

    @DeleteMapping("/delete/{scenicId}")
    public Result delete(@PathVariable Integer scenicId) {
        Integer userId = UserContextHolder.getUserId();
        UserScenicFavorites userScenicFavor = new UserScenicFavorites();
        userScenicFavor.setUserId(userId);
        userScenicFavor.setScenicId(scenicId);

        return userScenicFavoritesService.delete(userScenicFavor);
    }
}

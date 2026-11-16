package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicSpots;
import com.mpq.pojo.UserScenicFavorites;
import com.mpq.mapper.UserScenicFavoritesMapper;
import com.mpq.service.ScenicSpotsService;
import com.mpq.service.UserScenicFavoritesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 用户收藏景点表 服务实现类
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserScenicFavoritesServiceImpl extends ServiceImpl<UserScenicFavoritesMapper, UserScenicFavorites> implements UserScenicFavoritesService {

    private final ScenicSpotsService scenicSpotsService;

    @Override
    public Result add(UserScenicFavorites userScenicFavorites) {
        log.info("收藏景点：{}", userScenicFavorites);
        LambdaQueryWrapper<UserScenicFavorites> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserScenicFavorites::getUserId, userScenicFavorites.getUserId())
                .eq(UserScenicFavorites::getScenicId, userScenicFavorites.getScenicId());
        if (this.getOne(queryWrapper) != null) {
            return Result.success("已收藏");
        }
        ScenicSpots scenicSpots = scenicSpotsService.getById(userScenicFavorites.getScenicId());
       log.info("收藏景点：{}", scenicSpots);
        scenicSpots.setFavoriteCount(scenicSpots.getFavoriteCount() + 1);

        scenicSpotsService.updateById(scenicSpots);

        return this.save(userScenicFavorites) ? Result.success("收藏成功") : Result.failure(500, "收藏失败");

    }

    @Transactional
    @Override
    public Result delete(UserScenicFavorites userScenicFavor) {

        LambdaQueryWrapper<UserScenicFavorites> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserScenicFavorites::getUserId, userScenicFavor.getUserId())
                .eq(UserScenicFavorites::getScenicId, userScenicFavor.getScenicId());
        if (this.getOne(queryWrapper) == null) {
            return Result.success("取消成功");
        }

        ScenicSpots scenicSpots = scenicSpotsService.getById(userScenicFavor.getScenicId());
        scenicSpots.setFavoriteCount(scenicSpots.getFavoriteCount() - 1);
        scenicSpotsService.updateById(scenicSpots);

        this.remove(queryWrapper);

        return Result.success("取消成功");
    }

    @Override
    public Page<UserScenicFavorites> getByUserId(Page<UserScenicFavorites> page, Integer userId) {
        LambdaQueryWrapper<UserScenicFavorites> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserScenicFavorites::getUserId, userId);
        return this.page(page, queryWrapper);

    }
}

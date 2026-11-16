package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicReviews;
import com.mpq.mapper.ScenicReviewsMapper;
import com.mpq.pojo.User;
import com.mpq.pojo.UserRole;
import com.mpq.pojo.vo.ScenicReviewsAndUserVo;
import com.mpq.service.ScenicReviewsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.service.ScenicSpotsService;
import com.mpq.service.UserService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 景点评论表 服务实现类
 * </p>
 *
 * @author mpq
 * @since 2026-03-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScenicReviewsServiceImpl extends ServiceImpl<ScenicReviewsMapper, ScenicReviews> implements ScenicReviewsService {
    private final ScenicReviewsMapper scenicReviewsMapper;
    private final ScenicSpotsService scenicSpotsService;
    private final UserService userService;

    @Override
    public Page<ScenicReviewsAndUserVo> getScenicReviews(Page<ScenicReviews> page, Integer scenicId) {
        Page<ScenicReviewsAndUserVo> pageListAndUser = scenicReviewsMapper.getListAndUser(page, scenicId);
        return pageListAndUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addScenicReviews(ScenicReviews scenicReviews) {
        long t1 = System.currentTimeMillis();
        int insert = scenicReviewsMapper.insert(scenicReviews);

        if (insert > 0) {
            CompletableFuture.runAsync(() -> scenicSpotsService.avgScore(scenicReviews.getScenicId()));
            long t2 = System.currentTimeMillis();
            log.info("修改平均分需要时间：{}",t2-t1);
            return Result.success("添加成功");
        }
        return Result.failure(500, "添加失败");
    }

    @Override
    public Result deleteScenicReviews(Integer id) {
        LambdaQueryWrapper<ScenicReviews> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicReviews::getReviewId, id);
        ScenicReviews scenicReviews = scenicReviewsMapper.selectById(id);
        Integer userId = UserContextHolder.getUserId();
        User currentById = userService.getCurrentById(userId);
        if (!Objects.equals(userId, scenicReviews.getUserId()) && !currentById.getRole().equals(UserRole.admin))
            return Result.failure(403, "无权限删除");

        int delete = scenicReviewsMapper.deleteById(id);

        if (delete > 0) {
            scenicSpotsService.avgScore(scenicReviews.getScenicId());
            return Result.success("删除成功");
        }
        return Result.failure(500, "删除失败");
    }


}

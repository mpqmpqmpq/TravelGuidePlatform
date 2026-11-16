package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.ScenicReviewsMapper;
import com.mpq.mapper.ScenicSpotsMapper;
import com.mpq.pojo.ScenicReviews;
import com.mpq.pojo.ScenicSpots;
import com.mpq.service.ScenicSpotsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service

public class ScenicSpotsServiceImpl extends ServiceImpl<ScenicSpotsMapper, ScenicSpots> implements ScenicSpotsService {

    private static final String CACHE_KEY_HOT_SCENIC_SPOTS = "scenicSpots:hot";
    private final ScenicSpotsMapper scenicSpotsMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ScenicReviewsMapper scenicReviewsMapper;

    private final Executor asyncExecutor;

    private static final String CACHE_KEY_PREFIX = "scenicSpots:list:";
    private static final String CACHE_KEY_BY_ID = "scenicSpots:id:";
    private static final String CACHE_KEY_BY_CONDITIONAL = "scenicSpots:conditional:";
    private static final long CACHE_EXPIRE_MINUTES = 10L;
    private static final long CACHE_EXPIRE_RANDOM_MINUTES = 5L;
    private static final Random random = new Random();

    public ScenicSpotsServiceImpl(ScenicSpotsMapper scenicSpotsMapper, RedisTemplate<String, Object> redisTemplate,
                                  ScenicReviewsMapper scenicReviewsMapper, @Qualifier("userBehaviorExecutor") Executor asyncExecutor) {
        this.scenicSpotsMapper = scenicSpotsMapper;
        this.redisTemplate = redisTemplate;
        this.scenicReviewsMapper = scenicReviewsMapper;
        this.asyncExecutor = asyncExecutor;
    }

    // ==================== 查询（缓存读取） ====================

    @Override
    public Page<ScenicSpots> getScenicSpotsList(Page<ScenicSpots> page) {
        String redisKey = CACHE_KEY_PREFIX + page.getCurrent() + ":size:" + page.getSize();
        Page<ScenicSpots> cachedPage = (Page<ScenicSpots>) redisTemplate.opsForValue().get(redisKey);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 查询数据库
        LambdaQueryWrapper<ScenicSpots> queryWrapper = new LambdaQueryWrapper<>();
        Page<ScenicSpots> dbPage = this.page(page, queryWrapper);

        // 存入缓存
        long expire = CACHE_EXPIRE_MINUTES + random.nextInt((int) CACHE_EXPIRE_RANDOM_MINUTES);
        redisTemplate.opsForValue().set(redisKey, dbPage, expire, TimeUnit.MINUTES);
        return dbPage;
    }

    @Override
    public Result addScenicSpots(ScenicSpots scenicSpots) {
        // 业务校验：名称唯一
        if (countByName(scenicSpots.getName()) > 0) {
            return Result.failure(400, "景点已存在");
        }

        // 第一次立即清除列表缓存
        clearListCache();

        // 插入数据库
        boolean saved = this.save(scenicSpots);
        if (!saved) {
            return Result.failure(500, "添加失败");
        }
        // 异步延迟第二次清除（500ms后）
        delayClearListCache(500);

        return Result.success("添加成功");
    }

    @Override
    public Result updateScenicSpots(ScenicSpots scenicSpots) {
        // 第一次清除缓存
        clearListCache();

        // 更新数据库
        int updated = scenicSpotsMapper.updateScenicSpots(scenicSpots);
        if (updated <= 0) {
            return Result.failure(500, "更新失败");
        }

        // 异步延迟第二次清除
        delayClearListCache(500);
        return Result.success("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteScenicSpots(Integer id) {
        // 第一次清除缓存
        clearListCache();

        // 删除数据库记录
        boolean removed = this.removeById(id);
        if (!removed) {
            return Result.failure(500, "删除失败");
        }

        // 异步延迟第二次清除
        delayClearListCache(500);


        return Result.success("删除成功");
    }

    @Override
    public ScenicSpots getScenicSpotsById(Integer id) {
        String redisKey = CACHE_KEY_BY_ID + id;
        ScenicSpots cachedScenicSpot = (ScenicSpots) redisTemplate.opsForValue().get(redisKey);
        if (cachedScenicSpot != null) {
            return cachedScenicSpot;
        }
        ScenicSpots dbScenicSpot = this.getById(id);
        long expire = CACHE_EXPIRE_MINUTES + random.nextInt((int) CACHE_EXPIRE_RANDOM_MINUTES);
        redisTemplate.opsForValue().set(redisKey, dbScenicSpot, expire, TimeUnit.MINUTES);
        return dbScenicSpot;
    }

    @Override
    public Page<ScenicSpots> getByConditional(Page<ScenicSpots> page, BigDecimal ticketPrice, String sortBy) {
        String redisKey = CACHE_KEY_BY_CONDITIONAL + page.getCurrent() + ":size:" + page.getSize() +
                (ticketPrice != null ? ":price_" + ticketPrice : "") +
                (sortBy != null ? ":sort_" + sortBy : "");
        Page<ScenicSpots> cachedPage = (Page<ScenicSpots>) redisTemplate.opsForValue().get(redisKey);
        if (cachedPage != null) {
            return cachedPage;
        }
        LambdaQueryWrapper<ScenicSpots> queryWrapper = new LambdaQueryWrapper<>();
        if (ticketPrice != null) {
            if (ticketPrice.compareTo(BigDecimal.ZERO) == 0) {
                queryWrapper.eq(ScenicSpots::getTicketPrice, BigDecimal.ZERO);
            } else {
                queryWrapper.gt(ScenicSpots::getTicketPrice, BigDecimal.ZERO);
            }
        }
        if ("score".equals(sortBy)) {
            queryWrapper.orderByDesc(ScenicSpots::getAvgScore);
        } else if ("views".equals(sortBy)) {
            queryWrapper.orderByDesc(ScenicSpots::getViewCount);
        } else if ("price_asc".equals(sortBy)) {
            queryWrapper.orderByAsc(ScenicSpots::getTicketPrice);
        } else if ("price_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(ScenicSpots::getTicketPrice);
        }
        Page<ScenicSpots> pageResult = this.page(page, queryWrapper);
        if (pageResult.getRecords() != null && !pageResult.getRecords().isEmpty()) {
            long expire = CACHE_EXPIRE_MINUTES + random.nextInt((int) CACHE_EXPIRE_RANDOM_MINUTES);
            redisTemplate.opsForValue().set(redisKey, pageResult, expire, TimeUnit.MINUTES);
        }
        return pageResult;
    }

    @Override
    public Page<ScenicSpots> getHotScenicSpots(Page<ScenicSpots> page) {
        String redisKey = CACHE_KEY_HOT_SCENIC_SPOTS + page.getCurrent() + ":size:" + page.getSize();
        Page<ScenicSpots> cachedPage = (Page<ScenicSpots>) redisTemplate.opsForValue().get(redisKey);
        if (cachedPage != null) {
            return cachedPage;
        }
        LambdaQueryWrapper<ScenicSpots> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ScenicSpots::getViewCount);
        Page<ScenicSpots> pageResult = this.page(page, queryWrapper);
        if (pageResult.getRecords() != null && !pageResult.getRecords().isEmpty()) {
            long expire = CACHE_EXPIRE_MINUTES + random.nextInt((int) CACHE_EXPIRE_RANDOM_MINUTES);
            redisTemplate.opsForValue().set(redisKey, pageResult, expire, TimeUnit.MINUTES);
            return pageResult;
        }
        return pageResult;
    }


    /**
     * 立即清除所有景点列表缓存（使用 SCAN 游标边扫描边删除，避免阻塞和内存溢出）
     */
    private void clearListCache() {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                int totalCount = 0;

                // 清除普通列表缓存
                ScanOptions listOptions = ScanOptions.scanOptions()
                        .match(CACHE_KEY_PREFIX + "*")
                        .count(100)
                        .build();
                Cursor<byte[]> listCursor = connection.scan(listOptions);
                int listCount = 0;
                while (listCursor.hasNext()) {
                    byte[] keyBytes = listCursor.next();
                    connection.del(keyBytes);
                    listCount++;
                }
                totalCount += listCount;

                // 清除条件查询缓存
                ScanOptions conditionalOptions = ScanOptions.scanOptions()
                        .match(CACHE_KEY_BY_CONDITIONAL + "*")
                        .count(100)
                        .build();
                Cursor<byte[]> conditionalCursor = connection.scan(conditionalOptions);
                int conditionalCount = 0;
                while (conditionalCursor.hasNext()) {
                    byte[] keyBytes = conditionalCursor.next();
                    connection.del(keyBytes);
                    conditionalCount++;
                }
                totalCount += conditionalCount;

                if (totalCount > 0) {
                    log.info("立即清除 {} 个景点缓存（普通列表:{}，条件查询:{}）",
                            totalCount, listCount, conditionalCount);
                } else {
                    log.debug("未找到需要清除的景点缓存");
                }
                return null;
            });
        } catch (Exception e) {
            log.error("清除景点缓存异常", e);
        }
    }

    /**
     * 异步延迟清除缓存
     *
     * @param delayMillis 延迟毫秒数
     */
    private void delayClearListCache(long delayMillis) {
        asyncExecutor.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(delayMillis);
                clearListCache();
                log.debug("延迟二次清除景点列表缓存完成，延迟 {} ms", delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("延迟清除列表缓存被中断", e);
            }
        });
    }

    // 辅助方法：根据名称计数
    private long countByName(String name) {
        return this.count(new LambdaQueryWrapper<ScenicSpots>()
                .eq(ScenicSpots::getName, name));
    }

    public void avgScore(Integer scenicId) {

        QueryWrapper<ScenicReviews> qw = new QueryWrapper<>();
        qw.select("AVG(rating) as avg_rating")
                .eq("scenic_id", scenicId);
        Map<String, Object> result = scenicReviewsMapper.selectMaps(qw).stream().findFirst().orElse(null);
        BigDecimal avg = result == null || result.get("avg_rating") == null
                ? BigDecimal.ZERO
                : new BigDecimal(result.get("avg_rating").toString()).setScale(1, RoundingMode.HALF_UP);
        ScenicSpots scenicSpots = new ScenicSpots();
        scenicSpots.setScenicId(scenicId);
        scenicSpots.setAvgScore(avg);
        this.updateById(scenicSpots);
    }

    @Override
    public Page<ScenicSpots> searchSpots(Page<ScenicSpots> page, String keyword) {
        return scenicSpotsMapper.searchSpots(page, keyword);
    }

    @Override
    public Page<ScenicSpots> getScenicSpotsListByLocaltion(Page<ScenicSpots> pageResult, String location) {
        LambdaQueryWrapper<ScenicSpots> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ScenicSpots::getLocation, location);
        return this.page(pageResult, queryWrapper);
    }

    @Override
    public Result updateViews(Integer id) {
        ScenicSpots scenicSpots = this.getById(id);
        if (scenicSpots == null)
            return Result.failure(400, "景点不存在");

        clearListCache();
        scenicSpots.setViewCount(scenicSpots.getViewCount() + 1);
        boolean b = this.updateById(scenicSpots);
        delayClearListCache(500);
        if (b)
            return Result.success("更新成功");
        return Result.failure(500, "更新失败");
    }
}
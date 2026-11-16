package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mpq.pojo.ScenicImages;
import com.mpq.mapper.ScenicImagesMapper;
import com.mpq.service.ScenicImagesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 景点图片表 服务实现类
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
@Service
public class ScenicImagesServiceImpl extends ServiceImpl<ScenicImagesMapper, ScenicImages> implements ScenicImagesService {

    @Override
    public List<ScenicImages> getByScenicId(Integer scenicId) {
        LambdaQueryWrapper<ScenicImages> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScenicImages::getScenicId, scenicId);
        return list(queryWrapper);

    }
}

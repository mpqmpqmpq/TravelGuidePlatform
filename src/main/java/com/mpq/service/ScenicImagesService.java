package com.mpq.service;

import com.mpq.pojo.ScenicImages;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 景点图片表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
public interface ScenicImagesService extends IService<ScenicImages> {

    List<ScenicImages> getByScenicId(Integer scenicId);
}

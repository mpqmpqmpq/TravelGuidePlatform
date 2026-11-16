package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.UserScenicFavorites;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户收藏景点表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
public interface UserScenicFavoritesService extends IService<UserScenicFavorites> {

    Result add(UserScenicFavorites userScenicFavorites);

    Result delete(UserScenicFavorites userScenicFavor);


    Page<UserScenicFavorites> getByUserId(Page<UserScenicFavorites> page, Integer userId);
}

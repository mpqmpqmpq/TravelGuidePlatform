package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.UserFollow;

public interface UserFollowService extends IService<UserFollow> {
    boolean addUserFollow(Integer userId, Integer followingId);

    boolean deleteUserFollow(Integer userId, Integer id);

    Page<UserFollow> getUserFollowList(Page<UserFollow> page, Integer userId);

    boolean isFollowing(Integer userId, Integer id);

    Page<UserFollow> getFollowerList(Page<UserFollow> page, Integer userId);
}

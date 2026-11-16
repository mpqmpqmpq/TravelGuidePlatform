package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.UserFollowMapper;
import com.mpq.pojo.User;
import com.mpq.pojo.UserFollow;
import com.mpq.service.UserFollowService;
import com.mpq.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserService userService;

    @Override
    @Transactional
    public boolean addUserFollow(Integer userId, Integer followingId) {
        User user = userService.getUserById(userId);
        user.setUserId(userId);
        user.setFollowerCount(user.getFollowerCount() + 1);
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(userId);
        userFollow.setFollowingId(followingId);
        userFollow.setCreatedAt(LocalDateTime.now());
        log.info("用户：{}", user);
        userService.updateUserById(user);
        User userFollowerById = userService.getUserById(followingId);
        userFollowerById.setUserId(followingId);
        userFollowerById.setFollowingCount(userFollowerById.getFollowerCount() + 1);
        userService.updateUserById(userFollowerById);

        return this.save(userFollow);
    }

    @Override
    @Transactional
    public boolean deleteUserFollow(Integer userId, Integer id) {
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowingId, id);
        UserFollow userFollow = userFollowMapper.selectOne(queryWrapper);
        if (userFollow == null) {
            return false;
        }
        User userById = userService.getUserById(userId);
        userById.setUserId(userId);
        if (userById.getFollowerCount() > 0) {
            userById.setFollowerCount(userById.getFollowerCount() - 1);
        }
        userService.updateUserById(userById);
//        被取消关注的

        User userById1 = userService.getUserById(userFollow.getFollowingId());
        if (userById1.getFollowingCount() > 0) {
            userById1.setUserId(userFollow.getFollowingId());
            userById1.setFollowingCount(userById1.getFollowingCount() - 1);
            userService.updateUserById(userById1);
        }
        return this.removeById(userFollow.getFollowId());
    }

    @Override
    public Page<UserFollow> getUserFollowList(Page<UserFollow> page, Integer userId) {
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId);
        return this.page(page, queryWrapper);

    }

    @Override
    public boolean isFollowing(Integer userId, Integer id) {
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowerId, userId);
        queryWrapper.eq(UserFollow::getFollowingId, id);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Page<UserFollow> getFollowerList(Page<UserFollow> page, Integer userId) {
        LambdaQueryWrapper<UserFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFollow::getFollowingId, userId);
        return this.page(page, queryWrapper);
    }
}

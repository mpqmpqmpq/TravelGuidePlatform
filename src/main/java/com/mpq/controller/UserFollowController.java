package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.UserFollow;
import com.mpq.service.UserFollowService;
import com.mpq.utils.NotificationHelper;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/userFollow")
@RequiredArgsConstructor
@Slf4j
public class UserFollowController {

    private final UserFollowService userFollowService;
    private final NotificationHelper notificationHelper;

    @PostMapping("/addUserFollow")
    public Result addUserFollow(@RequestBody Integer followingId) {
        Integer userId = UserContextHolder.getUserId();
        boolean addUserFollow = userFollowService.addUserFollow(userId, followingId);
        if (addUserFollow) {
            notificationHelper.sendNotification(userId, followingId, 2, null, 1, "关注了你");
            return Result.success("已关注", 200);
        }
        return Result.failure(500, "关注失败");
    }

    @DeleteMapping("/deleteUserFollow/{id}")
    public Result deleteUserFollowById(@PathVariable Integer id) {
        Integer userId = UserContextHolder.getUserId();
        boolean deleteUserFollow = userFollowService.deleteUserFollow(userId, id);
        if (deleteUserFollow) {
            return Result.success("已取消关注", 200);
        }
        return Result.failure(500, "取消关注失败");
    }

    @GetMapping("/getUserFollow")
    public Result getUserFollowList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Integer userId = UserContextHolder.getUserId();
        Page<UserFollow> page = new Page<>(pageNum, pageSize);
        Page<UserFollow> userFollowList = userFollowService.getUserFollowList(page, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", userFollowList.getRecords());
        result.put("total", userFollowList.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("current", userFollowList.getCurrent());
        return Result.success("获取关注列表成功", result);
    }

    @GetMapping("/isFollowing/{id}")
    public Result isFollowing(@PathVariable Integer id) {
        Integer userId = UserContextHolder.getUserId();
        boolean following = userFollowService.isFollowing(userId, id);
        return following ? Result.success("已关注", 200) : Result.success("未关注", 200);

    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/getFollower")
    public Result getFollowerList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        Integer userId = UserContextHolder.getUserId();
        Page<UserFollow> page = new Page<>(pageNum, pageSize);
        Page<UserFollow> followerList = userFollowService.getFollowerList(page, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", followerList.getRecords());
        result.put("total", followerList.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("current", followerList.getCurrent());
        return Result.success("获取关注列表成功", result);
    }

}

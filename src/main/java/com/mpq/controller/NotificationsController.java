package com.mpq.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.vo.NotificationVO;
import com.mpq.service.NotificationsService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationsService notificationsService;
    @GetMapping("/list")
    public Result list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer actionType
    ) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        Page<NotificationVO> page = new Page<>(pageNum, size);
        return notificationsService.getList(userId, page, actionType);
    }
    @PutMapping("/{id}/read")
    public Result markAsRead(@PathVariable Long id) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return notificationsService.markAsRead(id, userId);
    }
    @PutMapping("/{id}/unread")
    public Result markAsUnread(@PathVariable Long id) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return notificationsService.markAsUnread(id, userId);
    }
    @PutMapping("/read-all")
    public Result markAllAsRead() {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return notificationsService.markAllAsRead(userId);
    }
    @DeleteMapping("/{id}")
    public Result deleteNotification(@PathVariable Long id) {
        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return notificationsService.deleteNotification(id, userId);
    }
}
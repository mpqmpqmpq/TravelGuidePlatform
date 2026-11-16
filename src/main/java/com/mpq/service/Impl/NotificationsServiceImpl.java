package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.NotificationsMapper;
import com.mpq.pojo.Notification;
import com.mpq.pojo.vo.NotificationVO;
import com.mpq.service.NotificationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationsServiceImpl extends ServiceImpl<NotificationsMapper, Notification> implements NotificationsService {
    private final NotificationsMapper notificationsMapper;

    @Override
    public Result getList(Integer userId, Page<NotificationVO> page, Integer actionType) {
        Page<NotificationVO> notificationVOPage = notificationsMapper.selectNotificationPage(page, userId, actionType);
        Map<String, Object> result = new HashMap<>();
        result.put("list", notificationVOPage.getRecords());
        result.put("total", notificationVOPage.getTotal());
        result.put("pages", notificationVOPage.getPages());
        result.put("current", notificationVOPage.getCurrent());
        result.put("size", notificationVOPage.getSize());

        return Result.success("获取成功", result);
    }

    @Override
    public Result markAsRead(Long notificationId, Integer userId) {
        Notification notification = notificationsMapper.selectById(notificationId);
        if (notification == null) {
            return Result.failure(400, "通知不存在");
        }
        if (!notification.getRecipientId().equals(userId)) {
            return Result.failure(403, "无权限操作");
        }
        notification.setIsRead(true);
        notificationsMapper.updateById(notification);
        return Result.success("标记已读成功");
    }

    @Override
    public Result markAsUnread(Long notificationId, Integer userId) {
        Notification notification = notificationsMapper.selectById(notificationId);
        if (notification == null) {
            return Result.failure(400, "通知不存在");
        }
        if (!notification.getRecipientId().equals(userId)) {
            return Result.failure(403, "无权限操作");
        }
        notification.setIsRead(false);
        notificationsMapper.updateById(notification);
        return Result.success("标记未读成功");
    }

    @Override
    @Transactional
    public Result markAllAsRead(Integer userId) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, false);
        Notification notification = new Notification();
        notification.setIsRead(true);
        notificationsMapper.update(notification, queryWrapper);
        return Result.success("全部标为已读成功");
    }

    @Override
    public Result deleteNotification(Long notificationId, Integer userId) {
        Notification notification = notificationsMapper.selectById(notificationId);
        if (notification == null) {
            return Result.failure(400, "通知不存在");
        }
        if (!notification.getRecipientId().equals(userId)) {
            return Result.failure(403, "无权限操作");
        }
        notificationsMapper.deleteById(notificationId);
        return Result.success("删除成功");
    }
}
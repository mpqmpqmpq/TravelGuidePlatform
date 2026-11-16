package com.mpq.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.Notification;
import com.mpq.pojo.vo.NotificationVO;

public interface NotificationsService extends IService<Notification> {
    Result getList(Integer userId, Page<NotificationVO> page, Integer actionType);
    Result markAsRead(Long notificationId, Integer userId);
    Result markAsUnread(Long notificationId, Integer userId);
    Result markAllAsRead(Integer userId);
    Result deleteNotification(Long notificationId, Integer userId);
}
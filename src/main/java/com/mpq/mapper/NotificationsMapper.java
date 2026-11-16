package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.Notification;
import com.mpq.pojo.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationsMapper extends BaseMapper<Notification> {
    Page<NotificationVO> selectNotificationPage(Page<NotificationVO> page, @Param("userId") Integer userId, @Param("actionType") Integer actionType);
}

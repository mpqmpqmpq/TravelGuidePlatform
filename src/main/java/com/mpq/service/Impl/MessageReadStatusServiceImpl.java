package com.mpq.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.MessageReadStatusMapper;
import com.mpq.pojo.MessageReadStatus;
import com.mpq.service.MessageReadStatusService;
import org.springframework.stereotype.Service;

@Service
public class MessageReadStatusServiceImpl extends ServiceImpl<MessageReadStatusMapper, MessageReadStatus> implements MessageReadStatusService {
}

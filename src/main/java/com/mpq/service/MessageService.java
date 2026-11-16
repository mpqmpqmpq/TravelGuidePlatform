package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Message;

import java.util.List;

public interface MessageService extends IService<Message>{

    List<Message> getMessages(Integer conversationId, Integer userId, Integer pageNum, Integer size);
    void updateMessageCacheOnSend(Message message);

    void saveAllMessagesFromMQ();
}

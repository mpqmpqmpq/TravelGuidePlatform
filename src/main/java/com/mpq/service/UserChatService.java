package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Conversation;
import com.mpq.pojo.Message;
import com.mpq.pojo.vo.ConversationWithUserVo;

import java.util.List;

public interface UserChatService extends IService<Conversation> {
    Message sendMessage(Integer senderId, Integer receiverId, String content);


    Conversation getORCreateConversation(Integer user1Id, Integer user2Id);
    List<Conversation> getUserConversations(Integer userId);


    List<ConversationWithUserVo> selectConversationsWithUsers(Integer userId);
}

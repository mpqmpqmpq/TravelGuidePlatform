package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("select message_id, conversation_id, sender_id, receiver_id, content, status, created_at from messages where conversation_id = #{conversationId}")
    List<Message> findMessagesByConversationId(Integer conversationId);


   int addBatchMessages(List<Message> messages);
}

package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.Conversation;
import com.mpq.pojo.vo.ConversationWithUserVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    @Select("select conversation_id, user1_id, user2_id, last_message_id, updated_at, unread_count1, unread_count2 from conversations where user1_id = #{user1Id} and user2_id = #{user2Id} ")
    Conversation findByUserIds(Integer user1Id, Integer user2Id);

    List<ConversationWithUserVo> selectConversationsWithUsers(Integer userId);
}

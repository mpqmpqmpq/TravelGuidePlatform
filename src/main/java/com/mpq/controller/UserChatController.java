package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.pojo.Message;
import com.mpq.pojo.vo.ConversationWithUserVo;
import com.mpq.service.MessageService;
import com.mpq.service.UserChatService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user/chat")
@Slf4j
public class UserChatController {
    private final UserChatService userChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    /**
     * 发送消息
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return
     */
    @PostMapping("/message")
    public Result<Message> sendMessage(
            @RequestParam Integer receiverId,
            @RequestParam String content
            ) {
        Integer senderId = UserContextHolder.getUserId();
        Message message = userChatService.sendMessage(senderId,receiverId,content);

        log.info("准备发送消息给接收者: " + message.getReceiverId());
        messagingTemplate.convertAndSend("/queue/private/"+message.getReceiverId(),message);
        return  Result.success("200",message);
    }
    /**
     * 获取消息列表
     * @param conversationId 会话ID
     * @param pageNum 偏移量，默认0
     * @param size 限制数量，默认100
     * @return
     */
    @GetMapping("/conversations/{conversationId}/message")
    public Result<List<Message>> getMessages(@PathVariable Integer conversationId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "100") Integer size) {
        Integer userId = UserContextHolder.getUserId();
        List<Message> messages = messageService.getMessages(conversationId, userId, pageNum, size);
        return Result.success("200", messages);
    }

    /**
     * 获取会话列表
     * @return
     */
    @GetMapping("/conversations")
    public Result<List<ConversationWithUserVo>> getConversations() {
        Integer userId = UserContextHolder.getUserId();
        List<ConversationWithUserVo> conversations = userChatService.selectConversationsWithUsers(userId);
        return Result.success("200",conversations);
    }

}

package com.mpq.pojo;

import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class PendingUserBehaviorLog {
    final Channel channel;
    final long deliveryTag;
    final UserBehaviorLog userBehaviorLog;

}

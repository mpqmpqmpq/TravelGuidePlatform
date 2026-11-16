package com.mpq.pojo;

import com.rabbitmq.client.Channel;
import lombok.Data;


@Data
public  class PendingMessage {
        final Message message;
        final Channel channel;
        final long deliveryTag;

        public PendingMessage(Message message, Channel channel, long deliveryTag) {
            this.message = message;
            this.channel = channel;
            this.deliveryTag = deliveryTag;
        }
    }
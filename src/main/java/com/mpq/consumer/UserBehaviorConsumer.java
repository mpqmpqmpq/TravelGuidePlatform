package com.mpq.consumer;


import com.mpq.config.RabbitMQConfig;
import com.mpq.pojo.PendingUserBehaviorLog;
import com.mpq.pojo.UserBehaviorLog;
import com.mpq.service.UserBehaviorLogService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorConsumer {
    private final UserBehaviorLogService userBehaviorLogService;


    private final ConcurrentLinkedQueue<PendingUserBehaviorLog> buffer = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 20;
    @RabbitListener(queues = RabbitMQConfig.BEHAVIOR_LOG_QUEUE,
    containerFactory = "rabbitListenerContainerFactory")
    public void behaviorLogs(UserBehaviorLog behaviorLog, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        buffer.offer(new PendingUserBehaviorLog(channel,deliveryTag,behaviorLog));
        if (buffer.size()>=BATCH_SIZE){
            userBehaviorLogService.addBatchLog(buffer);
        }
    }

//    没五秒执行一次，首次执行前等待10秒
    @Scheduled(fixedDelay = 5000,initialDelay = 10000)
    public void batchLogsForTime(){
        if (buffer.isEmpty()) return;

        userBehaviorLogService.addBatchLog(buffer);
        log.info("执行日志批量存储成功");
    }



    @PreDestroy
    public  void destroy(){

        while (!buffer.isEmpty()){

            userBehaviorLogService.addBatchLog(buffer);

        }
    }

}

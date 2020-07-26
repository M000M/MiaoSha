package com.itheima.leyou.queue;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    @Bean
    public Queue queueOrder(){
        return new Queue("order_queue", true); //初始化时设定队列的名字
    }
}

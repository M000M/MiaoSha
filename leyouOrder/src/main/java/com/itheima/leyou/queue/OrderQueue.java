package com.itheima.leyou.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderQueue {

    @RabbitListener(queues = "order_queue")
    public void insertOrder(String msg) {
        //1、监听消息

        //2、执行insertOrder方法

        //3、如果失败输出失败信息

        //4、如果失败输入成功
    }
}

package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public Map<String, Object> createOrder(String sku_id, String user_id) {
        Map<String, Object> resultMap = new HashMap<>();
        //1、判断传入的参数
        if(sku_id == null || sku_id.equals("")) {
            resultMap.put("result", false);
            resultMap.put("msg", "前端传入参数有误");
            return resultMap;
        }

        String order_id = String.valueOf(System.currentTimeMillis());

        //2、取redis政策
        String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_" + sku_id);

        if (policy != null && !policy.equals("")) {
            //3、判断时间，开始时间 <= 当前时间并且当前时间 <= 结束时间
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
            Map<String, Object> policyInfo = JSONObject.parseObject(policy, Map.class);

            try {
                Date begin_time = simpleDateFormat.parse(policyInfo.get("begin_time").toString());
                Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
                Date now_time = simpleDateFormat.parse(now);

                if (begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()) {
                    long limitQuanty = Long.parseLong(policyInfo.get("quanty").toString());

                    //4、redis计数器
                    if (stringRedisTemplate.opsForValue().increment("SKU_QUANTY_" + sku_id, 1) <= limitQuanty) {
                        //5、通过计数器，写入订单队列，并且写入redis
                        Map<String, Object> orderInfo = new HashMap<>();
                        String sku = stringRedisTemplate.opsForValue().get("SKU_" + sku_id);
                        Map<String, Object> skuMap = JSONObject.parseObject(sku, Map.class);

                        orderInfo.put("order_id", order_id);
                        orderInfo.put("total_fee", skuMap.get("price"));
                        orderInfo.put("actual_fee", policyInfo.get("price"));
                        orderInfo.put("post_fee", 0);
                        orderInfo.put("payment_type", 0);
                        orderInfo.put("user_id", user_id);
                        orderInfo.put("status", 1);
                        orderInfo.put("create_time", now);

                        orderInfo.put("sku_id", skuMap.get("sku_id"));
                        orderInfo.put("num", 1);
                        orderInfo.put("title", skuMap.get("title"));
                        orderInfo.put("own_spec", skuMap.get("own_spec"));
                        orderInfo.put("price", policyInfo.get("price"));
                        orderInfo.put("image", skuMap.get("images"));

                        try {
                            String order = JSON.toJSONString(orderInfo);
                            amqpTemplate.convertAndSend("order_queue", order);
                            stringRedisTemplate.opsForValue().set("ORDER_" + order_id, order);
                        } catch (Exception e) {
                            resultMap.put("result", false);
                            resultMap.put("msg", "队列写入失败!");
                            return resultMap;
                        }

                    } else {
                        //6、没有通过计数器，提示商品已经售完
                        resultMap.put("result", false);
                        resultMap.put("msg", "商品已经售完，踢回去的3亿9");
                        return resultMap;
                    }
                } else {
                    //7、时间判断以外，提示活动已经过期
                    resultMap.put("result", false);
                    resultMap.put("msg", "活动已经过期");
                    return resultMap;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            //8、没有取出政策，提示活动已经过期
            resultMap.put("result", false);
            resultMap.put("msg", "活动已经过期");
            return resultMap;
        }

        //9、返回正常信息，包含order_id
        resultMap.put("order_id", order_id);
        resultMap.put("result", true);
        resultMap.put("msg", "秒杀成功");
        return resultMap;
    }
}

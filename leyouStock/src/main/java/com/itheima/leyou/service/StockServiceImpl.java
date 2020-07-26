package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IStockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class StockServiceImpl implements IStockService {

    @Autowired
    private IStockDao iStockDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> getStockList() {
        Map<String, Object> resultMap = new HashMap<>();

        //1、取自iStickDao的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStockList();

        //2、如果没有取出来，返回一个错误信息
        if (list == null || list.size() == 0){
            resultMap.put("result", false);
            resultMap.put("msg", "您没有取出商品信息");
            resultMap.put("sku_list", null);
            return resultMap;
        }

        //3、取redis政策
        resultMap = getLimitPolicy(list);

        //4、返回正常信息
        resultMap.put("sku_list", list);
        return resultMap;
    }

    public Map<String, Object> getStock(String sku_id){
        Map<String, Object> resultMap = new HashMap<>();
        //1、判断传入的参数
        if(sku_id == null || sku_id.equals("")) {
            resultMap.put("result", false);
            resultMap.put("msg", "您传入的参数有误");
            resultMap.put("sku_list", null);
            return resultMap;
        }
        //2、取自iStockDao的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStock(sku_id);
        //3、如果没有取出来，返回一个错误
        if (list == null || list.size() == 0){
            resultMap.put("result", false);
            resultMap.put("msg", "您没有取出商品信息");
            resultMap.put("sku", null);
            return resultMap;
        }

        //4、从Redis中取政策
        resultMap = getLimitPolicy(list); //Java传递的是引用值，该函数会将list的每个元素取出进行处理，在各自的Map中添加对应的元素
                                          //再返回本函数中将list加入到resultMap中

        //5、返回正常信息
        resultMap.put("sku", list);
        return resultMap;
    }

    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo) {
        Map<String, Object> resultMap = new HashMap<>();
        //1、验证传入的参数
        if(policyInfo == null) {
            resultMap.put("result", false);
            resultMap.put("msg", "您传入的参数有误");
            return resultMap;
        }
        //2、取自iStockDao的方法
        boolean result = iStockDao.insertLimitPolicy(policyInfo);

        //3、如果没有执行成功，返回错误信息
        if(!result){
            resultMap.put("result", false);
            resultMap.put("msg", "数据库写入政策失败");
            return resultMap;
        }

        //4、写入Redis, StringRedisTemplate
        //4.1、取名  key: LIMIT_POLICY_{sku_id}, value: policyInfo ---> String
        //4.2、redis有效期, 有效期：结束时间减去当前时间
        long diff = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
        try {
            Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
            Date now_time = simpleDateFormat.parse(now);
            diff = (end_time.getTime() - now_time.getTime()) / 1000;
            if(diff < 0){
                resultMap.put("result", false);
                resultMap.put("msg", "结束时间不能小于当前时间");
                return resultMap;
            }
        } catch (ParseException e){
            e.printStackTrace();
        }

        String policy = JSON.toJSONString(policyInfo);
        stringRedisTemplate.opsForValue().set("LIMIT_POLICY_" + policyInfo.get("sku_id").toString(), policy, diff, TimeUnit.SECONDS);

        //商品存入Redis, 上面存的是政策
        ArrayList<Map<String, Object>> list = iStockDao.getStock(policyInfo.get("sku_id").toString());
        String sku = JSON.toJSONString(list.get(0));
        stringRedisTemplate.opsForValue().set("SKU_" + policyInfo.get("sku_id").toString(), sku, diff, TimeUnit.SECONDS);

        //5、返回正常信息
        resultMap.put("result", true);
        resultMap.put("msg", "政策写入完毕");
        return resultMap;
    }

    private Map<String, Object> getLimitPolicy(ArrayList<Map<String, Object>> list){
        Map<String, Object> resultMap = new HashMap<>();
        for(Map<String, Object> skuMap: list){
            //3.1、取政策，如果取到政策，才给商品赋值
            //3.2、开始时间 <= 当前时间，并且当前时间 <= 结束时间
            String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_" + skuMap.get("sku_id").toString());
            if (policy != null && !policy.equals("")){

                Map<String, Object> policyInfo = JSONObject.parseObject(policy, Map.class);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
                try{
                    Date begin_time = simpleDateFormat.parse(policyInfo.get("begin_time").toString());
                    Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
                    Date now_time = simpleDateFormat.parse(now);
                    if(begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()){
                        //赋值：limitPrice limitQuanty limitBeginTime limitEndTime nowTime
                        skuMap.put("limitPrice", policyInfo.get("price"));
                        skuMap.put("limitQuanty", policyInfo.get("quanty"));
                        skuMap.put("limitBeginTime", policyInfo.get("begin_time"));
                        skuMap.put("limitEndTime", policyInfo.get("end_time"));
                        skuMap.put("nowTime", now);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }
}

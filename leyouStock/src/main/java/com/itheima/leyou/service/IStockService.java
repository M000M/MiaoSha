package com.itheima.leyou.service;

import java.util.Map;

public interface IStockService {

    Map<String, Object> getStockList();

    Map<String, Object> getStock(String sku_id);

    Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo);
}

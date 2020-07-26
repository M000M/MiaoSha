package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IStockDao {

    ArrayList<Map<String, Object>> getStockList();

    ArrayList<Map<String, Object>> getStock(String sku_id);

    boolean insertLimitPolicy(Map<String, Object> policyInfo);
}

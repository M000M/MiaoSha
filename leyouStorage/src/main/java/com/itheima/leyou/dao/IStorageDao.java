package com.itheima.leyou.dao;

import java.util.Map;

public interface IStorageDao {

    Map<String, Object> insertStorage(String sku_id, double in_quanty, double out_quanty);
}

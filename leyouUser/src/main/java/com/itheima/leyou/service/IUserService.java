package com.itheima.leyou.service;

import java.util.Map;

public interface IUserService {

    Map<String, Object> getUser(String username, String password);

    Map<String, Object> insertUser(String username, String password);
}

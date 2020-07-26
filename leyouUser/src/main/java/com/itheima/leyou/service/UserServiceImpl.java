package com.itheima.leyou.service;

import com.itheima.leyou.dao.IUserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private IUserDao userDao;

    public Map<String, Object> getUser(String username, String password) {
        Map<String, Object> resultMap = new HashMap<>();

        if(username == null || username.equals("")) {
            resultMap.put("result", false);
            resultMap.put("msg", "用户名不能为空");
            return resultMap;
        }

        ArrayList<Map<String, Object>> list = userDao.getUser(username, password);
        if(list == null || list.size() == 0) {
            resultMap.put("result", false);
            resultMap.put("msg", "没有找到会员信息");
            return resultMap;
        }

        resultMap = list.get(0);
        resultMap.put("result", true);
        resultMap.put("msg", "查找会员信息成功");
        return resultMap;
    }

    public Map<String, Object> insertUser(String username, String password) {
        Map<String, Object> resultMap = new HashMap<>();
        if (username == null || username.equals("")) {
            resultMap.put("result", false);
            resultMap.put("msg", "用户名不能为空");
            return resultMap;
        }

        int user_id = userDao.insertUser(username, password);

        if(user_id < 0) {
            resultMap.put("result", false);
            resultMap.put("msg", "数据库没有更新成功");
            return resultMap;
        }

        resultMap.put("user_id", user_id);
        resultMap.put("username", username);
        resultMap.put("phone", username);
        resultMap.put("password", password);
        resultMap.put("result", true);
        resultMap.put("msg", "创建会员成功");
        return resultMap;
    }
}

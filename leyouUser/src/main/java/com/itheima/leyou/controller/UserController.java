package com.itheima.leyou.controller;

import com.alibaba.fastjson.JSON;
import com.itheima.leyou.service.IUserService;
import org.aspectj.lang.annotation.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private IUserService userService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Map<String, Object> longin(String username, String password, HttpServletRequest httpServletRequest) {
        Map<String, Object> userMap = userService.getUser(username, password);

        if(!(Boolean)userMap.get("result")) {
            userMap = userService.insertUser(username, password);

            if((Boolean)userMap.get("result")) {
                return userMap;
            }
        }

        HttpSession httpSession = httpServletRequest.getSession();
        String user = JSON.toJSONString(userMap);
        httpSession.setAttribute("user", user);

        return userMap;
    }
}

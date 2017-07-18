package com.opslab.core.action;

import com.opslab.core.model.UserInfo;
import com.opslab.core.service.UserInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 用户相关的action
 */

@Controller
@RequestMapping("/user")
public class UserInfoAction {
    @Resource(name="userInfoService")
    private UserInfoService service;

    @ResponseBody
    @RequestMapping("info/{userId}")
    public UserInfo userInof(@PathVariable("userId")String userId){
        return service.queryUserInfoById(userId);
    }
}

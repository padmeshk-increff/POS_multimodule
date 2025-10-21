package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.UserFlow;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.utils.NormalizeUtil;
import com.increff.pos.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserDto {

    @Autowired
    private UserFlow userFlow;

    public Map<String,String> add(UserForm userForm) throws ApiException{
        NormalizeUtil.normalize(userForm);
        User user = UserUtil.convert(userForm);

        userFlow.add(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User signed up successfully");
        return response;
    }
}

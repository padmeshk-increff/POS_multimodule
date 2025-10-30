package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.UserFlow;
import com.increff.pos.helper.UserMapper;
import com.increff.pos.model.form.UserForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserDto extends AbstractDto{

    @Autowired
    private UserFlow userFlow;

    @Autowired
    private UserMapper userMapper;

    public Map<String,String> add(UserForm userForm) throws ApiException{
        normalize(userForm, Arrays.asList("password"));
        User user = userMapper.convert(userForm);

        userFlow.add(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User signed up successfully");
        return response;
    }
}

package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.SessionFlow;
import com.increff.pos.helper.UserMapper;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.result.LoginResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SessionDto extends AbstractDto{

    @Autowired
    private SessionFlow sessionFlow;

    @Autowired
    private UserMapper userMapper;

    public LoginData login(LoginForm loginForm) throws ApiException{
        normalize(loginForm, Arrays.asList("password"));

        User user = userMapper.convert(loginForm);

        LoginResult loginResult = sessionFlow.login(user);

        return userMapper.convert(loginResult);
    }
}

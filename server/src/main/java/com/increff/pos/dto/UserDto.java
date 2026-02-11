package com.increff.pos.dto;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.flow.UserFlow;
import com.increff.pos.helper.UserMapper;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserDto extends AbstractDto{

    @Autowired
    private UserApi userApi;

    @Autowired
    private UserFlow userFlow;

    @Autowired
    private UserMapper userMapper;

    public Map<String,String> add(UserForm userForm) throws ApiException{
        ValidationUtil.validate(userForm);
        normalize(userForm, Arrays.asList("password"));

        User user = userMapper.convert(userForm);

        userFlow.add(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User signed up successfully");
        return response;
    }

    public AuthUserData getSelf(Authentication authentication) throws ApiException {
        if (authentication == null) {
            throw new ApiException("User is not authenticated");
        }

        String email = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();

        User user = userApi.getCheckByEmail(email);

        return new AuthUserData(user.getId(), user.getEmail(), user.getRole().toString());
    }

    public AuthUserData handleLoginSuccess(Authentication authentication) throws ApiException {
        if (authentication == null) {
            throw new ApiException("Authentication object was null during login success.");
        }
        String email = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();

        User user = userApi.getCheckByEmail(email);

        return userMapper.toAuthUserData(user);
    }
}

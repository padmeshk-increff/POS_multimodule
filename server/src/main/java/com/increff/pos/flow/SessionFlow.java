package com.increff.pos.flow;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.model.result.LoginResult;
import com.increff.pos.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class SessionFlow {

    private static final Logger log = LoggerFactory.getLogger(SessionFlow.class);
    @Autowired
    private UserApi userApi;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResult login(User user) throws ApiException {

        User existingUser = userApi.getCheckByEmail(user.getEmail());

        Boolean passwordMatch = passwordEncoder.matches(user.getPassword(), existingUser.getPassword());
        if(!passwordMatch){
            throw new ApiException("Invalid credentials provided. Please check your email and password.");
        }

        String token = jwtUtil.generateToken(existingUser);

        LoginResult loginResult = new LoginResult();
        loginResult.setUser(existingUser);
        loginResult.setToken(token);
        return loginResult;
    }
}

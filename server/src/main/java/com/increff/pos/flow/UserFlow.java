package com.increff.pos.flow;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.model.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class UserFlow {

    @Autowired
    private UserApi userApi;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${supervisor.emails}")
    private String supervisorEmails;

    public void add(User user) throws ApiException {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole(getRole(user.getEmail()));

        userApi.add(user);
    }

    private Role getRole(String email) {
        List<String> supervisorEmailList = Arrays.asList(supervisorEmails.split(","));
        if (supervisorEmailList.contains(email)) {
            return Role.SUPERVISOR;
        }
        return Role.OPERATOR;
    }
}

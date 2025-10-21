package com.increff.pos.flow;

import com.increff.pos.api.UserApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.User;
import com.increff.pos.model.enums.Role;
import com.increff.pos.model.form.UserForm;
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
        // 1. Hash the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Determine the role based on the properties file
        user.setRole(getRole(user.getEmail()));

        // 3. Pass the fully prepared entity to the API layer for saving
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

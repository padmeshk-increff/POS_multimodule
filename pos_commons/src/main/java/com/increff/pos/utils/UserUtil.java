package com.increff.pos.utils;

import com.increff.pos.entity.User;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.result.LoginResult;

public class UserUtil {

    public static User convert(UserForm userForm){
        User user = new User();
        user.setEmail(userForm.getEmail());
        user.setPassword(userForm.getPassword());
        return user;
    }

    public static User convert(LoginForm loginForm){
        User user = new User();
        user.setPassword(loginForm.getPassword());
        user.setEmail(loginForm.getEmail());
        return user;
    }

    public static LoginData convert(LoginResult loginResult){
        LoginData loginData = new LoginData();
        User user = loginResult.getUser();

        loginData.setEmail(user.getEmail());
        loginData.setRole(user.getRole());
        loginData.setToken(loginResult.getToken());
        return loginData;
    }
}

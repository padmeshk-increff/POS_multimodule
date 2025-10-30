package com.increff.pos.helper;

import com.increff.pos.entity.User;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.enums.Role;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.result.LoginResult;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-30T08:41:21+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User convert(UserForm userForm) {
        if ( userForm == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( userForm.getEmail() );
        user.setPassword( userForm.getPassword() );

        return user;
    }

    @Override
    public User convert(LoginForm loginForm) {
        if ( loginForm == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( loginForm.getEmail() );
        user.setPassword( loginForm.getPassword() );

        return user;
    }

    @Override
    public LoginData convert(LoginResult loginResult) {
        if ( loginResult == null ) {
            return null;
        }

        LoginData loginData = new LoginData();

        loginData.setEmail( loginResultUserEmail( loginResult ) );
        loginData.setRole( loginResultUserRole( loginResult ) );
        loginData.setToken( loginResult.getToken() );

        return loginData;
    }

    private String loginResultUserEmail(LoginResult loginResult) {
        if ( loginResult == null ) {
            return null;
        }
        User user = loginResult.getUser();
        if ( user == null ) {
            return null;
        }
        String email = user.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private Role loginResultUserRole(LoginResult loginResult) {
        if ( loginResult == null ) {
            return null;
        }
        User user = loginResult.getUser();
        if ( user == null ) {
            return null;
        }
        Role role = user.getRole();
        if ( role == null ) {
            return null;
        }
        return role;
    }
}

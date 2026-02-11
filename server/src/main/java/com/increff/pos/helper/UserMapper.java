package com.increff.pos.helper;

import com.increff.pos.entity.User;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.form.LoginForm;
import com.increff.pos.model.form.UserForm;
import com.increff.pos.model.result.LoginResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User convert(UserForm userForm);

    User convert(LoginForm loginForm);

    @Mapping(source="user.email", target = "email")
    @Mapping(source="user.role", target = "role")
    LoginData convert(LoginResult loginResult);

    AuthUserData toAuthUserData(User user);
}

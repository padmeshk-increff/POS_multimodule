package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.UserDto;
import com.increff.pos.model.form.UserForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserDto userDto;

    @RequestMapping(path = "/users/signup", method = RequestMethod.POST)
    public Map<String, String> signup(@RequestBody UserForm userForm) throws ApiException {
        return userDto.add(userForm);
    }
}

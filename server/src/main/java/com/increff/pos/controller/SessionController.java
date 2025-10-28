package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.SessionDto;
import com.increff.pos.model.data.LoginData;
import com.increff.pos.model.form.LoginForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class SessionController {

    @Autowired
    private SessionDto sessionDto;

    @RequestMapping(path = "/session/login", method = RequestMethod.POST)
    public LoginData login(@Valid @RequestBody LoginForm form) throws ApiException {
        return sessionDto.login(form);
    }
}

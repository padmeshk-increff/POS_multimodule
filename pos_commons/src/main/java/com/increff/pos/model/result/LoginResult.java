package com.increff.pos.model.result;

import com.increff.pos.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResult {
    private User user;
    private String token;
}

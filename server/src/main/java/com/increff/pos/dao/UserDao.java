package com.increff.pos.dao;

import com.increff.pos.entity.User;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

@Repository
public class UserDao extends AbstractDao<User>{

    private static final String SELECT_BY_EMAIL = "select u from User u where u.email = :email";

    public User selectByEmail(String email) {
        TypedQuery<User> query = getQuery(SELECT_BY_EMAIL);
        query.setParameter("email", email);
        return getFirstRowFromQuery(query);
    }
}

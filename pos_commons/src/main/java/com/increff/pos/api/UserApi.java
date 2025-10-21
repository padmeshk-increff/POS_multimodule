package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.UserDao;
import com.increff.pos.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = ApiException.class)
public class UserApi extends AbstractApi{

    @Autowired
    private UserDao userDao;

    public User login(User user) throws ApiException{
        User existingUser = getByEmail(user.getEmail());

        if(!user.getPassword().equals(existingUser.getPassword())){
            throw new ApiException("Invalid password");
        }

        return user;
    }

    public User add(User user) throws ApiException {
        checkNull(user,"User cannot be null");

        User existing = userDao.selectByEmail(user.getEmail());
        checkNotNull(existing,"User with given email already exists");

        user.setPassword(user.getPassword());

        userDao.insert(user);
        return user;
    }

    public User getByEmail(String email) throws ApiException {
        checkNull(email,"Email cannot be null");

        User user = userDao.selectByEmail(email);
        checkNull(user,"User with given email does not exist");

        return user;
    }

}

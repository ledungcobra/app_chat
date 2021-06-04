package com.appchat.server.service;

import com.appchat.server.context.SApplicationContext;
import com.appchat.server.dao.BaseDao;
import com.appchat.server.dao.UserDao;
import com.appchat.server.entities.User;

import java.util.concurrent.Future;

public class UserService extends BaseService
{

    public UserService()
    {
        super();
        this.dao = SApplicationContext.userDao;
    }

    public Future<User> findByUserName(String userName)
    {
        return service.submit(() -> {
            if (dao instanceof UserDao)
            {
                return ((UserDao) dao).findByUserName(userName);
            }
            return null;
        });
    }

}

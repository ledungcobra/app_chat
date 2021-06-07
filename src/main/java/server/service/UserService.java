package server.service;

import server.context.SApplicationContext;
import server.dao.UserDao;
import server.entities.User;

import java.util.concurrent.Future;

public class UserService extends BaseService<User, Long>
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

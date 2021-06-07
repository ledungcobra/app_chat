package server.dao;

import server.entities.User;
import org.hibernate.Session;

public class UserDao extends BaseDao<User, Long>
{
    public UserDao(Session session)
    {
        super(session);
    }

    public User findByUserName(String userName)
    {
        try
        {
            User user = (User) session.createQuery("FROM User u WHERE u.userName=:userName")
                    .setParameter("userName", userName).getSingleResult();
            return user;
        } catch (Exception e)
        {
            return null;
        }
    }
}

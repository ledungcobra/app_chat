package server.dao;

import lombok.val;
import org.hibernate.query.Query;
import server.entities.User;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

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

    public List<User> findByKeyword(String keyword)
    {
        try
        {
            Query query = session.createQuery("FROM User u where u.displayName like :keyword");
            query.setParameter("keyword", "%" + keyword + "%");
            return query.getResultList();
        } catch (Exception e)
        {
            return new ArrayList<>();
        }
    }
}

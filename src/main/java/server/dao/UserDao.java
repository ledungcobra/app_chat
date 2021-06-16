package server.dao;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import server.entities.User;

import java.util.ArrayList;
import java.util.List;

public class UserDao extends BaseDao<User, Long> {


    public UserDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public User findByUserName(String userName) {
        try {
            User user = (User) getCurrentSession()
                    .createQuery("FROM User u WHERE u.userName=:userName")
                    .setParameter("userName", userName).getSingleResult();
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    public List<User> findByKeyword(String keyword) {
        try {
            getCurrentSession().beginTransaction();
            Query query = getCurrentSession()
                    .createQuery("FROM User u where u.displayName like :k");
            query.setParameter("k", "%" + keyword + "%");
            List resultList = query.getResultList();
            getCurrentSession().close();
            return resultList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

package server.dao;

import org.hibernate.Session;
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
        Session session = null;
        try {
            session = openSession();

            User user = (User) session
                    .createQuery("FROM User u WHERE u.userName=:userName")
                    .setParameter("userName", userName).getSingleResult();
            return user;
        } catch (Exception e) {
            return null;
        } finally {
            session.close();
        }
    }

    public List<User> findByKeyword(String keyword) {
        Session session = null;
        try {
            session = openSession();
            Query query = session
                    .createQuery("FROM User u where u.displayName like :k");
            query.setParameter("k", "%" + keyword + "%");
            List resultList = query.getResultList();
            return resultList;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    public List<User> getFriends(Long id) {
        Session session = null;
        try {
            session = openSession();

            List<User> friends = session
                    .createQuery("SELECT DISTINCT f.partner FROM FriendShip f WHERE f.owner.id=:id", User.class)
                    .setParameter("id", id).getResultList();
            return friends;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }


}

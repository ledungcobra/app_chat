package server.dao;


import org.hibernate.SessionFactory;
import server.entities.BaseEntity;
import org.hibernate.Session;
import server.entities.User;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseDao<T extends BaseEntity, ID extends Serializable> {

    protected final SessionFactory sessionFactory;
    protected final Class<T> clazz;

    public BaseDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        clazz = ((Class) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    protected Session getCurrentSession() {
        return this.sessionFactory.getCurrentSession();
    }

    public T find(ID id) {
        T t = this.getCurrentSession().get(clazz, id);
        return t;
    }

    public void delete(T object) {
        this.getCurrentSession().delete(object);
    }

    public T insert(T object) {
        this.getCurrentSession().save(object);
        return object;
    }

    public void deleteById(ID id) {
        this.getCurrentSession().createQuery("DELETE " + clazz.getSimpleName() + "x WHERE x.id=:id")
                .setParameter("id", id)
                .executeUpdate();
        this.getCurrentSession().clear();
    }

    public List<T> findAll() {
        return this.getCurrentSession().createQuery("FROM " + clazz.getSimpleName()).getResultList();
    }

    public void update(T object) {
        this.getCurrentSession().update(object);
    }

    public List<User> getFriends(Long id) {
        try {
            List<User> friends =getCurrentSession()
                    .createQuery("SELECT DISTINCT f.partner FROM FriendShip f WHERE f.owner.id=:id", User.class)
                    .setParameter("id", id).getResultList();

            return friends;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

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
    protected Session currentSession;

    public BaseDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        clazz = ((Class) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    protected Session openSession() {
        this.currentSession = this.sessionFactory.openSession();
        return this.currentSession;
    }


    public T find(ID id) {
        Session s = this.openSession();
        T t = s.get(clazz, id);
        s.close();
        return t;
    }

    public void delete(T object) {

        Session s = this.openSession();
        s.beginTransaction();
        s.delete(object);
        s.getTransaction().commit();
        s.close();
    }

    public T insert(T object) {
        Session session = null;
        try {
            session = this.openSession();
            session.beginTransaction();
            session.save(object);
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }

        return object;
    }

    public void update(T object) {
        Session session = null;
        try {
            session = this.openSession();
            session.beginTransaction();
            session.update(object);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.close();
        }
    }


}

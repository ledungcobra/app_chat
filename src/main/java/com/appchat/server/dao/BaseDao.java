package com.appchat.server.dao;


import com.appchat.server.entities.BaseEntity;
import org.hibernate.Session;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class BaseDao<T extends BaseEntity, ID extends Serializable>
{

    protected final Session session;
    protected final Class<T> clazz;

    public BaseDao(Session session)
    {
        this.session = session;
        clazz = ((Class) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    public T find(ID id)
    {
        return this.session.get(clazz, id);
    }

    public void delete(T object)
    {
        session.delete(object);
    }

    public T insert(T object)
    {
        this.session.save(object);
        return object;
    }

    public void deleteById(ID id)
    {
        this.session.createQuery("DELETE " + clazz.getSimpleName() + "x WHERE x.id=:id")
                .setParameter("id", id)
                .executeUpdate();
        this.session.clear();
    }

    public List<T> findAll()
    {
        return this.session.createQuery("FROM " + clazz.getSimpleName()).getResultList();
    }

}

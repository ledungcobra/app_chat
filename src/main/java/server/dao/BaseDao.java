package server.dao;


import server.entities.BaseEntity;
import org.hibernate.Session;
import server.entities.User;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

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

    public void update(T object)
    {
        this.session.update(object);
    }

    public List<User> getFriends(Long id)
    {
        try
        {
            List<User> friends = session
                    .createQuery("SELECT DISTINCT f.partner FROM FriendShip f WHERE f.owner.id=:id", User.class)
                    .setParameter("id", id).getResultList();

            return friends;
        } catch (Exception e)
        {
            return new ArrayList<>();
        }
    }
}

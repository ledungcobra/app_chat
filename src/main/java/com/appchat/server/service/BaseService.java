package com.appchat.server.service;

import com.appchat.server.dao.BaseDao;
import com.appchat.server.entities.BaseEntity;
import com.appchat.utils.HibernateUtils;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class BaseService<T extends BaseEntity, ID extends Serializable>
{
    protected BaseDao<T, ID> dao;
    protected final ExecutorService service = Executors.newSingleThreadExecutor();

    public BaseService()
    {
    }

    public Future<T> insert(T obj)
    {
        return service.submit(() -> {
            HibernateUtils.doTransaction(() -> {
                dao.insert(obj);
            });
            return obj;
        });
    }

}

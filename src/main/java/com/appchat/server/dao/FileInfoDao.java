package com.appchat.server.dao;

import com.appchat.server.entities.File;
import org.hibernate.Session;

public class FileInfoDao extends BaseDao<File, Long>
{
    public FileInfoDao(Session session)
    {
        super(session);
    }
}

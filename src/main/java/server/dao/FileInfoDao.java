package server.dao;

import server.entities.File;
import org.hibernate.Session;

public class FileInfoDao extends BaseDao<File, Long>
{
    public FileInfoDao(Session session)
    {
        super(session);
    }
}
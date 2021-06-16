package server.dao;

import org.hibernate.SessionFactory;
import server.entities.File;
import org.hibernate.Session;

public class FileInfoDao extends BaseDao<File, Long>
{

    public FileInfoDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }
}

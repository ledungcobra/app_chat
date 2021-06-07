package server.dao;

import server.entities.PrivateMessage;
import org.hibernate.Session;

public class MessageDao extends BaseDao<PrivateMessage, Long>
{
    public MessageDao(Session session)
    {
        super(session);
    }
}

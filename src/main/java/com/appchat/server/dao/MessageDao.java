package com.appchat.server.dao;

import com.appchat.server.entities.PrivateMessage;
import org.hibernate.Session;

public class MessageDao extends BaseDao<PrivateMessage, Long>
{
    public MessageDao(Session session)
    {
        super(session);
    }
}

package server.dao;

import org.hibernate.Session;
import server.entities.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationDao extends BaseDao<Notification, Long>
{
    public NotificationDao(Session session)
    {
        super(session);
    }

    public List<Notification> getNotifications(Long userId)
    {
        try
        {
            return session.createQuery("from Notification where receiveUser.id=:id",Notification.class)
                    .setParameter("id", userId)
                    .getResultList();

        } catch (Exception ex)
        {
            return new ArrayList<>();
        }
    }
}

package server.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import server.entities.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationDao extends BaseDao<Notification, Long> {


    public NotificationDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Notification> getNotifications(Long userId) {
        Session session = null;
        try {
            session = openSession();
            List<Notification> notifications = session.createQuery("from Notification where receiveUser.id=:id", Notification.class)
                    .setParameter("id", userId)
                    .getResultList();

            return notifications;
        } catch (Exception ex) {
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
}

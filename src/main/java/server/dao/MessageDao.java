package server.dao;

import org.hibernate.SessionFactory;
import server.entities.PrivateMessage;
import org.hibernate.Session;

import java.util.List;

public class MessageDao extends BaseDao<PrivateMessage, Long> {

    public MessageDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<PrivateMessage> getMessageByUserId(Long userId, Long friendId, int numberOfMessages, int offset) {
        Session session = null;
        try {
            String query = "with RECURSIVE CTE as (SELECT pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID,1 level " +
                    "                       FROM private_message pm " +
                    "                       where ?1 in (pm.RECEIVER_ID, pm.SENDER_ID) and ?2 in (pm.RECEIVER_ID, pm.SENDER_ID)  " +
                    "                         and pm.NEXT_ID is null " +
                    "" +
                    "                       union all " +
                    "" +
                    "                       SELECT pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID, c.level + 1 " +
                    "                       FROM private_message pm " +
                    "                                join CTE c " +
                    "                                     on pm.NEXT_ID = c.id " +
                    "                       where ?1 in (pm.RECEIVER_ID, pm.SENDER_ID) and  ?2 in (pm.RECEIVER_ID, pm.SENDER_ID)  " + " and c.level < ?3 )" +
                    " select pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID" +
                    " from CTE pm " +
                    " order by id asc ; ";
            session = openSession();
            System.out.println(query);
            List<PrivateMessage> resultList = session.createNativeQuery(query, PrivateMessage.class)
                    .setParameter(1, userId)
                    .setParameter(2, friendId)
                    .setParameter(3, numberOfMessages)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            return null;
        } finally {
            session.close();
        }
    }

    public PrivateMessage addNewMessage(Long senderId, Long receiverId, String content, Long previousMessageId) {
        Session session = null;
        try {
            String query1 = "INSERT INTO " +
                    "PRIVATE_MESSAGE (CREATED_AT, UPDATED_AT, CONTENT, IS_SEEN_BY_RECEIVER, SENDER_ID, RECEIVER_ID, NEXT_ID)" +
                    " values(curdate(), curdate() , ?1 ,0, ?2 , ?3 , null ) ";
            session = openSession();
            session.beginTransaction();
            session.createNativeQuery(query1)
                    .setParameter(1, content)
                    .setParameter(2, senderId)
                    .setParameter(3, receiverId)
                    .executeUpdate();
            session.createNativeQuery("UPDATE PRIVATE_MESSAGE SET NEXT_ID = LAST_INSERT_ID() WHERE ID = ?1")
                    .setParameter(1, previousMessageId)
                    .executeUpdate();
            PrivateMessage msg = session.createNativeQuery("SELECT * FROM PRIVATE_MESSAGE WHERE ID=LAST_INSERT_ID()", PrivateMessage.class).getSingleResult();
            session.getTransaction().commit();
            return msg;
        } catch (Exception e) {
            return null;
        } finally {
            session.close();
        }

    }

}

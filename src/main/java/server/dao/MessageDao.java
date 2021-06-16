package server.dao;

import org.hibernate.SessionFactory;
import server.entities.PrivateMessage;
import org.hibernate.Session;

import java.security.PrivateKey;
import java.util.List;

public class MessageDao extends BaseDao<PrivateMessage, Long> {

    public MessageDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<PrivateMessage> getMessageByUserId(Long userId, Long friendId, int numberOfMessages, int offset) {
        String query = "with RECURSIVE CTE as (SELECT pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID,1 level " +
                "                       FROM private_message pm " +
                "                       where " + userId + " in (pm.RECEIVER_ID, pm.SENDER_ID) and  " + friendId +" in (pm.RECEIVER_ID, pm.SENDER_ID)  "  +
                "                         and pm.NEXT_ID is null " +
                "" +
                "                       union all " +
                "" +
                "                       SELECT pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID, c.level + 1 " +
                "                       FROM private_message pm " +
                "                                join CTE c " +
                "                                     on pm.NEXT_ID = c.id "+
                "                       where " + userId + " in (pm.RECEIVER_ID, pm.SENDER_ID) and  " + friendId +" in (pm.RECEIVER_ID, pm.SENDER_ID)  " +" and c.level < " + numberOfMessages  + ")" +
                " select pm.id, pm.CONTENT, pm.SENDER_ID, pm.RECEIVER_ID, pm.CREATED_AT, pm.IS_SEEN_BY_RECEIVER, pm.UPDATED_AT, pm.NEXT_ID" +
                " from CTE pm " +
                " order by id asc limit " + offset + "," + numberOfMessages + "; ";

        System.out.println(query);
        return getCurrentSession().createNativeQuery(query, PrivateMessage.class).getResultList();
    }

    public PrivateMessage addNewMessage(Long senderId, Long receiverId, String content, Long previousMessageId) {
        getCurrentSession().createNativeQuery("INSERT INTO PRIVATE_MESSAGE (CREATED_AT, UPDATED_AT, CONTENT, IS_SEEN_BY_RECEIVER, SENDER_ID, RECEIVER_ID, NEXT_ID)" +
                " values(curdate(), curdate() ,'" + content + "',0, " + senderId + "," + receiverId + ", null ) ").executeUpdate();
        getCurrentSession().createNativeQuery("UPDATE PRIVATE_MESSAGE SET NEXT_ID = LAST_INSERT_ID() WHERE ID = "+previousMessageId)
                .executeUpdate();
        return getCurrentSession().createNativeQuery("SELECT * FROM PRIVATE_MESSAGE WHERE ID=LAST_INSERT_ID()", PrivateMessage.class).getSingleResult();

    }

    public void markAsSeen(Long messageId) {
        getCurrentSession().createNativeQuery("UPDATE PRIVATE_MESSAGE SET IS_SEEN_BY_RECEIVER = 1 where id=" + messageId)
                .executeUpdate();
    }

}

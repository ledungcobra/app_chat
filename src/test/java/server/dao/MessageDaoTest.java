package server.dao;

import lombok.extern.log4j.Log4j;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import utils.Constants;
import utils.HibernateUtils;

@Log4j
public class MessageDaoTest {


    private static MessageDao messageDao;
    private static SessionFactory sessionFactory;
    private Transaction transaction;
    private static GroupDao groupDao;

    @BeforeClass
    public static void setUp() throws Exception {
        sessionFactory = HibernateUtils.buildSessionFactory(Constants.DATABASE_URL, Constants.DATABASE_USERNAME, Constants.DATABASE_PASSWORD);
        messageDao = new MessageDao(sessionFactory);
        groupDao = new GroupDao(sessionFactory);
    }

    @Before
    public void before() {
        transaction = sessionFactory.getCurrentSession().beginTransaction();

    }

    @After
    public void end() {
        transaction.commit();
    }

    //
//    @Test
//    public void getMessage() {
//        val messages = messageDao.getMessageByUserId(5L, 2);
//        assertEquals(2, messages.size());
//    }
//
//    @Test
//    public void sendMessage() {
//        messageDao.sendMessage(4L, 5L, "Xin chao bạn", 2L);
//        messageDao.getMessageByUserId(5L, 5).
//                forEach(m -> log.debug(m.getSender().getId() + m.getContent() + m.getReceiver().getId() + ""));
//    }

    //    @Test
//    public void markMessageRead() {
//        messageDao.markAsSeen(7L);
//        assertEquals(true, messageDao.find(7L).getIsSeenByReceiver());
//
//    }
    @Test
    public void insertNewGroup() {
    }

}
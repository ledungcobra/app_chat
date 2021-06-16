package utils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class HibernateUtils {

    public static final String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";
    public static final String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";
    public static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";
    public static final String HIBERNATE_CFG_XML = "hibernate.cfg.xml";
    private static SessionFactory sessionFactory;


    public static SessionFactory buildSessionFactory(String dbUrl, String username, String password) {


        Configuration configuration = new Configuration().configure(HIBERNATE_CFG_XML);
        configuration.setProperty(HIBERNATE_CONNECTION_USERNAME, username);
        configuration.setProperty(HIBERNATE_CONNECTION_URL, dbUrl);
        configuration.setProperty(HIBERNATE_CONNECTION_PASSWORD, password);
        sessionFactory = configuration.buildSessionFactory();

        return sessionFactory;

    }


    public static void doTransaction(UnitOfWork work) {

        Transaction transaction = null;
        Session session = sessionFactory.getCurrentSession();

        try {
            transaction = session.beginTransaction();
            work.doWork();
            transaction.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        }

    }

    public static <T> T doTransaction2(UnitOfWork2<T> work) {

        Transaction transaction = null;
        Session session = sessionFactory.getCurrentSession();
        T r = null;
        try {
            transaction = session.beginTransaction();
            r = work.doWork();
            session.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        }

        return r;


    }

}

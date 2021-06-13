package utils;

import lombok.val;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.Objects;

public class HibernateUtils
{

    private static SessionFactory sessionFactory;
    private static Session session;


    public static SessionFactory buildSessionFactory()
    {
        sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
        return sessionFactory;

    }

    public static void sql(String sql)
    {
        val session = openSession();

        val trans = session.beginTransaction();
        try
        {
            val query = session.createSQLQuery(sql);
            query.executeUpdate();
            session.clear();
            trans.commit();
        } finally
        {
            if (trans != null && trans.isActive())
            {
                trans.rollback();
            }
        }
    }


    public static Session openSession()
    {
        if (sessionFactory == null) buildSessionFactory();
        if (Objects.isNull(session))
        {
            session = sessionFactory.openSession();
        }

        return session;
    }

    public static void closeSession()
    {
        if (Objects.nonNull(session))
        {
            session.close();
        }
    }

    public static void doTransaction(UnitOfWork work)
    {

        Transaction transaction = null;

        try
        {
            transaction = openSession().beginTransaction();
            work.doWork();
            transaction.commit();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            if (transaction != null && transaction.isActive())
            {
                transaction.rollback();
            }
            throw ex;
        }

    }

}

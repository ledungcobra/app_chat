package server.dao;

import lombok.val;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import server.entities.FriendOffer;

import java.util.ArrayList;
import java.util.List;

public class FriendOfferDao extends BaseDao<FriendOffer, Long> {


    public FriendOfferDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<FriendOffer> getUnSeenOffer(Long userId) {
        Session session = null;
        try {
            session = openSession();
            val query = session.createQuery("from FriendOffer f where f.partner.id=:userId and f.accepted = null");
            query.setParameter("userId", userId);
            List resultList = query.getResultList();
            return resultList;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            session.close();
        }

    }

    public void acceptFriendForUser(Long friendOfferId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();
            session.createNativeQuery(" INSERT INTO FRIENDSHIP(USER_ID,PARTNER_ID) " +
                    " SELECT DISTINCT fo.OWNER_ID, fo.PARTNER FROM FRIEND_OFFER fo " +
                    " WHERE fo.id=:id ")
                    .setParameter("id", friendOfferId).executeUpdate();
            session.createNativeQuery(" INSERT INTO FRIENDSHIP(USER_ID,PARTNER_ID) " +
                    " SELECT DISTINCT fo.PARTNER,fo.OWNER_ID FROM FRIEND_OFFER fo " +
                    " WHERE fo.id=:id ")
                    .setParameter("id", friendOfferId).executeUpdate();


            session.createNativeQuery("UPDATE FRIEND_OFFER SET ACCEPTED=1 WHERE ID=:id")
                    .setParameter("id", friendOfferId)
                    .executeUpdate();
            session.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void ignoreFriendForUser(Long friendOfferId) {
        Session session = null;
        try {
            session = openSession();
            session.createNativeQuery("UPDATE FRIEND_OFFER SET ACCEPTED=0 WHERE ID=:id")
                    .setParameter("id", friendOfferId)
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}

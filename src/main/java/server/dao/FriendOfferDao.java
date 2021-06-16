package server.dao;

import lombok.val;
import org.hibernate.SessionFactory;
import server.entities.FriendOffer;

import java.util.ArrayList;
import java.util.List;

public class FriendOfferDao extends BaseDao<FriendOffer, Long> {


    public FriendOfferDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<FriendOffer> getUnSeenOffer(Long userId) {
        try {
            val query = getCurrentSession().createQuery("from FriendOffer f where f.partner.id=:userId and f.accepted = null");
            query.setParameter("userId", userId);
            return query.getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    public void acceptFriendForUser(Long friendOfferId) {
        try {
            int numberOfRows = getCurrentSession().createNativeQuery(" INSERT INTO FRIENDSHIP(USER_ID,PARTNER_ID) " +
                    " SELECT DISTINCT fo.OWNER_ID, fo.PARTNER FROM FRIEND_OFFER fo " +
                    " WHERE fo.id=:id ")
                    .setParameter("id", friendOfferId).executeUpdate();
            System.out.println("Insert " + numberOfRows);

            numberOfRows = getCurrentSession().createNativeQuery(" INSERT INTO FRIENDSHIP(USER_ID,PARTNER_ID) " +
                    " SELECT DISTINCT fo.PARTNER,fo.OWNER_ID FROM FRIEND_OFFER fo " +
                    " WHERE fo.id=:id ")
                    .setParameter("id", friendOfferId).executeUpdate();

            System.out.println("Insert " + numberOfRows);

            numberOfRows = getCurrentSession().createNativeQuery("UPDATE FRIEND_OFFER SET ACCEPTED=1 WHERE ID=:id")
                    .setParameter("id", friendOfferId)
                    .executeUpdate();
            System.out.println("Update " + numberOfRows);
            getCurrentSession().clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ignoreFriendForUser(Long friendOfferId) {
        try {
            int numberOfRows = getCurrentSession().createNativeQuery("UPDATE FRIEND_OFFER SET ACCEPTED=0 WHERE ID=:id")
                    .setParameter("id", friendOfferId)
                    .executeUpdate();
            System.out.println("Update " + numberOfRows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package server.dao;

import lombok.val;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import server.entities.FriendShip;

import java.util.List;

public class FriendshipDao extends BaseDao<FriendShip, Long> {

    public FriendshipDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void deleteMany(Long id, List<Long> friendIds) {
        Session session = null;
        try {

            session = openSession();
            openSession().createQuery("delete from FriendShip fs where fs.owner.id=:id and fs.partner.id in (:ids)")
                    .setParameter("id", id)
                    .setParameter("ids", friendIds)
                    .executeUpdate();

            openSession().createQuery("delete from FriendShip  fs where fs.owner.id in (:ids) and fs.partner.id = :id")
                    .setParameter("id", id)
                    .setParameter("ids", friendIds)
                    .executeUpdate();
        } finally {
            session.close();
        }


    }
}

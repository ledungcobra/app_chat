package server.dao;

import org.hibernate.Session;
import server.entities.FriendShip;

import java.util.List;

public class FriendshipDao extends BaseDao<FriendShip, Long>
{
    public FriendshipDao(Session session)
    {
        super(session);
    }


    public void deleteMany(Long id, List<Long> friendIds)
    {
        session.createQuery("delete from FriendShip fs where fs.owner.id=:id and fs.partner.id in (:ids)")
                .setParameter("id", id)
                .setParameter("ids", friendIds)
                .executeUpdate();

        session.createQuery("delete from FriendShip  fs where fs.owner.id in (:ids) and fs.partner.id = :id")
                .setParameter("id", id)
                .setParameter("ids", friendIds)
                .executeUpdate();
    }
}

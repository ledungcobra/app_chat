package server.dao;

import common.dto.GroupDto;
import common.dto.UserPendingDto;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.transform.Transformers;
import server.entities.Group;
import server.entities.User;
import server.entities.UserPending;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GroupDao extends BaseDao<Group, Long> {
    public GroupDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<User> getAllMember(Long groupId) {
        Session session = null;
        try {
            session = openSession();
            return session.createNativeQuery("SELECT u.* FROM USER_GROUP ug INNER JOIN USER u on ug.USER_ID = u.id " +
                    "WHERE ug.GROUP_ID = ?1  ", User.class)
                    .setParameter(1, groupId)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    public void addMemberToGroup(Long userId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery("INSERT INTO USER_GROUP(GROUP_ID,USER_ID ) VALUES(?1, ?2)")
                    .setParameter(1, groupId)
                    .setParameter(2, userId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    public Group createNewGroup(Long createdUserId, String groupName) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();
            String query1 = " INSERT INTO GROUP_CHAT(NAME ) VALUES(?1)  ";
            String query2 = " SELECT LAST_INSERT_ID()  ";
            String query3 = " INSERT INTO USER_GROUP_ADMIN(USER_ID,GROUP_ID) VALUES(?1,?2)  ";
            String query4 = " INSERT INTO USER_GROUP(USER_ID,GROUP_ID) VALUES(?1, ?2)  ";
            String query5 = "SELECT * FROM GROUP_CHAT WHERE ID = ?1";


            session.createNativeQuery(query1)
                    .setParameter(1, groupName)
                    .executeUpdate();

            Long newGroupId = ((BigInteger) session.createNativeQuery(query2).getSingleResult()).longValue();


            session.createNativeQuery(query3)
                    .setParameter(1, createdUserId)
                    .setParameter(2, newGroupId)
                    .executeUpdate();
            session.createNativeQuery(query4)
                    .setParameter(1, createdUserId)
                    .setParameter(2, newGroupId)
                    .executeUpdate();

            Group savedGroup = session.createNativeQuery(query5, Group.class)
                    .setParameter(1, newGroupId)
                    .getSingleResult();

            session.getTransaction().commit();

            return savedGroup;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return null;
    }

    public void removeMember(Long memberId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(" DELETE FROM USER_GROUP WHERE USER_ID = ?1 AND GROUP_ID = ?2 ")
                    .setParameter(1, memberId)
                    .setParameter(2, groupId)
                    .executeUpdate();
            session.createNativeQuery(" DELETE FROM USER_GROUP_ADMIN WHERE USER_ID = ?1 AND GROUP_ID = ?2 ")
                    .setParameter(1, memberId)
                    .setParameter(2, groupId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    public void removeAdmin(Long memberId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(
                    " DELETE FROM USER_GROUP_ADMIN WHERE USER_ID = ?1 AND GROUP_ID = ?2 ")
                    .setParameter(1, memberId)
                    .setParameter(2, groupId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void addAdmin(Long memberId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(
                    "  INSERT INTO USER_GROUP_ADMIN(USER_ID, GROUP_ID) VALUES(?1, ?2)")
                    .setParameter(1, memberId)
                    .setParameter(2, groupId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    public List<Group> findGroupByKeyword(String keyword) {
        Session session = null;
        try {
            session = openSession();
            return session.createQuery("from Group g  where g.name like :keyword", Group.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    public Object[] acceptAPending(Long pendingId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(
                    "  INSERT INTO USER_GROUP(USER_ID, GROUP_ID)  " +
                            "   SELECT USER_ID, GROUP_ID FROM USER_GROUP_PENDING WHERE ID = ?1")
                    .setParameter(1, pendingId)
                    .executeUpdate();
            User user = session.createNativeQuery(
                    "SELECT u.* FROM USER u JOIN USER_GROUP ug ON u.ID = ug.USER_ID AND ug.ID = LAST_INSERT_ID()", User.class)
                    .getSingleResult();

            Group group = session.createNativeQuery(
                    "SELECT g.* FROM GROUP_CHAT g JOIN USER_GROUP ug ON g.ID = ug.GROUP_ID AND ug.ID = LAST_INSERT_ID()", Group.class)
                    .getSingleResult();

            session.getTransaction().commit();
            return new Object[]{user, group};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    public void insertAPending(Long userId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(
                    "  INSERT INTO USER_GROUP_PENDING(USER_ID, GROUP_ID) VALUES " +
                            "  (?1, ?2) ")
                    .setParameter(1, userId)
                    .setParameter(2, groupId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public boolean isAdmin(Long userId, Long groupId) {
        Session session = null;
        try {
            session = openSession();

            return session.createNativeQuery(
                    " SELECT 1 " +
                            " FROM USER_GROUP_ADMIN WHERE  USER_ID=?1 AND GROUP_ID=?2")
                    .setParameter(1, userId)
                    .setParameter(2, groupId)
                    .getResultList() != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }

    public List<UserPending> getPendingList(Long groupId) {
        Session session = null;
        try {
            session = openSession();
            return session.createQuery(
                    "From UserPending up where up.group.id = :groupId", UserPending.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    public void removePending(Long pendingId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();
            session.createNativeQuery(
                    " DELETE  FROM USER_GROUP_PENDING WHERE ID = ?1")
                    .setParameter(1, pendingId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public List<Group> findGroupByUserId(Long userId) {
        Session session = null;
        try {
            session = openSession();
            return session.
                    createNativeQuery("SELECT * FROM GROUP_CHAT gc JOIN USER_GROUP ug ON gc.ID = ug.GROUP_ID WHERE ug.USER_ID = ?1", Group.class)
                    .setParameter(1, userId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }
}

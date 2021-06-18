package server.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import server.entities.Group;
import server.entities.GroupMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupDao extends BaseDao<Group, Long> {
    public GroupDao(SessionFactory sessionFactory) {
        super(sessionFactory);
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


    public void createNewGroup(Long createdUserId, String groupName) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(" INSERT INTO GROUP_CHAT(NAME ) VALUES(?2); " +
                    " SET @GROUP_ID = LAST_INSERT_ID(); " +
                    " INSERT INTO USER_GROUP_ADMIN(USER_ID,GROUP_ID) VALUES(?2,@GROUP_ID); " +
                    " INSERT INTO USER_GROUP(USER_ID,GROUP_ID) VALUES(?1, @GROUP_ID); ")
                    .setParameter(1, createdUserId)
                    .setParameter(2, groupName)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void deleteAMember(Long memberId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(" DELETE FROM USER_GROUP WHERE USER_ID = ?1 AND GROUP_ID = ?2; " +
                    " DELETE FROM USER_GROUP_ADMIN WHERE USER_ID = ?1 AND GROUP_ID = ?2; ")
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

    public void removeAMember(Long memberId, Long groupId) {
        Session session = null;
        try {
            session = openSession();
            session.beginTransaction();

            session.createNativeQuery(" DELETE FROM USER_GROUP WHERE USER_ID = ?1 AND GROUP_ID = ?2;")
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
                    " DELETE FROM USER_GROUP_ADMIN WHERE USER_ID = ?1 AND GROUP_ID = ?2; ")
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



}

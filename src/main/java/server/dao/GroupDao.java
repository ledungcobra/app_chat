package server.dao;

import org.hibernate.SessionFactory;
import server.entities.Group;

public class GroupDao extends BaseDao<Group, Long>{
    public GroupDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void addMemberToGroup(Long userId, Long groupId){

    }

}

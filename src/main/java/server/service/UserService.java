package server.service;

import common.dto.UserPendingDto;
import server.context.SApplicationContext;
import server.dao.UserDao;
import server.entities.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static server.context.SApplicationContext.*;
import static utils.HibernateUtils.*;
import static utils.HibernateUtils.doTransaction2;

public class UserService extends BaseService<User, Long> {

    public UserService() {
        super();
        this.dao = SApplicationContext.userDao;
    }


    public Future<User> findByUserNameAsync(String userName) {
        return service.submit(() -> {
            if (dao instanceof UserDao) {
                return ((UserDao) dao).findByUserName(userName);
            }
            return null;
        });
    }

    public Future<List<User>> findUserByKeywordAsync(String keyword) {
        return service.submit(
                () -> ((UserDao) dao).findByKeyword(keyword));
    }

    public Future<Void> updateUserAsync(User user) {
        return service.submit(() -> {
            doTransaction(() -> {
                dao.update(user);
            });
            return null;
        });
    }

    public Future<List<FriendOffer>> getUnSeenFriendOffersAsync(Long userId) {
        return service.submit(() ->
                doTransaction2(() -> friendOfferDao.getUnSeenOffer(userId))
        );
    }

    public Future<Void> saveFriendOffersAsync(Set<FriendOffer> friendOffers) {
        return service.submit(() -> {
            doTransaction(() -> {
                try {
                    friendOffers.forEach(friendOfferDao::insert);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        });
    }

    public Future<Void> acceptFriendsAsync(List<FriendOffer> friendOffers) {
        return service.submit(() -> {
            doTransaction(() -> {
                friendOffers.forEach(f -> friendOfferDao.acceptFriendForUser(f.getId()));
            });
            return null;
        });

    }

    public Future<Object> ignoreFriendOffersAsync(List<FriendOffer> friendOffers) {
        return service.submit(() -> {
            doTransaction(() -> {
                friendOffers.forEach(f -> {
                    friendOfferDao.ignoreFriendForUser(f.getId());
                });
            });
            return null;
        });
    }

    public Future<?> unFriendsAsync(Long id, List<Long> friendIds) {
        return service.submit(() -> {
            doTransaction(() -> {
                friendshipDao.deleteMany(id, friendIds);

            });
        });
    }

    public Future<List<User>> getFriends(Long id) {
        return service.submit(() ->
                doTransaction2(
                        () -> ((UserDao) userDao).getFriends(id)
                )
        );
    }


    public Future<List<PrivateMessage>> getPrivateMessage(Long userId, Long friendId, int numberOfMessages, int offset) {
        return service.submit(() ->
                doTransaction2(() ->
                        messageDao.getMessageByUserId(userId, friendId, numberOfMessages, offset)));
    }

    public Future<PrivateMessage> addMessageAsync(Long senderId, Long receiverId, String content, Long previousMessageId) {
        return service.submit(() -> {
            return doTransaction2(() -> messageDao.addNewMessage(senderId, receiverId, content, previousMessageId));
        });
    }

    public void joinGroup(Long userId, Long groupId) {
        service.submit(() -> {
            groupDao.insertAPending(userId, groupId);
        });
    }

    public List<Group> findGroupByKeyword(String keyword) {
        return groupDao.findGroupByKeyword(keyword);
    }

    public void leaveGroup(Long groupId, Long userId) {
        groupDao.removeMember(userId, groupId);
    }

    public void addMemberToGroup(Long memberId, Long groupId) {
        groupDao.addMemberToGroup(memberId, groupId);
    }

    public List<User> getAllMembers(Long groupId) {
        return groupDao.getAllMember(groupId);
    }

    public Group createNewGroup(String groupName, Long authorId) {
        return groupDao.createNewGroup(authorId, groupName);
    }

    public Object[] acceptAMember(Long pendingId) {
        return groupDao.acceptAPending(pendingId);
    }

    public void addAdmin(Long adminId, Long groupId) {
        groupDao.addAdmin(adminId, groupId);
    }

    public boolean isAdmin(Long userId, Long groupId) {
        return groupDao.isAdmin(userId, groupId);
    }

    public void removeAdmin(Long userId, Long groupId) {
        groupDao.removeAdmin(userId, groupId);
    }

    public List<GroupMessage> getMessages(Long groupId, Long numberOfMessages) {
        return messageDao.getAllMessage(groupId, numberOfMessages);
    }

    public GroupMessage addGroupMessage(Long senderId, Long  groupId,String content,Long previousMessageId) {
        return messageDao.addNewMessageToGroup(senderId,groupId, content, previousMessageId);
    }

    public List<UserPending> getPendingList(Long groupId) {
        return groupDao.getPendingList(groupId);
    }

    public void removePending(Long pendingId) {
        groupDao.removePending(pendingId);
    }

    public List<Group> getGroupListByUserId(Long userId) {
        return groupDao.findGroupByUserId(userId);
    }

    public User getUserByFriendOfferId(Long id) {
        return ((UserDao)userDao).findByFriendOfferId(id);
    }
}

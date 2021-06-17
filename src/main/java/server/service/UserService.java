package server.service;

import server.context.SApplicationContext;
import server.dao.UserDao;
import server.entities.FriendOffer;
import server.entities.Notification;
import server.entities.PrivateMessage;
import server.entities.User;

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
                        () -> ((UserDao)userDao).getFriends(id)
                )
        );
    }

    public Future<List<Notification>> getNotifications(Long userId) {
        return service.submit(() -> doTransaction2(() -> notificationDao.getNotifications(userId)));
    }

    public Future<List<PrivateMessage>> getPrivateMessage(Long userId, Long friendId, int numberOfMessages,int offset) {
        return service.submit(() ->
                doTransaction2(() ->
                        messageDao.getMessageByUserId(userId,  friendId, numberOfMessages, offset)));
    }

    public Future<PrivateMessage> addMessageAsync(Long senderId, Long receiverId, String content, Long previousMessageId) {
        return service.submit(() -> {
            return doTransaction2(() -> messageDao.addNewMessage(senderId, receiverId, content, previousMessageId));
        });
    }
}
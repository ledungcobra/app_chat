package server.context;

import server.dao.*;
import server.entities.User;
import server.service.PrivateMessageService;
import server.service.UserService;
import utils.HibernateUtils;
import org.hibernate.Session;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SApplicationContext
{
    public static final ExecutorService service;
    public static final UserService userService;
    public static final BaseDao userDao;
    public static final Session session;
    public static final PrivateMessageService privateMessageService;
    public static final ConcurrentMap<Socket, User> currentUsers;
    public static final FriendOfferDao friendOfferDao;
    public static final FriendshipDao friendshipDao;
    public static final NotificationDao notificationDao;


    static
    {

        session = HibernateUtils.openSession();
        service = Executors.newFixedThreadPool(13);

        userDao = new UserDao(session);
        friendOfferDao = new FriendOfferDao(session);
        friendshipDao = new FriendshipDao(session);
        notificationDao = new NotificationDao(session);

        userService = new UserService();
        privateMessageService = new PrivateMessageService();
        currentUsers = new ConcurrentHashMap<>();

    }


}

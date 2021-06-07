package server.context;

import server.dao.BaseDao;
import server.dao.UserDao;
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
    public static final  ConcurrentMap<User, Socket> currentOnlineUsers;


    static
    {

        session = HibernateUtils.openSession();
        service = Executors.newFixedThreadPool(13);

        userDao = new UserDao(session);

        userService = new UserService();
        privateMessageService = new PrivateMessageService();
        currentOnlineUsers = new ConcurrentHashMap<>();

    }


}

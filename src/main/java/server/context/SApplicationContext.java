package server.context;

import org.hibernate.SessionFactory;
import server.core.TCPServer;
import server.dao.*;
import server.entities.User;
import server.service.UserService;
import server.view.ServerConfigScreen;
import utils.HibernateUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SApplicationContext {

    private static String url;
    private static int threadsInt;
    public static ExecutorService service;
    public static UserService userService;
    public static BaseDao userDao;
    public static SessionFactory sessionFactory;
    public static ConcurrentMap<Socket, User> currentUsers;
    public static FriendOfferDao friendOfferDao;
    public static FriendshipDao friendshipDao;
    public static MessageDao messageDao;
    public static GroupDao groupDao;

    public static AtomicBoolean isRunning = new AtomicBoolean(false);
    public static Integer port;
    public static TCPServer tcpServer;
    public static ServerConfigScreen configScreen;


    public static void init(int portInt, String url, int threadsInt, String dbUrl, String username, String password) {

        port = portInt;
        SApplicationContext.url = url;
        SApplicationContext.threadsInt = threadsInt;

        connectDb(dbUrl, username, password);
        userDao = new UserDao(sessionFactory);
        friendOfferDao = new FriendOfferDao(sessionFactory);
        friendshipDao = new FriendshipDao(sessionFactory);
        messageDao = new MessageDao(sessionFactory);
        groupDao = new GroupDao(sessionFactory);

        userService = new UserService();
        currentUsers = new ConcurrentHashMap<>();
    }

    public static void startServer() {
        service = Executors.newFixedThreadPool(threadsInt);
        isRunning.set(true);
        try {
            tcpServer = new TCPServer(url, port);
            service.submit(() -> {

                if (Thread.interrupted()) {
                    System.out.println("STOP");
                }

                while (isRunning.get()) {
                    try {
                        System.out.println("Listening");
                        Socket socket = tcpServer.listenConnection();
                        service.submit(() -> {
                            try {
                                tcpServer.process(socket);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }).get();
            System.out.println("STOP");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopServer() {

        try {
            isRunning.set(false);
//            sessionFactory.close();
            tcpServer.close();

            service.submit(() -> {
                configScreen.updateOnlineList(currentUsers);
                service.shutdownNow();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void connectDb(String dbUrl, String username, String password) {
        sessionFactory = HibernateUtils.buildSessionFactory(dbUrl, username, password);
    }
}

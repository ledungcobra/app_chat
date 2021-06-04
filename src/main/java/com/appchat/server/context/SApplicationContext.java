package com.appchat.server.context;

import com.appchat.server.dao.BaseDao;
import com.appchat.server.dao.UserDao;
import com.appchat.server.service.PrivateMessageService;
import com.appchat.server.service.UserService;
import com.appchat.utils.HibernateUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SApplicationContext
{
    public static final ExecutorService service;
    public static final UserService userService;
    public static final BaseDao userDao;
    public static final Session session;
    public static final PrivateMessageService privateMessageService;

    static
    {

        session = HibernateUtils.openSession();
        service = Executors.newFixedThreadPool(10);

        userDao = new UserDao(session);

        userService = new UserService();
        privateMessageService = new PrivateMessageService();

    }


}

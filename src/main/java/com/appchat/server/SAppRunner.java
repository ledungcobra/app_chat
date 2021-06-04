package com.appchat.server;

import com.appchat.server.context.SApplicationContext;

public class SAppRunner
{
    public static void main(String[] args)
    {

        try
        {
            Class.forName(SApplicationContext.class.getName()  );
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
//
//        try (TCPServer tcpServer = new TCPServer())
//        {
//            tcpServer.initServer();
//
//            while (true)
//            {
//                System.out.println("LISTENING ON PORT " + PORT);
//                Socket socket = tcpServer.listenConnection();
//
//                System.out.println("CONNECTED");
//
//                SApplicationContext.service.submit(() -> {
//                    boolean done;
//                    do
//                    {
//                        done = tcpServer.handleRequest(socket);
//                    } while (!done);
//                });
//
//            }
//
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//        }

    }
}

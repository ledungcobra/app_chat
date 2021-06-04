package com.appchat.server;

import com.appchat.server.context.SApplicationContext;
import com.appchat.server.core.TCPServer;

import java.net.Socket;

import static com.appchat.utils.Constaints.PORT;

public class SAppRunner
{
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException
    {

        try
        {
            Class.forName(SApplicationContext.class.getName());
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }

        try (TCPServer tcpServer = new TCPServer())
        {
            SApplicationContext.service.submit(() -> {
                tcpServer.initServer();

                while (true)
                {
                    System.out.println("LISTENING ON PORT " + PORT);
                    Socket socket = tcpServer.listenConnection();

                    System.out.println("CONNECTED");

                    SApplicationContext.service.submit(() -> {
                        boolean done;
                        try
                        {
                            do
                            {
                                done = tcpServer.handleRequest(socket);
                            } while (!done);
//                            socket.close();
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                            System.out.println("ERROR HERE");
                        }

                    });

                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        Thread.currentThread().join();

    }
}

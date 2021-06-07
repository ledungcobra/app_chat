package server;

import server.context.SApplicationContext;
import server.core.TCPServer;

import java.net.Socket;

public class ServerRunner
{
    public static void main(String[] args) throws InterruptedException
    {



        try (TCPServer tcpServer = new TCPServer())
        {
            Class.forName(SApplicationContext.class.getName());
            SApplicationContext.service.submit(() -> {
                while (true)
                {
                    try{
                        Socket socket = tcpServer.listenConnection();
                        System.out.println("Connected");
                        SApplicationContext.service.submit(() -> {
                            tcpServer.process(socket);
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        Thread.currentThread().join();
    }

}

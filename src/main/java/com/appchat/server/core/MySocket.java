package com.appchat.server.core;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MySocket extends Socket
{

    public MySocket(String host, int port) throws IOException
    {
        super(host, port);
    }

    @Override
    public synchronized void close() throws IOException
    {
        System.out.println("RUN CLOSE");
        super.close();
    }
}

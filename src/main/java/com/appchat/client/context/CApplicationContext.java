package com.appchat.client.context;

import com.appchat.client.core.TCPClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CApplicationContext
{
    public static final ExecutorService service;
    public static final TCPClient tcpClient;

    static
    {

        // 1 thread for connecting/ reconnecting socket
        // 1 thread for sending request
        // 1 thread for listening incoming notification
        service = Executors.newFixedThreadPool(3);

        tcpClient = new TCPClient();

    }
}

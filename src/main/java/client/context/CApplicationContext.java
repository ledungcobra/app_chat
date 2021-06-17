package client.context;

import client.core.TCPClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CApplicationContext {
    public static ExecutorService networkThreadService;
    public static ExecutorService uiThreadService;

    public static TCPClient tcpClient;

    static {
        // 1 thread for connecting/ reconnecting socket
        // 1 thread for sending request
        // 1 thread for listening incoming notification
        networkThreadService = Executors.newFixedThreadPool(6);
        uiThreadService = Executors.newFixedThreadPool(3);
    }

    public static void init(String host, int port) {
        tcpClient = new TCPClient(host, port);
    }


}

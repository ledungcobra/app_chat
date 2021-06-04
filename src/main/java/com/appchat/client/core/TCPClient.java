package com.appchat.client.core;

import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;

import static com.appchat.utils.Constaints.HOST;
import static com.appchat.utils.Constaints.PORT;

public class TCPClient {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;


    public TCPClient() {
    }

    @SneakyThrows(value = {UnknownHostException.class, IOException.class, SecurityException.class, IllegalStateException.class})
    public Socket connectToServer() {
        if (this.socket == null) {
            this.socket = new Socket(HOST, PORT);
        } else {
            if (this.socket.isClosed()) {
                this.socket.connect(new InetSocketAddress(HOST, PORT));
            }
        }
        return this.socket;
    }



}
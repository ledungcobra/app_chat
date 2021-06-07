package utils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketExtension
{


    @SneakyThrows
    public static BufferedReader getBufferedReader(@NonNull Socket socket)
    {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @SneakyThrows
    public static BufferedWriter getBufferedWriter(@NonNull Socket socket)
    {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @SneakyThrows
    public static ObjectInputStream getObjectInputStream(@NonNull Socket socket)
    {
        return new ObjectInputStream(socket.getInputStream());
    }

    @SneakyThrows
    public static ObjectOutputStream getObjectOutputStream(@NonNull Socket socket)
    {
        return new ObjectOutputStream(socket.getOutputStream());
    }
}
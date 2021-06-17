package utils;

import common.dto.CommandObject;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static server.core.TCPServer.objectOutputStreamMap;

public class SocketExtension {

    public static ObjectInputStream getObjectInputStream(@NonNull Socket socket) {
        try {
            return new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ObjectOutputStream getObjectOutputStream(@NonNull Socket socket) {
        try {
            return new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean sendResponseToSocket(Socket socket, CommandObject commandObject) {
        if (socket == null) return false;
        try {
            final ObjectOutputStream anotherUserStream = objectOutputStreamMap.get(socket);
            synchronized (anotherUserStream) {
                anotherUserStream.writeObject(commandObject);
                anotherUserStream.flush();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}


package server.handler;

import common.dto.Command;
import common.dto.CommandObject;
import server.context.SApplicationContext;
import server.entities.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static server.context.SApplicationContext.currentUsers;

public abstract class RequestHandler {
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    protected Socket socket;

    public RequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.socket = socket;
    }

    protected Future<Boolean> sendResponseAsync(CommandObject object) {
        try {
            System.out.println("Send " + object + " to " + currentUsers.get(socket).getDisplayName());
        } catch (Exception e) {

        } finally {
            return SApplicationContext.service.submit(() -> sendResponse(object));
        }
    }

    public boolean sendResponse(CommandObject object) {
        if (objectOutputStream == null) return false;
        synchronized (objectOutputStream) {
            try {
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public User getCurrentUser() {
        return currentUsers.get(socket);
    }

    public Optional<Socket> getFriendSocket(User user) {
        return currentUsers.entrySet().stream().filter(e -> e.getValue().getId().equals(user.getId()))
                .map(e -> e.getKey()).findFirst();
    }

    public abstract Optional<Consumer<CommandObject>> getHandle(Command command);

}

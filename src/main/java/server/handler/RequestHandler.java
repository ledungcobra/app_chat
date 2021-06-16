package server.handler;

import common.dto.Command;
import common.dto.CommandObject;
import server.context.SApplicationContext;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static server.context.SApplicationContext.currentUsers;

public abstract class RequestHandler
{
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    protected Socket socket;

    public RequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.socket = socket;
    }

    protected Future<Boolean> sendResponseAsync(CommandObject object)
    {
        System.out.println("Send "+ object + " to " + currentUsers.get(socket).getDisplayName());
        return SApplicationContext.service.submit(() -> sendResponse(object));
    }

    public boolean sendResponse(CommandObject object)
    {
        if (objectOutputStream == null) return false;
        synchronized (objectOutputStream)
        {
            try
            {
                System.out.println("SEND " + object);
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
                return true;
            } catch (Exception e)
            {
                return false;
            }
        }
    }

    public abstract Optional<Consumer<CommandObject>> getHandle(Command command);

}

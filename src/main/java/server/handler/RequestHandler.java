package server.handler;

import server.context.SApplicationContext;
import common.dto.Command;
import common.dto.CommandObject;
import utils.SocketExtension;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

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

        return SApplicationContext.service.submit(() -> {
            return sendResponse(object);
        });
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

package client.core;

import client.context.CApplicationContext;
import common.dto.Command;
import common.dto.CommandObject;
import lombok.Getter;
import lombok.SneakyThrows;
import utils.SocketExtension;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.Constants.HOST;
import static utils.Constants.PORT;

@Getter
public class TCPClient implements Closeable
{

    private Socket socket;
    private CopyOnWriteArrayList<ResponseHandler> handlers;
    private AtomicBoolean isListening = new AtomicBoolean(false);
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public TCPClient()
    {
        handlers = new CopyOnWriteArrayList<>();
    }

    @SneakyThrows(value = {UnknownHostException.class, IOException.class, SecurityException.class, IllegalStateException.class})

    public Socket connect()
    {
        this.socket = new Socket(HOST, PORT);
        oos = SocketExtension.getObjectOutputStream(this.socket);
        ois = SocketExtension.getObjectInputStream(this.socket);
        return this.socket;
    }


    public CompletableFuture<Socket> connectAsync()
    {
        CompletableFuture<Socket> completableFuture = new CompletableFuture<>();

        CApplicationContext.service.submit(() -> {
            try
            {
                this.socket = connect();
                completableFuture.complete(socket);
            } catch (Exception e)
            {
                this.socket = null;
                completableFuture.complete(null);
                e.printStackTrace();
            } finally
            {
                return this.socket;
            }
        });
        return completableFuture;
    }

    public Future<Boolean> sendRequestAsync(CommandObject commandObject)
    {
        return CApplicationContext.service.submit(() -> sendRequest(commandObject));
    }

    public synchronized boolean sendRequest(CommandObject commandObject)
    {
        if (this.socket == null) return false;
        synchronized (oos)
        {
            try
            {
                oos.writeObject(commandObject);
                oos.flush();
                return true;
            } catch (IOException e)
            {
                return false;
            }
        }
    }


    // Run And wait
    @SneakyThrows
    public void listeningOnEventAsync()
    {
        System.out.println("CLIENT LISTENING");

        this.isListening.set(true);

        CApplicationContext.service.submit(() -> {
            while (isListening.get())
            {
                for (int i = 0; i < this.handlers.size(); i++)
                {
                    CommandObject commandObject = readObjectFromInputStream();
                    if (commandObject == null) continue;
                    System.out.println("RECEIVED " + commandObject);
                    handlers.get(i).listen(commandObject);

                }
            }
        });
    }

    public CommandObject readObjectFromInputStream()
    {
        CommandObject object = null;
        synchronized (ois)
        {
            try
            {
                object = (CommandObject) ois.readObject();
            } catch (Exception e)
            {
            } finally
            {
            }
        }

        return object;
    }

    @Override
    public void close() throws IOException
    {
        if (socket != null)
        {
            isListening.set(false);
            this.socket.close();
            this.socket = null;
        }
    }

    public Boolean stillAlive()
    {
        return this.sendRequest(new CommandObject(Command.C2S_PING));
    }


    public void closeHandler(ResponseHandler handler)
    {
        this.handlers.remove(handler);
    }

    public void addListener(ResponseHandler handler)
    {
        this.handlers.add(handler);
    }
}
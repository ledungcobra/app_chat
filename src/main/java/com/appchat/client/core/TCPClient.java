package com.appchat.client.core;

import com.appchat.client.context.CApplicationContext;
import com.appchat.server.dto.CommandObject;
import com.appchat.utils.SocketExtension;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.appchat.utils.Constaints.HOST;
import static com.appchat.utils.Constaints.PORT;

public class TCPClient implements Closeable
{

    private Socket socket;
    private AtomicBoolean isConnected = new AtomicBoolean(false);


    public Future<Boolean> tryConnectAsync(int tryCount)
    {
        final AtomicInteger tryCountAtomic = new AtomicInteger(tryCount);
        return CApplicationContext.service.submit(() -> {

            while (tryCountAtomic.getAndDecrement() > 0)
            {
                try
                {
                    this.connectToServer();

                    if (socket != null)
                    {
                        isConnected.set(true);
                        break;
                    } else
                    {
                        isConnected.set(false);
                    }

                    Thread.sleep(1000);

                } catch (Exception e)
                {
                    isConnected.set(false);
                    e.printStackTrace();
                }
            }
            return isConnected.get();
        });
    }

    @SneakyThrows(value = {UnknownHostException.class, IOException.class, SecurityException.class, IllegalStateException.class})
    public Socket connectToServer()
    {
        this.socket = new Socket(HOST, PORT);
        return this.socket;
    }

    public Future<Boolean> sendRequestAsync(CommandObject commandObject)
    {
        return CApplicationContext.service.submit(() -> {
            ObjectOutputStream oos = SocketExtension.getObjectOutputStream(this.socket);
            try
            {
                oos.writeObject(commandObject);
                oos.flush();
                return true;
            } catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }
        });
    }

    @SneakyThrows
    public CommandObject receive()
    {
        ObjectInputStream ois = SocketExtension.getObjectInputStream(this.socket);
        try
        {
            return (CommandObject) ois.readObject();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void close() throws IOException
    {
        if (socket != null) this.socket.close();
    }

    public boolean isActive()
    {
        return this.socket != null && this.socket.isConnected() && !this.socket.isClosed();
    }
}
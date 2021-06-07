package server.core;

import server.context.SApplicationContext;
import common.dto.Command;
import common.dto.CommandObject;
import server.handler.AuthRequestHandler;
import server.handler.PingRequestHandler;
import server.handler.RequestHandler;
import server.service.PrivateMessageService;
import utils.SocketExtension;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.Constants.INITIALIZING_BEFORE_MSG;
import static utils.Constants.PORT;

public class TCPServer implements Closeable
{
    private ServerSocket socket;
    private PrivateMessageService privateMessageService = SApplicationContext.privateMessageService;
    private final Map<Socket, ObjectInputStream> objectInputStreamMap;
    private Map<Socket, ObjectOutputStream> objectOutputStreamMap;

    public TCPServer() throws IOException
    {
        this.socket = new ServerSocket(PORT);
        this.objectInputStreamMap = new HashMap<>();
        this.objectOutputStreamMap = new HashMap<>();

    }


    public synchronized Socket listenConnection() throws Exception
    {
        if (this.socket == null) throw new Exception(INITIALIZING_BEFORE_MSG);
        Socket anonymousSocket = socket.accept();

        this.objectInputStreamMap.put(anonymousSocket, SocketExtension.getObjectInputStream(anonymousSocket));
        this.objectOutputStreamMap.put(anonymousSocket, SocketExtension.getObjectOutputStream(anonymousSocket));


        if (anonymousSocket == null)
        {
            throw new Exception("Cannot initial socket");
        }
        return anonymousSocket;
    }

    // Run and wait
    public void process(Socket socket)
    {
        List<RequestHandler> handlers = registerHandlers(socket);
        listeningOnEvent(socket, handlers);
    }

    public ObjectInputStream getObjectInputStream(Socket socket)
    {
        return this.objectInputStreamMap.get(socket);
    }

    public ObjectOutputStream getObjectOutputStream(Socket socket)
    {
        return this.objectOutputStreamMap.get(socket);
    }

    private List<RequestHandler> registerHandlers(Socket socket)
    {
        List<RequestHandler> requestHandlers = new ArrayList<>();
        val inputStream = getObjectInputStream(socket);
        val outputStream = getObjectOutputStream(socket);

        requestHandlers.add(new AuthRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new PingRequestHandler(inputStream, outputStream, socket));

        return requestHandlers;
    }

    // Run and wait
    private void listeningOnEvent(Socket socket, List<RequestHandler> handlers)
    {
        while (true)
        {
            // Run and wait
            System.out.println("LISTENING");
            CommandObject commandObject = null;

            commandObject = this.readObjectFromInputStream(objectInputStreamMap.get(socket));
            System.out.println("RECEIVED " + commandObject);

            if (commandObject == null)
            {
                System.out.println("STOP LISTENING");

                synchronized (this.objectOutputStreamMap)
                {
                    this.objectOutputStreamMap.remove(socket);
                }
                synchronized (this.objectInputStreamMap)
                {
                    this.objectInputStreamMap.remove(socket);
                }

                break;
            }

            System.out.println((commandObject.getCommand().toString()));

            if (commandObject.getCommand().equals(Command.C2S_EXIT))
            {
                System.out.println("BREAK");
                break;
            }

            CommandObject finalCommandObject = commandObject;
            handlers.forEach(handler ->
                    handler.getHandle(finalCommandObject.getCommand()).ifPresent(handlerSync ->
                            {
                                SApplicationContext.service.submit(() -> {
                                    handlerSync.accept(finalCommandObject);
                                });
                            }
                    )
            );


        }


    }

    public CommandObject readObjectFromInputStream(final ObjectInputStream objectInputStream)
    {
        CommandObject object = null;
        synchronized (objectInputStream)
        {
            try
            {
                object = (CommandObject) objectInputStream.readObject();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {

            }
        }

        return object;
    }

    @Override
    public void close() throws IOException
    {
        for (val entry : SApplicationContext.currentOnlineUsers.entrySet())
        {
            entry.getValue().close();
        }
    }

}

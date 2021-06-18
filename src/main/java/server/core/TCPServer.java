package server.core;

import common.dto.CommandObject;
import lombok.extern.log4j.Log4j;
import lombok.val;
import server.handler.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static server.context.SApplicationContext.*;
import static utils.Constants.INITIALIZING_BEFORE_MSG;

@Log4j(topic = "LOG")
public class TCPServer implements Closeable {
    private ServerSocket socket;
    public static final Map<Socket, ObjectInputStream> objectInputStreamMap;
    public static final Map<Socket, ObjectOutputStream> objectOutputStreamMap;
    public static final Set<Socket> sockets;

    static {
        objectInputStreamMap = new HashMap<>();
        objectOutputStreamMap = new HashMap<>();
        sockets = new HashSet<>();
    }

    public void addSocketToMap(Socket socket) {
        if (objectOutputStreamMap.containsKey(socket) &&
                objectInputStreamMap.containsKey(socket)
        ) {
            return;
        }

        try {
            objectInputStreamMap.put(socket, new ObjectInputStream(socket.getInputStream()));
            objectOutputStreamMap.put(socket, new ObjectOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        sockets.add(socket);

    }

    public TCPServer(String ip, Integer port) throws IOException {
        String[] token = ip.split("\\.");
        byte[] bytes = new byte[token.length];
        for (int i = 0; i < token.length; i++) {
            bytes[i] = Byte.parseByte(token[i]);
        }
        System.out.println(Arrays.toString(bytes));
        this.socket = new ServerSocket(port, 0, InetAddress.getByAddress(bytes));
    }

    public synchronized Socket listenConnection() throws Exception {
        if (this.socket == null) throw new Exception(INITIALIZING_BEFORE_MSG);
        Socket anonymousSocket = socket.accept();


        if (anonymousSocket == null) {
            throw new Exception("Cannot initial socket");
        }
        addSocketToMap(anonymousSocket);

        return anonymousSocket;
    }

    // Run and wait
    public void process(Socket socket) {
        if (currentUsers.containsKey(socket)) {
            System.out.println("Already listening to this user ");
            return;
        }
        List<RequestHandler> handlers = registerHandlers(socket);
        listeningOnEvent(socket, handlers);
    }

    public ObjectInputStream getObjectInputStream(Socket socket) {
        return objectInputStreamMap.get(socket);
    }

    public ObjectOutputStream getObjectOutputStream(Socket socket) {
        return objectOutputStreamMap.get(socket);
    }

    private List<RequestHandler> registerHandlers(Socket socket) {
        List<RequestHandler> requestHandlers = new ArrayList<>();
        val inputStream = getObjectInputStream(socket);
        val outputStream = getObjectOutputStream(socket);

        requestHandlers.add(new AuthRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new PingRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new GetListRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new FriendRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new MessageRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new FileRequestHandler(inputStream, outputStream, socket));
        requestHandlers.add(new GroupRequestHandler(inputStream, outputStream, socket));

        return requestHandlers;
    }

    // Run and wait
    private void listeningOnEvent(Socket socket, List<RequestHandler> handlers) {
        while (true) {
            // Run and wait
            System.out.println("LISTENING");
            CommandObject commandObject = null;

            commandObject = this.readObjectFromInputStream(objectInputStreamMap.get(socket));
            System.out.println("RECEIVED " + commandObject);

            if (commandObject == null) {
                System.out.println("A client exit");
                synchronized (objectOutputStreamMap) {
                    objectOutputStreamMap.remove(socket);
                }
                synchronized (objectInputStreamMap) {
                    objectInputStreamMap.remove(socket);
                }
                currentUsers.remove(socket);
                configScreen.updateOnlineList(currentUsers);
                break;
            }

            System.out.println((commandObject.getCommand().toString()));

            CommandObject finalCommandObject = commandObject;
            handlers.forEach(handler ->
                    handler.getHandle(finalCommandObject.getCommand())
                            .ifPresent(handlerSync ->
                                    {
                                        service.submit(() -> {
                                            handlerSync.accept(finalCommandObject);
                                        });
                                    }
                            )
            );
        }

    }

    public CommandObject readObjectFromInputStream(final ObjectInputStream objectInputStream) {
        CommandObject object = null;
        synchronized (objectInputStream) {
            try {
                object = (CommandObject) objectInputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    @Override
    public void close() throws IOException {
        for (val s : sockets) {
            s.close();
        }
        this.socket.close();
        sockets.clear();
        currentUsers.clear();

    }

}

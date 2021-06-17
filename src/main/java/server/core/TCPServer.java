package server.core;

import lombok.extern.log4j.Log4j;
import server.context.SApplicationContext;
import common.dto.CommandObject;
import server.handler.*;
import utils.SocketExtension;
import lombok.val;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static server.context.SApplicationContext.*;
import static utils.Constants.INITIALIZING_BEFORE_MSG;
import static utils.Constants.PORT;

@Log4j(topic = "LOG")
public class TCPServer implements Closeable {
    private ServerSocket socket;
    public static final Map<Socket, ObjectInputStream> objectInputStreamMap;
    public static final Map<Socket, ObjectOutputStream> objectOutputStreamMap;

    static {
        objectInputStreamMap = new HashMap<>();
        objectOutputStreamMap = new HashMap<>();
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

        objectInputStreamMap.put(anonymousSocket, SocketExtension.getObjectInputStream(anonymousSocket));
        objectOutputStreamMap.put(anonymousSocket, SocketExtension.getObjectOutputStream(anonymousSocket));


        if (anonymousSocket == null) {
            throw new Exception("Cannot initial socket");
        }
        return anonymousSocket;
    }

    // Run and wait
    public void process(Socket socket) {
        if(currentUsers.containsKey(socket)) {
            System.out.println("Listening to this user already");
            return;
        }
        List<RequestHandler> handlers = registerHandlers(socket);
        listeningOnEvent(socket, handlers);
    }

    public ObjectInputStream getObjectInputStream(Socket socket) {
        return this.objectInputStreamMap.get(socket);
    }

    public ObjectOutputStream getObjectOutputStream(Socket socket) {
        return this.objectOutputStreamMap.get(socket);
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
        requestHandlers.add(new CallRequestHandler(inputStream, outputStream, socket));

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
                synchronized (this.objectOutputStreamMap) {
                    this.objectOutputStreamMap.remove(socket);
                }
                synchronized (this.objectInputStreamMap) {
                    this.objectInputStreamMap.remove(socket);
                }
                currentUsers.remove(socket);
                configScreen.updateOnlineList(currentUsers);
                break;
            }

            System.out.println((commandObject.getCommand().toString()));

            CommandObject finalCommandObject = commandObject;
            handlers.forEach(handler ->
                    handler.getHandle(finalCommandObject.getCommand()).ifPresent(handlerSync ->
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
            } finally {

            }
        }

        return object;
    }

    @Override
    public void close() throws IOException {
        for (val entry : currentUsers.entrySet()) {
            entry.getKey().close();
        }
        this.socket.close();
    }

}

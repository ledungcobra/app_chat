package client.core;

import common.dto.Command;
import common.dto.CommandObject;
import lombok.Getter;
import utils.SocketExtension;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static client.context.CApplicationContext.*;

@Getter
public class TCPClient implements Closeable {

    private Socket socket;
    private final CopyOnWriteArrayList<ResponseHandler> handlers;
    private AtomicBoolean isListening = new AtomicBoolean(false);
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String host;
    private int port;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
        handlers = new CopyOnWriteArrayList<>();
    }


    public Socket connect() throws IOException {
        if (this.socket != null) this.socket.close();

        this.socket = new Socket(host, port);
        oos = SocketExtension.getObjectOutputStream(this.socket);
        ois = SocketExtension.getObjectInputStream(this.socket);

        return this.socket;
    }


    public CompletableFuture<Socket> connectAsync() {
        CompletableFuture<Socket> completableFuture = new CompletableFuture<>();

        networkThreadService.submit(() -> {
            try {
                this.socket = connect();
                completableFuture.complete(socket);
            } catch (Exception e) {
                this.socket = null;
                completableFuture.complete(null);
                e.printStackTrace();
            } finally {
                return this.socket;
            }
        });
        return completableFuture;
    }

    public Future<Boolean> sendRequestAsync(CommandObject commandObject) {
        return networkThreadService.submit(() -> sendRequest(commandObject));
    }

    public boolean sendRequest(CommandObject commandObject) {
        if (this.socket == null) return false;
        synchronized (oos) {
            try {
                oos.writeObject(commandObject);
                oos.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }


    // Run And wait
    public synchronized void listeningOnEventAsync() {

        System.out.println("CLIENT LISTENING");
        this.isListening.set(true);
        networkThreadService.submit(() -> {
            out:
            while (isListening.get()) {
                try {
                    CommandObject commandObject = readObjectFromInputStream();
                    for (int i = 0; i < this.handlers.size(); i++) {
                        System.out.println("Number of listener is " + handlers.size());
                        if (commandObject == null) {
                            isListening.set(false);
                            System.out.println("STOP LISTENING");
                            break out;
                        }
                        System.out.println("RECEIVED " + commandObject);
                        System.out.println("Passs to " + handlers.get(i).getClass().getSimpleName());
                        handlers.get(i).listenOnNetworkEvent(commandObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public CommandObject readObjectFromInputStream() {
        CommandObject object = null;
        synchronized (ois) {
            try {
                object = (CommandObject) ois.readObject();
            } catch (Exception e) {
                object = null;
                e.printStackTrace();
            }
        }

        return object;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            handlers.clear();
            isListening.set(false);
            this.socket.close();
            this.socket = null;
        }
    }

    public Boolean stillAlive() {
        return this.sendRequest(new CommandObject(Command.C2S_PING));
    }


    public void closeHandler(ResponseHandler handler) {
        synchronized (this.handlers) {
            this.handlers.remove(handler);
        }
    }

    public void registerListener(ResponseHandler handler) {
        synchronized (this.handlers) {
            this.handlers.add(handler);
        }
    }

    /**
     *  Clear all thread
     * @throws IOException
     */
    public void reconnect() throws IOException {
        networkThreadService.shutdownNow();
        connect();
        networkThreadService = Executors.newFixedThreadPool(6);
        listeningOnEventAsync();
    }
}
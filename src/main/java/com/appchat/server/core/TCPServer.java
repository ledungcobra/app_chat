package com.appchat.server.core;

import com.appchat.server.dto.Command;
import com.appchat.server.dto.CommandObject;
import com.appchat.server.context.SApplicationContext;
import com.appchat.server.entities.PrivateMessage;
import com.appchat.server.entities.User;
import com.appchat.server.service.PrivateMessageService;
import com.appchat.server.service.UserService;
import com.appchat.utils.SocketExtension;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.appchat.utils.Constaints.PORT;

public class TCPServer implements Closeable
{

    public static final String INITIALIZING_BEFORE_MSG = "You must call init server before calling this method";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USRNAME_PWD_FAIL = "User name or password wrong please check it";
    public static final String REGISTER_SUCCESS = "Register success";
    public static final String SALT = "SALT";
    private ServerSocket socket;
    private ConcurrentMap<User, Socket> currentOnlineUsers;
    private final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<>();
    private UserService userService = SApplicationContext.userService;

    private PrivateMessageService privateMessageService = SApplicationContext.privateMessageService;

    public TCPServer()
    {
        currentOnlineUsers = new ConcurrentHashMap<>();
    }


    @SneakyThrows
    public ServerSocket initServer()
    {
        this.socket = new ServerSocket(PORT);
        return this.socket;
    }

    public ConcurrentLinkedQueue getQueue()
    {
        return queue;
    }

    @SneakyThrows
    public synchronized Socket listenConnection()
    {
        if (this.socket == null) throw new Exception(INITIALIZING_BEFORE_MSG);
        Socket anonymousSocket = socket.accept();

        if (anonymousSocket == null)
        {
            throw new Exception("Cannot initial socket");
        }
        return anonymousSocket;
    }


    @SneakyThrows(value = {IOException.class})
    public String sendMessage(@NonNull BufferedWriter bufferedWriter, @NonNull String message)
    {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        return message;
    }

    @SneakyThrows
    public String receiveMessage(@NonNull BufferedReader bufferedReader)
    {
        return bufferedReader.readLine();
    }

    @Override
    public void close() throws IOException
    {
        for (val entry : currentOnlineUsers.entrySet())
        {
            entry.getValue().close();
        }
    }

    /**
     * @param clientSocket current socket connect to the server
     *                     this will handle request tho a single user in one thread
     * @return isDone
     */
    @SneakyThrows
    public boolean handleRequest(final Socket clientSocket)
    {
        ObjectInputStream oip = SocketExtension.getObjectInputStream(clientSocket);
        if (oip != null)
        {
            CommandObject commandObject = (CommandObject) oip.readObject();
            switch (commandObject.getCommand())
            {
                case C2S_LOGIN:
                {
                    return handleLogin(clientSocket, commandObject);
                }
                case C2S_REGISTER:
                {
                    return handleRegister(clientSocket, commandObject);
                }
                case C2S_SEND_PRIVATE_MESSAGE:
                {
                    return handleSendMessage(clientSocket, commandObject);
                }

                case C2S_EXIT:
                    return true;
            }
        }
        return false;
    }

    /**
     * @param clientSocket
     * @param commandObject payload instanceof Message
     * @return
     */
    @SneakyThrows
    private boolean handleSendMessage(Socket clientSocket, CommandObject commandObject)
    {
        PrivateMessage message = (PrivateMessage) commandObject.getPayload();
        if (message == null)
        {
            System.out.println("Message is null");
            return false;
        }

        User receiver = message.getReceiver();

        if (receiver == null)
        {
            System.out.println("Receiver is null");
            return false;
        }

        Socket receiverSocket = this.currentOnlineUsers.get(receiver);
        if (receiverSocket != null)
        {
            // Receiver is online
            CommandObject messageObj = new CommandObject();
            messageObj.setCommand(Command.S2C_SEND_PRIVATE_MESSAGE);
            messageObj.setPayload(commandObject.getPayload());

            this.responseRequest(receiverSocket, commandObject);

        }

        privateMessageService.insert(message);
        return false;
    }

    /**
     * @param clientSocket
     * @param commandObject
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */

    private boolean handleLogin(Socket clientSocket, CommandObject commandObject) throws ExecutionException, InterruptedException
    {
        User user = (User) commandObject.getPayload();
        CommandObject response = new CommandObject();

        if (user == null)
        {
            response.setCommand(Command.S2C_LOGIN_NACK);
            response.setPayload(USER_NOT_FOUND);
            responseRequest(clientSocket, response);

            return true;
        }

        User userInDb = userService.findByUserName(user.getUserName()).get();

        if (userInDb == null)
        {
            response.setCommand(Command.S2C_LOGIN_NACK);
            response.setPayload(USER_NOT_FOUND);
            responseRequest(clientSocket, response);
            return true;
        } else
        {
            if (userInDb.getUserName().equals(user.getUserName()) &&
                    BCrypt.checkpw(user.getPassword(), userInDb.getPassword())
            )
            {
                // Success login
                response.setCommand(Command.S2C_LOGIN_ACK);
                userInDb.getMessages();

                response.setPayload(userInDb);
                currentOnlineUsers.put(user, clientSocket);

            } else
            {
                response.setCommand(Command.S2C_LOGIN_NACK);
                response.setPayload(USRNAME_PWD_FAIL);
                return true;
            }
            responseRequest(clientSocket, response);
        }
        return false;
    }

    /**
     * @param clientSocket
     * @param commandObject
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private boolean handleRegister(Socket clientSocket, CommandObject commandObject) throws ExecutionException, InterruptedException
    {
        User user = (User) commandObject.getPayload();
        CommandObject response = new CommandObject();

        if (user == null)
        {
            response.setCommand(Command.S2C_LOGIN_NACK);
            response.setPayload(USER_NOT_FOUND);
            responseRequest(clientSocket, response);
            return true;
        }

        User userInDb = userService.findByUserName(user.getUserName()).get();

        if (userInDb == null)
        {
            user.setPassword(BCrypt.hashpw(user.getPassword(), SALT));
            response.setCommand(Command.S2C_REGISTER_ACK);
            response.setPayload(user);

            responseRequest(clientSocket, response);
            userService.insert(user);
        } else
        {
            response.setCommand(Command.S2C_REGISTER_ACK);
            response.setPayload("User already exist please take another username");
            responseRequest(clientSocket, response);
            return true;
        }
        return false;
    }


    /**
     * @param clientSocket
     * @param commandObject
     * @return result ? true:Completed, false: fail
     */

    public Future<Boolean> responseRequest(final Socket clientSocket, CommandObject commandObject)
    {
        return SApplicationContext.service.submit(() -> {
            try (ObjectOutputStream objectOutputStream = SocketExtension.getObjectOutputStream(clientSocket);)
            {
                objectOutputStream.writeObject(commandObject);
                objectOutputStream.flush();

                return true;
            } catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }
        });
    }

}

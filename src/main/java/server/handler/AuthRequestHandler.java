package server.handler;

import common.dto.Command;
import common.dto.CommandObject;
import common.dto.ObjectMapper;
import common.dto.UserDto;
import lombok.SneakyThrows;
import org.mindrot.jbcrypt.BCrypt;
import server.context.SApplicationContext;
import server.entities.User;
import server.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

import static server.context.SApplicationContext.currentOnlineUsers;
import static utils.Constants.USER_NOT_FOUND;
import static utils.Constants.USRNAME_PWD_FAIL;

public class AuthRequestHandler extends RequestHandler
{
    private UserService userService = SApplicationContext.userService;


    public AuthRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        super(objectInputStream, objectOutputStream, socket);
    }

    @SneakyThrows
    private void login(CommandObject commandObject)
    {
        User user = (User) commandObject.getPayload();
        CommandObject response = new CommandObject();

        if (user == null)
        {
            sendResponse(new CommandObject(Command.S2C_LOGIN_NACK, USER_NOT_FOUND));
            sendResponse(new CommandObject(Command.S2C_EXIT));
            return;
        }

        // Wait for database
        User userInDb = userService.findByUserName(user.getUserName()).get();

        if (userInDb == null)
        {
            sendResponseAsync(new CommandObject(Command.S2C_LOGIN_NACK, USER_NOT_FOUND));

            CommandObject exitCommand = new CommandObject();
            exitCommand.setCommand(Command.S2C_EXIT);
            sendResponseAsync(exitCommand);

        } else
        {
            if (userInDb.getUserName().equals(user.getUserName()) &&
                    BCrypt.checkpw(user.getPassword(), userInDb.getPassword())
            )
            {
                // Success login
                response.setCommand(Command.S2C_LOGIN_ACK);
                
                response.setPayload(ObjectMapper.<User, UserDto>map(userInDb));
                currentOnlineUsers.put(user, socket);

            } else
            {
                response.setCommand(Command.S2C_LOGIN_NACK);
                response.setPayload(USRNAME_PWD_FAIL);
            }
            sendResponseAsync(response);
        }
    }

    @SneakyThrows
    private void register(CommandObject commandObject)
    {
        User user = (User) commandObject.getPayload();
        CommandObject response = new CommandObject();

        if (user == null)
        {
            response.setCommand(Command.S2C_LOGIN_NACK);
            response.setPayload(USER_NOT_FOUND);
            sendResponseAsync(response);
        }

        User userInDb = userService.findByUserName(user.getUserName()).get();

        if (userInDb == null)
        {
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(4)));
            response.setCommand(Command.S2C_REGISTER_ACK);
            userService.insert(user).get();

            response.setPayload(ObjectMapper.<User, UserDto>map(user));

            sendResponseAsync(response);

        } else
        {
            response.setCommand(Command.S2C_REGISTER_NACK);
            response.setPayload("User already exist please take another username");
            sendResponseAsync(response);
        }
    }

//    public UserDto mapUserToUserDto(User user)
//    {
//
//        val userDto = ObjectMapper.<User, UserDto>map(user);
//        val friendDtos = user.getFriendships().stream()
//                .map(f -> ObjectMapper.<User, FriendDto>map(f.getPartner()))
//                .collect(Collectors.toList());
//        val privateMessageDtos = user.getMessages()
//                .stream()
//                .map(m -> {
//                    val messageDto = ObjectMapper.<PrivateMessage, PrivateMessageDto>map(m);
//                    messageDto.setReceiver(userDto);
//                    messageDto.setSender(ObjectMapper.<User, UserDto>map(m.getSender()));
//
//                    return messageDto;
//                }).collect(Collectors.toList());
//
//        val groupDtos = user.getGroups()
//                .stream()
//                .map(g-> ObjectMapper.<Group, GroupDto>map(g))
//                .collect(Collectors.toList());
//
//
//    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command)
    {
        if (command == Command.C2S_LOGIN)
        {
            return Optional.of(this::login);
        } else if (command == Command.C2S_REGISTER)
        {
            return Optional.of(this::register);
        }

        return Optional.empty();
    }

}

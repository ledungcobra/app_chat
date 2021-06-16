package server.handler;

import common.dto.*;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import server.context.SApplicationContext;
import server.entities.User;
import server.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

import static common.dto.Command.S2C_LOGIN_NACK;
import static common.dto.Command.S2C_REGISTER_NACK;
import static server.context.SApplicationContext.configScreen;
import static server.context.SApplicationContext.currentUsers;
import static utils.Constants.USER_NOT_FOUND;
import static utils.Constants.USRNAME_PWD_FAIL;

@Log4j(topic = "LOG")
public class AuthRequestHandler extends RequestHandler {
    private UserService userService = SApplicationContext.userService;


    public AuthRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {
        if (command == Command.C2S_LOGIN) {
            return Optional.of(this::login);
        } else if (command == Command.C2S_REGISTER) {
            return Optional.of(this::register);
        } else if (command == Command.C2S_LOGOUT) {
            return Optional.of(this::logout);

        }

        return Optional.empty();
    }

    private void logout(CommandObject commandObject) {

        synchronized (currentUsers) {
            currentUsers.remove(socket);
            configScreen.updateOnlineList(currentUsers);
        }
    }

    private void login(CommandObject commandObject) {
        try {


            UserAuthDto userAuthDto = (UserAuthDto) commandObject.getPayload();
            CommandObject response = new CommandObject();

            if (userAuthDto == null) {
                sendResponse(new CommandObject(Command.S2C_LOGIN_NACK, USER_NOT_FOUND));
//            sendResponse(new CommandObject(Command.S2C_EXIT));
                return;
            }

            // Wait for database
            User userInDb = userService.findByUserNameAsync(userAuthDto.getUserName()).get();

            if (userInDb == null) {
                sendResponseAsync(new CommandObject(Command.S2C_LOGIN_NACK, USER_NOT_FOUND));

//            CommandObject exitCommand = new CommandObject();
//            exitCommand.setCommand(Command.S2C_EXIT);
//            sendResponseAsync(exitCommand);

            } else {
                if (userInDb.getUserName().equals(userAuthDto.getUserName()) &&
                        BCrypt.checkpw(userAuthDto.getPassword(), userInDb.getPassword())
                ) {
                    // Success login
                    response.setCommand(Command.S2C_LOGIN_ACK);
                    UserDto userDto = ObjectMapper.<User, UserDto>map(userInDb);

                    response.setPayload(userDto);
                    currentUsers.put(socket, userInDb);
                    configScreen.updateOnlineList(currentUsers);
                } else {
                    response.setCommand(Command.S2C_LOGIN_NACK);
                    response.setPayload(USRNAME_PWD_FAIL);
                }
                sendResponseAsync(response);
            }
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_LOGIN_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void register(CommandObject commandObject) {
        try {
            UserAuthDto userAuthDto = (UserAuthDto) commandObject.getPayload();
            CommandObject response = new CommandObject();

            if (userAuthDto == null) {
                response.setCommand(Command.S2C_LOGIN_NACK);
                response.setPayload(USER_NOT_FOUND);
                sendResponseAsync(response);
                return;
            }

            User userInDb = userService.findByUserNameAsync(userAuthDto.getUserName()).get();

            if (userInDb == null) {
                userAuthDto.setPassword(BCrypt.hashpw(userAuthDto.getPassword(), BCrypt.gensalt(4)));
                response.setCommand(Command.S2C_REGISTER_ACK);
                User user = ObjectMapper.map(userAuthDto);
                userService.insert(user).get();

                response.setPayload(ObjectMapper.<User, UserDto>map(user));
                sendResponseAsync(response);

            } else {
                response.setCommand(S2C_REGISTER_NACK);
                response.setPayload("User already exist please take another username");
                sendResponseAsync(response);

            }
        } catch (Exception e) {
            log.debug(e);
            sendResponseAsync(new CommandObject(S2C_REGISTER_NACK));
        }

    }


}

package server.handler;

import common.dto.*;
import lombok.val;
import server.context.SApplicationContext;
import server.entities.Group;
import server.entities.User;
import server.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GetListRequestHandler extends RequestHandler
{
    private UserService userService = SApplicationContext.userService;

    public GetListRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        super(objectInputStream, objectOutputStream, socket);
    }

    /**
     * @param commandObject Response S2C_USER_NOT_FOUND | List<FriendDto>
     */
    private void getFriendListByUserId(CommandObject commandObject)
    {
        User user = (User) userService.findById((Long) commandObject.getPayload());
        if (user == null)
        {
            sendResponse(new CommandObject(Command.S2C_USER_NOT_FOUND));
            return;
        }

        val friendDtos = user.getFriendships()
                .stream()
                .map(f -> ObjectMapper.<User, FriendDto>map(f.getPartner()))
                .collect(Collectors.toList());

        sendResponse(new CommandObject(Command.S2C_GET_FRIEND_LIST_ACK, friendDtos));
    }

    private void getGroupListByUserId(CommandObject commandObject)
    {
        User user = (User) userService.findById((Long) commandObject.getPayload());

        if (user == null)
        {
            sendResponse(new CommandObject(Command.S2C_USER_NOT_FOUND));
            return;
        }

        val groupDtos = user.getGroups()
                .stream()
                .map(g -> ObjectMapper.<Group, GroupDto>map(g)).collect(Collectors.toList());
        sendResponse(new CommandObject(Command.S2C_GET_GROUP_LIST_ACK, groupDtos));

    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command)
    {

        if (command.equals(Command.C2S_GET_FRIEND_LIST))
        {
            return Optional.of(this::getFriendListByUserId);
        } else if (command.equals(Command.C2S_GET_GROUP_LIST))
        {
            return Optional.of(this::getGroupListByUserId);
        }

        return Optional.empty();
    }
}

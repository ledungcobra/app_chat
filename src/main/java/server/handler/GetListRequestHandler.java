package server.handler;

import common.dto.*;
import lombok.val;
import server.context.SApplicationContext;
import server.entities.FriendOffer;
import server.entities.Group;
import server.entities.User;
import server.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.currentUsers;

public class GetListRequestHandler extends RequestHandler
{
    private UserService userService = SApplicationContext.userService;

    public GetListRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        super(objectInputStream, objectOutputStream, socket);
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
        } else if (C2S_FIND_FRIEND_BY_KEYWORD.equals(command))
        {
            return Optional.of(this::getListUserByKeyword);
        } else if (command.equals(C2S_GET_UNSEEN_FRIEND_OFFERS))
        {
            return Optional.of(this::getUnSeenFriendOffers);
        }

        return Optional.empty();
    }

    /**
     * @param commandObject Response S2C_USER_NOT_FOUND | List<FriendDto>
     */
    private void getFriendListByUserId(CommandObject commandObject)
    {

        User user = currentUsers.get(socket);

        if (user == null)
        {
            sendResponse(new CommandObject(Command.S2C_USER_NOT_FOUND));
            return;
        }

        List<FriendDto> friendDtos = null;
        try
        {
            friendDtos = userService.getFriends(user.getId())
                    .get()
                    .stream()
                    .map(ObjectMapper::<User, FriendDto>map)
                    .collect(Collectors.toList());
            sendResponseAsync(new CommandObject(Command.S2C_GET_FRIEND_LIST_ACK, friendDtos));

        } catch (Exception e)
        {
            sendResponseAsync(new CommandObject(Command.S2C_GET_FRIEND_LIST_NACK, e.getMessage()));
            e.printStackTrace();
        }


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

    private void getListUserByKeyword(CommandObject commandObject)
    {
        User user = currentUsers.get(socket);
        String keyword = (String) commandObject.getPayload();

        if (keyword == null || keyword.isEmpty())
        {
            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_NACK, "Keyword cannot be blank"));
            return;
        }

        try
        {
            List<User> users = userService.findUserByKeywordAsync(keyword).get();
            List<FriendDto> friendDtoList = users
                    .stream()
                    .map(ObjectMapper::<User, FriendDto>map)
                    .filter(u -> u.getId() != null && !u.getId().equals(user.getId()))
                    .collect(Collectors.toList());

            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_ACK, friendDtoList));

        } catch (Exception e)
        {
            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_NACK, "An error occur where query data"));
            e.printStackTrace();
        }


    }


    private void getUnSeenFriendOffers(CommandObject commandObject)
    {
        try
        {
            List<FriendOffer> friendOffers = userService.getUnSeenFriendOffersAsync((Long) commandObject.getPayload()).get();
            List<FriendOfferDto> friendOfferDtos = friendOffers.stream().map(f -> ObjectMapper.<FriendOffer, FriendOfferDto>map(f)).collect(Collectors.toList());
            this.sendResponseAsync(new CommandObject(S2C_GET_UNSEEN_FRIEND_OFFERS_ACK, friendOfferDtos));

        } catch (Exception e)
        {
            this.sendResponseAsync(new CommandObject(S2C_GET_UNSEEN_FRIEND_OFFERS_NACK, e.getMessage()));
            e.printStackTrace();
        }

    }
}

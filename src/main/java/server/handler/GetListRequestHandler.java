package server.handler;

import common.dto.*;
import lombok.val;
import server.context.SApplicationContext;
import server.entities.FriendOffer;
import server.entities.Group;
import server.entities.User;
import server.entities.UserPending;
import server.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;

public class GetListRequestHandler extends RequestHandler {
    private UserService userService = SApplicationContext.userService;

    public GetListRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {

        switch (command) {
            case C2S_GET_FRIEND_LIST:
                return Optional.of(this::getFriendListByUserId);
            case C2S_GET_GROUP_LIST:
                return Optional.of(this::getGroupListByUserId);
            case C2S_FIND_FRIEND_BY_KEYWORD:
                return Optional.of(this::getListUserByKeyword);
            case C2S_GET_UNSEEN_FRIEND_OFFERS:
                return Optional.of(this::getUnSeenFriendOffers);
            case C2S_GET_PENDING_USER_GROUP_LIST:
                return Optional.of(this::getListPending);
        }

        return Optional.empty();
    }

    /**
     * @param commandObject Response S2C_USER_NOT_FOUND | List<FriendDto>
     */
    private void getFriendListByUserId(CommandObject commandObject) {

        User user = getCurrentUser();

        if (user == null) {
            sendResponse(new CommandObject(Command.S2C_USER_NOT_FOUND));
            return;
        }

        List<FriendDto> friendDtos = null;
        try {
            friendDtos = userService.getFriends(user.getId())
                    .get()
                    .stream()
                    .map(Mapper::<User, FriendDto>map)
                    .collect(Collectors.toList());
            sendResponseAsync(new CommandObject(Command.S2C_GET_FRIEND_LIST_ACK, friendDtos));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(Command.S2C_GET_FRIEND_LIST_NACK, e.getMessage()));
            e.printStackTrace();
        }


    }

    /**
     * @param commandObject
     */
    private void getGroupListByUserId(CommandObject commandObject) {
        try {
            User user = getCurrentUser();

            if (user == null) {
                sendResponse(new CommandObject(Command.S2C_USER_NOT_FOUND));
                return;
            }

            List<GroupDto> groups = userService.getGroupListByUserId(user.getId())
                    .stream().map(g -> Mapper2.map(g, GroupDto.class)).collect(Collectors.toList());
            sendResponse(new CommandObject(Command.S2C_GET_GROUP_LIST_ACK, groups));
        } catch (Exception exception) {
            exception.printStackTrace();
            sendResponse(new CommandObject(Command.S2C_GET_GROUP_LIST_NACK, exception.getMessage()));

        }

    }

    private void getListUserByKeyword(CommandObject commandObject) {
        User user = getCurrentUser();
        String keyword = (String) commandObject.getPayload();

        if (keyword == null || keyword.isEmpty()) {
            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_NACK, "Keyword cannot be blank"));
            return;
        }

        try {
            List<User> users = userService.findUserByKeywordAsync(keyword).get();
            List<FriendDto> friendDtoList = users
                    .stream()
                    .map(Mapper::<User, FriendDto>map)
                    .filter(u -> u.getId() != null && !u.getId().equals(user.getId()))
                    .collect(Collectors.toList());

            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_ACK, friendDtoList));

        } catch (Exception e) {
            sendResponse(new CommandObject(S2C_FIND_FRIEND_BY_KEYWORD_NACK, "An error occur where query data"));
            e.printStackTrace();
        }


    }


    private void getUnSeenFriendOffers(CommandObject commandObject) {
        try {
            List<FriendOffer> friendOffers = userService.getUnSeenFriendOffersAsync((Long) commandObject.getPayload()).get();
            List<FriendOfferDto> friendOfferDtos = friendOffers.stream().map(f -> Mapper.<FriendOffer, FriendOfferDto>map(f)).collect(Collectors.toList());
            this.sendResponseAsync(new CommandObject(S2C_GET_UNSEEN_FRIEND_OFFERS_ACK, friendOfferDtos));

        } catch (Exception e) {
            this.sendResponseAsync(new CommandObject(S2C_GET_UNSEEN_FRIEND_OFFERS_NACK, e.getMessage()));
            e.printStackTrace();
        }

    }

    /**
     * payload is group id
     * <p>
     * List<UserPendingDto>
     *
     * @param commandObject
     */
    private void getListPending(CommandObject commandObject) {
        try {
            Long groupId = (Long) commandObject.getPayload();
            if (groupId == null) {
                sendResponseAsync(new CommandObject(S2C_GET_PENDING_USER_GROUP_LIST_NACK, "Invalid payload"));
                return;
            }
            List<UserPending> userPendingDtos = userService.getPendingList(groupId);
            List<UserPendingDto> result = userPendingDtos.stream().map(u -> {
                return new UserPendingDto(u.getId(), u.getUser().getId(), u.getUser().getDisplayName(), u.getGroup().getId());
            }).collect(Collectors.toList());
            sendResponseAsync(new CommandObject(S2C_GET_PENDING_USER_GROUP_LIST_ACK, result));
        } catch (Exception exception) {
            sendResponseAsync(new CommandObject(S2C_GET_PENDING_USER_GROUP_LIST_NACK, exception.getMessage()));
            exception.printStackTrace();
        }
    }
}
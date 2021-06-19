package server.handler;

import common.dto.*;
import server.context.SApplicationContext;
import server.entities.FriendOffer;
import server.entities.User;
import utils.SocketExtension;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.currentUsers;
import static server.context.SApplicationContext.userService;
import static server.core.TCPServer.objectOutputStreamMap;

public class FriendRequestHandler extends RequestHandler {

    public FriendRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {

        if (command.equals(C2S_SEND_ADD_FRIEND_OFFER_TO_FRIENDS)) {
            return Optional.of(this::addFriends);
        } else if (command.equals(C2S_SEND_ACCEPT_FRIEND_OFFERS)) {
            return Optional.of(this::acceptFriends);
        } else if (command.equals(C2S_SEND_IGNORE_FRIEND_OFFERS)) {
            return Optional.of(this::ignoreFriends);
        } else if (command.equals(C2S_SEND_UNFRIEND_REQUEST)) {
            return Optional.of(this::unFriends);
        }
        return Optional.empty();
    }

    private void unFriends(CommandObject commandObject) {
        User user = getCurrentUser();
        if (user == null) {
            sendResponseAsync(new CommandObject(S2C_SEND_UNFRIEND_REQUEST_NACK, "Cannot find this user"));
            return;
        }

        List<Long> friendIds = (List<Long>) commandObject.getPayload();
        try {
            userService.unFriendsAsync(user.getId(), friendIds).get();
            sendResponseAsync(new CommandObject(S2C_SEND_UNFRIEND_REQUEST_ACK));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_UNFRIEND_REQUEST_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void ignoreFriends(CommandObject commandObject) {
        List<FriendOfferDto> friendOfferDtos = (List<FriendOfferDto>) commandObject.getPayload();
        if (friendOfferDtos == null) {
            sendResponseAsync(new CommandObject(S2C_SEND_IGNORE_FRIEND_OFFERS_NACK, "Pay load is empty"));
            return;
        }

        List<FriendOffer> friendOffers = friendOfferDtos.stream().map(f -> {
            FriendOffer friendOffer = new FriendOffer();
            friendOffer.setAccepted(f.getAccepted());
            friendOffer.setId(f.getId());
            return friendOffer;
        }).collect(Collectors.toList());

        try {
            userService.ignoreFriendOffersAsync(friendOffers).get();
            sendResponseAsync(new CommandObject(S2C_SEND_IGNORE_FRIEND_OFFERS_ACK));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_IGNORE_FRIEND_OFFERS_NACK, e.getMessage()));
            e.printStackTrace();
        }

    }

    private void acceptFriends(CommandObject commandObject) {
        List<FriendOfferDto> friendOfferDtos = (List<FriendOfferDto>) commandObject.getPayload();
        if (friendOfferDtos == null) {
            sendResponseAsync(new CommandObject(S2C_SEND_ACCEPT_FRIEND_OFFERS_NACK, "Payload is empty"));
            return;
        }

        List<FriendOffer> friendOffers = friendOfferDtos.stream().map(f -> {
            FriendOffer friendOffer = new FriendOffer();
            friendOffer.setAccepted(f.getAccepted());
            friendOffer.setId(f.getId());
            return friendOffer;
        }).collect(Collectors.toList());

        try {
            userService.acceptFriendsAsync(friendOffers).get();
            sendResponseAsync(new CommandObject(S2C_SEND_ACCEPT_FRIEND_OFFERS_ACK));

            for (FriendOffer friendOffer : friendOffers) {
                User friend = userService.getUserByFriendOfferId(friendOffer.getId());
                SocketExtension.sendResponseToSocket(getFriendSocket(friend).orElse(null),
                        new CommandObject(Command.S2C_NOTIFY_NEW_FRIEND, Mapper.<User, FriendDto>map(getCurrentUser())));
            }
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_ACCEPT_FRIEND_OFFERS_NACK, e.getMessage()));
            e.printStackTrace();
        }

    }

    /**
     * This method is use to add new friend send them friend offer waiting for them to accept or reject
     *
     * @param commandObject
     */
    private void addFriends(CommandObject commandObject) {

        Long[] friendIds = (Long[]) commandObject.getPayload();

        User user = getCurrentUser();

        Set<FriendOffer> friendOffers = Arrays.stream(friendIds).map(id -> {

            User partner = new User();
            partner.setId(id);
            FriendOffer friendOffer = new FriendOffer();
            friendOffer.setOwner(user);

            friendOffer.setDisplayName(user.getDisplayName());

            friendOffer.setPartner(partner);
            return friendOffer;
        }).collect(Collectors.toSet());


        try {
            userService.saveFriendOffersAsync(friendOffers).get();

            // Notify the friend via their socket associate with them
            friendOffers.forEach(f -> {
                if (currentUsers.containsValue(f.getPartner())) {
                    currentUsers.entrySet().parallelStream()
                            .filter(e -> e.getValue().equals(f.getPartner()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .ifPresent(v -> {
                                // Send notification to user in the app chat
                                notifyUserAsync(f, v);
                            });
                }

            });
            sendResponseAsync(new CommandObject(S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_ACK));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_FAIL, e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * This method is used to send friend offer to client if they are connecting to the server
     *
     * @param friendOffer
     * @param friendUserSocket
     * @return
     */
    public Future<Boolean> notifyUserAsync(FriendOffer friendOffer, Socket friendUserSocket) {
        return SApplicationContext.service.submit(() -> {
            if (friendUserSocket == null) return false;

            try {
                final ObjectOutputStream anotherUserStream = objectOutputStreamMap.get(friendUserSocket);
                synchronized (anotherUserStream) {
                    anotherUserStream.writeObject(new CommandObject(S2C_NOTIFY_NEW_FRIEND_OFFER,
                            Mapper.<FriendOffer, FriendOfferDto>map(friendOffer)));
                    anotherUserStream.flush();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
}

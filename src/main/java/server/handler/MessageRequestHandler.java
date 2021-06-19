package server.handler;

import common.dto.*;
import server.entities.GroupMessage;
import server.entities.PrivateMessage;
import server.entities.User;
import utils.SocketExtension;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.*;
import static server.core.TCPServer.objectOutputStreamMap;

public class MessageRequestHandler extends RequestHandler {
    static Map<String, String> emojis = new HashMap<>();

    static {
        String color = " rgb(255,200,61)";

        emojis.put(":D", "<span style='font-size:30px; color: " + color + ";'>&#128513;</span>");
        emojis.put(":-)", "<span style='font-size:30px;color: " + color + ";'>&#128512;</span>");
        emojis.put(":)", "<span style='font-size:30px;color: " + color + ";'>&#128512;</span>");
        emojis.put("=)", "<span style='font-size:30px;color: " + color + ";'>&#128522;</span>");
        emojis.put("T_T", "<span style='font-size:30px;color: " + color + ";'>&#128557;</span>");
        emojis.put(":-P", "<span style='font-size:30px;color: " + color + ";'>&#128523;</span>");
        emojis.put(":(", "<span style='font-size:30px;color: " + color + ";'>&#128543;</span>");
        emojis.put(":|", "<span style='font-size:30px;color: " + color + ";'>&#128528;</span>");
    }

    public MessageRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {

        if (command == C2S_SEND_PRIVATE_MESSAGE) {
            return Optional.of(this::sendPrivateMessage);
        } else if (command == C2S_GET_PRIVATE_MESSAGES) {
            return Optional.of(this::getPrivateMessage);
        } else if (command.equals(C2S_GET_GROUP_MESSAGES)) {
            return Optional.of(this::getGroupMessages);
        } else if (command.equals(C2S_SEND_GROUP_MESSAGE)) {
            return Optional.of(this::sendGroupMessage);
        }

        return Optional.empty();
    }

    /**
     * payload is GroupMessageDto
     *
     * @param commandObject
     */
    private void sendGroupMessage(CommandObject commandObject) {
        try {
            GroupMessageDto groupMessageDto = (GroupMessageDto) commandObject.getPayload();
            if (groupMessageDto == null) {
                sendResponseAsync(new CommandObject(S2S_SEND_GROUP_MESSAGE_NACK, "Payload is invalid"));
                return;
            }

            groupMessageDto.setContent(processingMessage(groupMessageDto.getContent()));
            Long previousMessageId = groupMessageDto.getId();
            if (groupMessageDto == null) {
                sendResponseAsync(new CommandObject(Command.S2S_SEND_GROUP_MESSAGE_NACK, "Invalid payload"));
                return;
            }

            groupMessageDto.setId(null);
            Long senderId = groupMessageDto.getSender().getId();
            Long groupId = groupMessageDto.getGroupReceiver().getId();
            String content = groupMessageDto.getContent();

            GroupMessage groupMessage = userService.addGroupMessage(senderId, groupId, content, previousMessageId);

            GroupMessageDto dto = new GroupMessageDto(groupMessage.getId(), groupMessage.getContent(),
                    Mapper.map(getCurrentUser()), Mapper.map(groupMessage.getGroupReceiver())
            );

            dto.setGroupReceiver(Mapper.map(groupMessage.getGroupReceiver()));

            List<User> users = userService.getAllMembers(groupId);
            users.forEach(u -> {
                if (!u.equals(getCurrentUser())) {
                    SocketExtension.sendResponseToSocket(getFriendSocket(u).orElse(null), new CommandObject(S2C_RECEIVE_A_GROUP_MESSAGE, dto));
                }
            });
            sendResponseAsync(new CommandObject(S2S_SEND_GROUP_MESSAGE_ACK, dto));

        } catch (Exception exception) {
            sendResponseAsync(new CommandObject(Command.S2S_SEND_GROUP_MESSAGE_NACK, exception.getMessage()));
        }
    }

    /**
     * [groupId, numberOfMessages]
     *
     * @param commandObject
     */
    private void getGroupMessages(CommandObject commandObject) {
        try {
            Long[] idx = (Long[]) commandObject.getPayload();
            if (idx == null || idx.length < 2) {
                sendResponseAsync(new CommandObject(S2C_GET_GROUP_MESSAGES_NACK, "Invalid payload"));
                return;
            }

            Long groupId = idx[0];
            Long numberOfMessages = idx[1];
            List<GroupMessage> messages = userService.getMessages(groupId, numberOfMessages);
            List<GroupMessageDto> messageDtos = messages.stream()
                    .map(m -> Mapper2.map(m, GroupMessageDto.class))
                    .collect(Collectors.toList());

            sendResponseAsync(new CommandObject(S2C_GET_GROUP_MESSAGES_ACK, new Object[]{messageDtos, groupId}));
        } catch (Exception exception) {
            exception.printStackTrace();
            sendResponseAsync(new CommandObject(S2C_GET_GROUP_MESSAGES_NACK, exception.getMessage()));
        }
    }

    private void getPrivateMessage(CommandObject commandObject) {
        try {
            RequestPrivateMessageDto messageRequest = (RequestPrivateMessageDto) commandObject.getPayload();

            List<PrivateMessage> privateMessageList = userService.
                    getPrivateMessage(messageRequest.getOwnerId(),
                            messageRequest.getFriendId(),
                            messageRequest.getNumberOfMessages(),
                            messageRequest.getOffset()).get();
            User friend = userService.findById(messageRequest.getFriendId()).get();
            List<PrivateMessageDto> privateMessageDtos = privateMessageList.stream()
                    .map(p -> {
                        PrivateMessageDto privateMessageDto = new PrivateMessageDto();
                        privateMessageDto.setReceiver(Mapper.map(p.getReceiver()));
                        privateMessageDto.setSender(Mapper.map(p.getSender()));
                        privateMessageDto.setContent(p.getContent());
                        privateMessageDto.setId(p.getId());
                        return privateMessageDto;
                    }).collect(Collectors.toList());

            sendResponseAsync(new CommandObject(S2C_GET_PRIVATE_MESSAGES_ACK, new ResponseMessageDto(Mapper.map(friend), privateMessageDtos)));
        } catch (InterruptedException | ExecutionException e) {
            sendResponseAsync(new CommandObject(S2C_GET_PRIVATE_MESSAGES_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage(CommandObject commandObject) {
        try {
            PrivateMessageDto messageDto = (PrivateMessageDto) commandObject.getPayload();

            messageDto.setContent(processingMessage(messageDto.getContent()));

            PrivateMessage privateMessage = userService.addMessageAsync(messageDto.getSender().getId(),
                    messageDto.getReceiver().getId(), messageDto.getContent(), messageDto.getId()).get();

            PrivateMessageDto savedMessageDto = new PrivateMessageDto();
            savedMessageDto.setReceiver(Mapper.map(privateMessage.getReceiver()));
            savedMessageDto.setSender(Mapper.map(privateMessage.getSender()));
            savedMessageDto.setId(privateMessage.getId());
            savedMessageDto.setContent(privateMessage.getContent());
            sendResponseAsync(new CommandObject(S2C_SEND_PRIVATE_MESSAGE_ACK, savedMessageDto));
            notifyToFriend(messageDto.getReceiver().getId(), savedMessageDto);

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_PRIVATE_MESSAGE_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

    public void notifyToFriend(Long friendId, PrivateMessageDto messageDto) {
        service.submit(() -> {
            currentUsers.entrySet().stream().filter((e) -> e.getValue().getId().equals(friendId))
                    .findFirst().ifPresent(e -> {
                try {
                    final ObjectOutputStream anotherUserStream = objectOutputStreamMap.get(e.getKey());
                    synchronized (anotherUserStream) {
                        anotherUserStream.writeObject(new CommandObject(S2C_RECEIVE_A_PRIVATE_MESSAGE, messageDto));
                        anotherUserStream.flush();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });
        });
    }

    private static String processingMessage(String content) {
        if (content == null) return "";
        for (Map.Entry<String, String> e : emojis.entrySet()) {
            content = content.replace(e.getKey(), e.getValue());
        }
        return content;
    }
}

package server.handler;

import common.dto.*;
import server.entities.GroupMessage;
import server.entities.PrivateMessage;
import server.entities.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.*;
import static server.core.TCPServer.objectOutputStreamMap;

public class MessageRequestHandler extends RequestHandler {
    static Map<String, String> emojis = new HashMap<>();

    static {
        emojis.put(":D", "\uD83D\uDE01");
        emojis.put(":-)", "\uD83D\uDE0A");
        emojis.put("=)", "\uD83D\uDE0A");
        emojis.put("^_^", "\uD83D\uDE0A");
        emojis.put(":-P", "\uD83D\uDE0B");
        emojis.put(":(", "\uD83D\uDE22");
        emojis.put(":kiss", "\uD83D\uDE1A");
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
     * payload is [GroupMessageDto, previousMessageId]
     *
     * @param commandObject
     */
    private void sendGroupMessage(CommandObject commandObject) {
        try {
            Object[] payload = (Object[]) commandObject.getPayload();
            if (payload == null) {
                sendResponseAsync(new CommandObject(S2S_SEND_GROUP_MESSAGE_NACK, "Payload is invalid"));
                return;
            }
            GroupMessageDto groupMessageDto = (GroupMessageDto) payload[0];

            groupMessageDto.setContent(processingMessage(groupMessageDto.getContent()));
            Long previousMessageId = (Long) payload[1];
            if (groupMessageDto == null) {
                sendResponseAsync(new CommandObject(Command.S2S_SEND_GROUP_MESSAGE_NACK, "Invalid payload"));
                return;
            }

            GroupMessage groupMessage = userService.addGroupMessage(Mapper2.map(groupMessageDto, GroupMessage.class), previousMessageId);
            sendResponseAsync(new CommandObject(S2S_SEND_GROUP_MESSAGE_ACK, Mapper2.map(groupMessage, GroupMessageDto.class)));
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

            sendResponseAsync(new CommandObject(S2C_GET_GROUP_MESSAGES_ACK, messageDtos));
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

            sendResponseAsync(new CommandObject(S2C_GET_PRIVATE_MESSAGES_ACK, new ResponsePrivateMessageDto(Mapper.map(friend), privateMessageDtos)));
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

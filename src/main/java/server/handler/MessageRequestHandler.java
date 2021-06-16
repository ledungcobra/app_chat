package server.handler;

import common.dto.*;
import server.entities.PrivateMessage;
import server.entities.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.*;
import static server.context.SApplicationContext.*;
import static server.core.TCPServer.objectOutputStreamMap;

public class MessageRequestHandler extends RequestHandler {

    public MessageRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {

        if (command == C2S_SEND_PRIVATE_MESSAGE) {
            return Optional.of(this::sendPrivateMessage);
        } else if (command == C2S_GET_PRIVATE_MESSAGES) {
            return Optional.of(this::getPrivateMessage);
        }

        return Optional.empty();
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
                        privateMessageDto.setReceiver(ObjectMapper.map(p.getReceiver()));
                        privateMessageDto.setSender(ObjectMapper.map(p.getSender()));
                        privateMessageDto.setContent(p.getContent());
                        privateMessageDto.setId(p.getId());
                        return privateMessageDto;
                    }).collect(Collectors.toList());

            sendResponseAsync(new CommandObject(S2C_GET_PRIVATE_MESSAGES_ACK, new ResponsePrivateMessageDto(ObjectMapper.map(friend), privateMessageDtos)));
        } catch (InterruptedException | ExecutionException e) {
            sendResponseAsync(new CommandObject(S2C_GET_PRIVATE_MESSAGES_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage(CommandObject commandObject) {
        try {
            PrivateMessageDto messageDto = (PrivateMessageDto) commandObject.getPayload();
            PrivateMessage privateMessage = userService.addMessageAsync(messageDto.getSender().getId(),
                    messageDto.getReceiver().getId(), messageDto.getContent(), messageDto.getId()).get();

            PrivateMessageDto savedMessageDto = new PrivateMessageDto();
            savedMessageDto.setReceiver(ObjectMapper.map(privateMessage.getReceiver()));
            savedMessageDto.setSender(ObjectMapper.map(privateMessage.getSender()));
            savedMessageDto.setId(privateMessage.getId());
            savedMessageDto.setContent(privateMessage.getContent());
            sendResponseAsync(new CommandObject(S2C_PRIVATE_MESSAGE, savedMessageDto));
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
                        anotherUserStream.writeObject(new CommandObject(S2C_PRIVATE_MESSAGE, messageDto));
                        anotherUserStream.flush();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });
        });
    }

}

package server.handler;

import common.dto.*;
import server.entities.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

import static common.dto.Command.*;
import static common.dto.Mapper.*;
import static utils.SocketExtension.*;

public class CallRequestHandler extends RequestHandler {
    public CallRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {
        switch (command) {
            case C2S_MAKE_A_VOICE_CALL:
                return Optional.of(this::makeAVoiceCall);
            case C2S_ACCEPT_A_VOICE_CALL:
                return Optional.of(this::acceptAVoiceCall);
            case C2S_DECLINE_A_VOICE_CALL:
                return Optional.of(this::declineAVoiceCall);
            case C2S_SEND_VOICE_CHUNK:
                return Optional.of(this::sendAVoiceChunk);
            case C2S_MUTE_VOICE:
                return Optional.of(this::muteVoice);
            case C2S_UNMUTE_VOICE:
                return Optional.of(this::unmuteVoice);
            case C2S_STOP_VOICE_CHAT:
                return Optional.of(this::stopVoiceChat);
        }
        return Optional.empty();
    }

    private void stopVoiceChat(CommandObject commandObject) {
        sendSignalToFriend(commandObject, S2C_STOP_VOICE_CHAT_ACK, S2C_STOP_VOICE_CHAT_NACK);
    }

    private void unmuteVoice(CommandObject commandObject) {
        sendSignalToFriend(commandObject, S2C_UNMUTE_VOICE_ACK, S2C_UNMUTE_VOICE_NACK);
    }

    private void muteVoice(CommandObject commandObject) {
        sendSignalToFriend(commandObject, S2C_MUTE_VOICE_ACK, S2C_MUTE_VOICE_NACK);
    }

    private void sendSignalToFriend(CommandObject commandObject, Command ackSignal, Command nackSignal) {
        try {
            FriendDto friendDto = (FriendDto) commandObject.getPayload();
            if (friendDto == null) {
                sendResponseAsync(new CommandObject(nackSignal, "Friend object cannot be null"));
                return;
            }

            sendResponseToSocket(getFriendSocket(
                    map(friendDto))
                            .orElseThrow(() -> new RuntimeException("Your friend is offline")),
                    new CommandObject(ackSignal));

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                sendResponseAsync(new CommandObject(nackSignal, e.getCause().getMessage()));
            } else {
                sendResponseAsync(new CommandObject(nackSignal, e.getMessage()));
            }
            e.printStackTrace();
        }
    }

    private void sendAVoiceChunk(CommandObject commandObject) {
        try {
            SenderVoiceData voiceData = (SenderVoiceData) commandObject.getPayload();
            if (voiceData == null) {
                sendResponseAsync(new CommandObject(S2C_SEND_VOICE_CHUNK_NACK, "You sent an invalid data!! reason data is null"));
                return;
            }

            Socket friendSocket = getFriendSocket(Mapper.map(voiceData.getReceiver()))
                    .orElseThrow(() -> new RuntimeException("You friend is offline"));

            sendResponseToSocket(friendSocket, new CommandObject(S2C_RECEIVE_VOICE_CHUNK,
                    new ReceiverVoiceData(voiceData.getData(), Mapper.map(getCurrentUser()))));
        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_VOICE_CHUNK_NACK, "Your friend is offline"));
        }


    }

    private void declineAVoiceCall(CommandObject commandObject) {
        sendSignalToFriend(commandObject, S2C_FRIEND_DECLINE_A_VOICE_CALL, S2C_DECLINE_A_VOICE_CALL_NACK);
    }

    private void acceptAVoiceCall(CommandObject commandObject) {
        sendSignalToFriend(commandObject, S2C_FRIEND_ACCEPT_A_VOICE_CALL, S2C_ACCEPT_A_VOICE_CALL_NACK);
        sendResponseAsync(new CommandObject(S2C_START_A_VOICE_CALL));

    }

    private void makeAVoiceCall(CommandObject commandObject) {
        try {
            FriendDto friend = (FriendDto) commandObject.getPayload();

            if (friend == null) {
                sendResponseAsync(new CommandObject(Command.S2C_MAKE_A_VOICE_CALL_NACK, "You must supply friend object to continue"));
                return;
            }

            Socket friendSocket = getFriendSocket(map(friend)).orElseThrow(() -> new RuntimeException("You friend is offline"));
            sendResponseToSocket(friendSocket, new CommandObject(Command.S2C_RECEIVE_A_PHONE_CALL,
                    Mapper.<User, FriendDto>map(getCurrentUser())));
            sendResponseAsync(new CommandObject(S2C_MAKE_A_VOICE_CALL_ACK));

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_MAKE_A_VOICE_CALL_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }

}

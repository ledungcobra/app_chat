package server.handler;

import common.dto.Command;
import common.dto.CommandObject;
import common.dto.FriendDto;
import common.dto.SendFileRequestDto;
import server.entities.User;
import utils.SocketExtension;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static common.dto.Command.*;
import static server.context.SApplicationContext.currentUsers;
import static server.context.SApplicationContext.service;

public class FileRequestHandler extends RequestHandler {

    public static final int MAX_FILESIZE = 1024;

    public FileRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {
        if (command.equals(S2S_SEND_PRIVATE_FILE)) {
            return Optional.of(this::sendSendPrivateFile);
        }
        return Optional.empty();
    }

    private void sendSendPrivateFile(CommandObject commandObject) {
        try {
            SendFileRequestDto fileRequestDto = (SendFileRequestDto) commandObject.getPayload();

            if (fileRequestDto.fileSize > MAX_FILESIZE) {
                sendResponseAsync(new CommandObject(S2S_SEND_PRIVATE_FILE_NACK, "The file size cannot exceed " + MAX_FILESIZE + " MB"));
                return;
            }
            Socket friendSocket = null;
            Set<Map.Entry<Socket, User>> entrySet = currentUsers.entrySet();
            for (Map.Entry<Socket, User> entry : entrySet) {
                if (entry.getValue().getId().equals(
                        ((FriendDto) (fileRequestDto.receiver)).getId())
                ) {
                    friendSocket = entry.getKey();
                    break;
                }
            }

            if (friendSocket == null) {
                sendResponseAsync(new CommandObject(S2S_SEND_PRIVATE_FILE_NACK, "Your friend is offline"));
                return;
            }

            service.submit(() -> {
                try {
                    SocketExtension.sendResponseToSocket(socket, new CommandObject(S2C_RECEIVE_FILE, fileRequestDto));
                    sendResponse(new CommandObject(S2S_SEND_PRIVATE_FILE_ACK, "Send file success"));
                } catch (Exception e) {
                    sendResponse(new CommandObject(S2C_SEND_PRIVATE_MESSAGE_NACK, e.getMessage()));
                }

            });

        } catch (Exception e) {
            sendResponseAsync(new CommandObject(S2C_SEND_PRIVATE_MESSAGE_NACK, e.getMessage()));
            e.printStackTrace();
        }
    }


}

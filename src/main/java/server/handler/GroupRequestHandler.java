package server.handler;

import common.dto.Command;
import common.dto.CommandObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

public class GroupRequestHandler extends RequestHandler {
    public GroupRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket) {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command) {
        if (command.equals(Command.C2S_FIND_GROUP_BY_KEYWORD)) {
            return Optional.of(this::findGroupByKeyword);
        } else if (command.equals(Command.C2S_SEND_JOIN_GROUPS)) {
            return Optional.of(this::joinGroups);
        }
        return Optional.empty();
    }

    private void joinGroups(CommandObject commandObject) {
    }

    private void findGroupByKeyword(CommandObject commandObject) {

    }
}

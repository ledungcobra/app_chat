package server.handler;

import common.dto.Command;
import common.dto.CommandObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

public class PingRequestHandler extends RequestHandler
{

    public PingRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        super(objectInputStream, objectOutputStream, socket);
    }

    private void pingOk(CommandObject commandObject)
    {

        sendResponseAsync(new CommandObject(Command.S2C_PING_OK));
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command)
    {
        if (command.equals(Command.C2S_PING))
        {
            return Optional.of(this::pingOk);
        }
        return Optional.empty();
    }
}

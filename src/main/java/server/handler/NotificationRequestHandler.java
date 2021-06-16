package server.handler;

import common.dto.Command;
import common.dto.CommandObject;
import common.dto.NotificationDto;
import common.dto.ObjectMapper;
import server.entities.Notification;
import server.entities.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.dto.Command.C2S_MARK_SEEN_NOTIFICATIONS;
import static server.context.SApplicationContext.userService;

public class NotificationRequestHandler extends RequestHandler
{

    public NotificationRequestHandler(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Socket socket)
    {
        super(objectInputStream, objectOutputStream, socket);
    }

    @Override
    public Optional<Consumer<CommandObject>> getHandle(Command command)
    {
        if (command.equals(Command.C2S_GET_UNSEEN_NOTIFICATIONS))
        {
            return Optional.of(this::getUnseenNotifications);
        } else if (command.equals(C2S_MARK_SEEN_NOTIFICATIONS))
        {
            return Optional.of(this::markSeenNotifications);
        }
        return Optional.empty();
    }

    private void markSeenNotifications(CommandObject commandObject)
    {
    }

    private void getUnseenNotifications(CommandObject commandObject)
    {
        Long userId = (Long) commandObject.getPayload();
        try
        {
            // TODO:
            User user = userService.findById(userId).get();
            List<NotificationDto> notificationDtoList = userService
                    .getNotifications(userId)
                    .get()
                    .stream()
                    .filter(n -> n.getIsSeen() == null || !n.getIsSeen())
                    .map(ObjectMapper::<Notification, NotificationDto>map)
                    .collect(Collectors.toList());

            sendResponseAsync(new CommandObject(Command.S2C_GET_UNSEEN_NOTIFICATION_ACK, notificationDtoList));
        } catch (Exception e)
        {
            sendResponseAsync(new CommandObject(Command.S2C_GET_UNSEEN_NOTIFICATION_NACK, e.getMessage()));
            e.printStackTrace();
        }

    }


}

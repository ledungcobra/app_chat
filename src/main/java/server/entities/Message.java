package server.entities;

import common.dto.Receiver;
import common.dto.UserDto;

public interface Message {
    Long getId();

    String getContent();

    UserDto getSender();

    Receiver getReceiver();

}

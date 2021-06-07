package client.core;

import common.dto.CommandObject;

public interface ResponseHandler
{
    void listen(CommandObject commandObject);
    void closeHandler();
}

package client.core;

import common.dto.CommandObject;

public interface ResponseHandler
{
    void listenOnNetworkEvent(CommandObject commandObject);
    void closeHandler();
}

package common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.concurrent.Future;

@Getter
@Setter

public class CommandObject implements Serializable
{


    public Command command;
    public Object payload;

    public CommandObject(Command command, Object payload)
    {
        this.command = command;
        this.payload = payload;
    }

    public CommandObject()
    {
    }

    public CommandObject(Command command)
    {
        this.command = command;
    }

    public Command getCommand()
    {
        return command;
    }

    public void setCommand(Command command)
    {
        this.command = command;
    }

    public Object getPayload()
    {
        return payload;
    }

    public void setPayload(Object payload)
    {
        this.payload = payload;
    }


    @Override
    public String toString()
    {
        return "CommandObject{" +
                "command=" + command +
                ", payload=" + payload +
                '}';
    }
}
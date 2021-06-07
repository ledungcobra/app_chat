package common.dto;

import java.io.Serializable;
import java.util.concurrent.Future;

public class CommandObject implements Serializable
{


    public Command command;
    public Object payload;
    public Future<?> result;

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

    public Future<?> getResult()
    {
        return result;
    }

    public void setResult(Future<?> result)
    {
        this.result = result;
    }

    @Override
    public String toString()
    {
        return "CommandObject{" +
                "command=" + command +
                ", payload=" + payload +
                ", result=" + result +
                '}';
    }
}
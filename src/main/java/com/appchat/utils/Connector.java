package com.appchat.utils;

import com.appchat.client.context.CApplicationContext;
import com.appchat.server.dto.Command;
import com.appchat.server.dto.CommandObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.appchat.utils.Constaints.CANNOT_CONNECT_TO_THE_SERVER;

public class Connector
{

    /**
     * @param consumer
     * @param exceptionHandler
     */
    public void connectAsync(Consumer<Boolean> consumer, Consumer<Exception> exceptionHandler)
    {
        CApplicationContext.service.submit(() -> {
            try
            {
                if (CApplicationContext.tcpClient.isActive())
                {
                    consumer.accept(true);
                } else
                {
                    consumer.accept(false);

                    boolean isSuccess = CApplicationContext.tcpClient.tryConnectAsync(Constaints.TRY_COUNT).get();
                    if (isSuccess)
                    {
                        consumer.accept(true);
                    } else
                    {
                        exceptionHandler.accept(new Exception(CANNOT_CONNECT_TO_THE_SERVER));
                    }
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                exceptionHandler.accept(ex);
            }
        });
    }


    public void listenAsync(Command command, Consumer<CommandObject> consumer, Consumer<Exception> exceptionConsumer, long timeout)
    {
        CApplicationContext.service.submit(() -> {
            long startTime = System.currentTimeMillis();
            try
            {
                while (System.currentTimeMillis() - startTime < timeout)
                {

                    CommandObject result = CApplicationContext.tcpClient.receive();
                    if (result.getCommand().equals(command))
                    {
                        consumer.accept(result);
                    }

                }
            } catch (Exception e)
            {
                exceptionConsumer.accept(e);
            }
        });
    }

    /**
     * @param command
     * @param consumer
     * @param exceptionConsumer
     * @return AtomicBoolean handle to stop this listener
     */
    public AtomicBoolean listenAsync(Command command, Consumer<CommandObject> consumer, Consumer<Exception> exceptionConsumer)
    {
        AtomicBoolean isDone = new AtomicBoolean(false);

        CApplicationContext.service.submit(() -> {
            try
            {
                while (!isDone.get())
                {
                    CommandObject result = CApplicationContext.tcpClient.receive();
                    if (result.getCommand().equals(command))
                    {
                        consumer.accept(result);
                    }

                }
            } catch (Exception e)
            {
                exceptionConsumer.accept(e);
                e.printStackTrace();
                isDone.set(true);
            } finally
            {
                isDone.set(true);
            }
        });
        return isDone;
    }

    public void listenAsync(List<Command> commands, Consumer<CommandObject> consumer, Consumer<Exception> exceptionConsumer, long timeout)
    {
        CApplicationContext.service.submit(() -> {
            long startTime = System.currentTimeMillis();
            try
            {
                while (System.currentTimeMillis() - startTime < timeout)
                {
                    CommandObject result = CApplicationContext.tcpClient.receive();
                    if (commands.contains(result.getCommand()))
                    {
                        consumer.accept(result);
                    }

                    System.out.println(result);
                }
            } catch (Exception e)
            {
                exceptionConsumer.accept(e);
            }
        });
    }

}
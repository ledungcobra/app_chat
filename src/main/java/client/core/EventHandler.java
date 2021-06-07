package client.core;

import java.util.function.Consumer;

@FunctionalInterface
public interface EventHandler
{
    void apply( Runnable resolve, Consumer<Exception> failureHandler);
}

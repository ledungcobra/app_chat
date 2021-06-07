package client.core;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Promise<T>
{
    private T result;
    private Exception e;

    public Promise(Callable<T> resolve, Callable<Exception> error) throws Exception
    {
        result = resolve.call();
        e = error.call();
    }

    public Promise<T> then(Consumer<T> func)
    {
        func.accept(result);
        return this;
    }

    public void catchError(Consumer<Exception> func)
    {
        func.accept(e);
    }

}

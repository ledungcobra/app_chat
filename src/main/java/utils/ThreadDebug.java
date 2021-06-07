package utils;

public class ThreadDebug
{
    public static void debugThread(){
        System.out.println("Thread name "+ Thread.currentThread().getName());
        System.out.println("Run in time "+ System.currentTimeMillis());

    }
}

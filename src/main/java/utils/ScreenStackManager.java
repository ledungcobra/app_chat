package utils;

import client.view.AbstractScreen;
import lombok.NonNull;
import lombok.val;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Stack;

public class ScreenStackManager
{

    private static ScreenStackManager INSTANCE;

    public static ScreenStackManager getInstance()
    {

        if (Objects.isNull(INSTANCE))
        {
            INSTANCE = new ScreenStackManager();
        }
        return INSTANCE;
    }

    private Stack<AbstractScreen> screensStack;

    private ScreenStackManager()
    {
        screensStack = new Stack<>();
    }

    public void pushScreen(@NonNull AbstractScreen screen)
    {
        synchronized (screensStack)
        {
            if (!screensStack.isEmpty())
            {
                hideScreen(screensStack.peek());
            }
            screensStack.push(screen);
            showScreen(screen);
        }
    }

    private void showScreen(@NonNull AbstractScreen screen)
    {
        screen.setLocationRelativeTo(null);
        screen.setVisible(true);
    }

    private void hideScreen(@NonNull AbstractScreen screen)
    {
        screen.setVisible(false);
    }

    private void closeScreen(@NonNull AbstractScreen screen)
    {
        WindowEvent winClosingEvent = new WindowEvent(screen, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(winClosingEvent);
    }

    public void forcePopTopScreen()
    {
        synchronized (screensStack)
        {

            if (screensStack.size() > 1)
            {
                val currentScreen = screensStack.pop();
                closeScreen(currentScreen);
            }

            if (!screensStack.isEmpty())
            {
                showScreen(screensStack.peek());
            }
        }
    }

    public void popTopScreen()
    {
        synchronized (screensStack)
        {
            if (screensStack.size() > 1)
            {
                val currentScreen = screensStack.pop();
                hideScreen(currentScreen);
            }

            if (!screensStack.isEmpty())
            {
                showScreen(screensStack.peek());
            }
        }
    }

    public Class getTopScreenClass()
    {
        return screensStack.peek().getClass();
    }

}

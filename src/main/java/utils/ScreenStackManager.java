package utils;

import client.view.AbstractScreen;
import client.view.LoginScreen;
import lombok.NonNull;
import lombok.val;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Stack;

public class ScreenStackManager {

    private static ScreenStackManager INSTANCE;

    public synchronized static ScreenStackManager getInstance() {

        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new ScreenStackManager();
        }
        return INSTANCE;
    }

    private final Stack<AbstractScreen> screensStack;

    private ScreenStackManager() {
        screensStack = new Stack<>();
    }

    public void pushScreen(@NonNull AbstractScreen screen, boolean hideParent) {
        synchronized (screensStack) {
            if (!screensStack.isEmpty() && hideParent) {
                hideScreen(screensStack.peek());
            }
            screensStack.push(screen);
            showScreen(screen);
        }
    }

    public void pushScreen(@NonNull AbstractScreen screen) {
        pushScreen(screen, true);
    }

    private void showScreen(@NonNull AbstractScreen screen) {
        screen.setLocationRelativeTo(null);
        screen.setVisible(true);
    }

    private void hideScreen(@NonNull AbstractScreen screen) {
        screen.setVisible(false);
    }

    private void closeScreen(@NonNull AbstractScreen screen) {
        WindowEvent winClosingEvent = new WindowEvent(screen, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(winClosingEvent);
    }

    public void forcePopTopScreen() {
        synchronized (screensStack) {

            if (screensStack.size() > 1) {
                val currentScreen = screensStack.pop();
                closeScreen(currentScreen);
            }

            if (!screensStack.isEmpty()) {
                showScreen(screensStack.peek());
            }
        }
    }

    public void popTopScreen() {
        synchronized (screensStack) {
            if (screensStack.size() > 1) {
                val currentScreen = screensStack.pop();
                hideScreen(currentScreen);
            }

            if (!screensStack.isEmpty()) {
                showScreen(screensStack.peek());
            }
        }
    }

    public Class getTopScreenClass() {
        return screensStack.peek().getClass();
    }

    public void popTo(Class<LoginScreen> loginScreenClass) {

        try {
            synchronized (screensStack) {
                while (!screensStack.peek().getClass().isAssignableFrom(loginScreenClass)) {
                    AbstractScreen pop = screensStack.pop();
                    pop.setVisible(false);
                    pop.dispose();

                    screensStack.peek().setVisible(true);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

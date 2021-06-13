package utils;

import client.view.AbstractScreen;
import lombok.SneakyThrows;

import java.util.Map;

public class Navigator<T extends AbstractScreen>
{

    private Class<T> clazz;
    private T screen;
    private T parentScreen;


    @SneakyThrows()
    public Navigator(T... t)
    {
        clazz = (Class<T>) t.getClass().getComponentType();
        screen = clazz.newInstance();
    }

    public void navigate(int width, int height, Map<String, Object> data)
    {
        screen.setSize(width, height);
        screen.setData(data);
        screen.onCreateView();
        screen.addEventListener();

        ScreenStackManager.getInstance().pushScreen(screen);
    }

    public void navigate(Map<String, Object> data, boolean hideParent)
    {
        screen.setData(data);
        screen.onCreateView();
        screen.addEventListener();

        ScreenStackManager.getInstance().pushScreen(screen, hideParent);
    }

    public void navigate(Map<String, Object> data)
    {
        navigate(data, true);
    }


    public void navigate()
    {
        screen.onCreateView();
        screen.addEventListener();

        ScreenStackManager.getInstance().pushScreen(screen);
    }
}

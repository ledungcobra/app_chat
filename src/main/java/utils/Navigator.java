package utils;

import client.view.AbstractScreen;
import lombok.SneakyThrows;

import javax.swing.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

public class Navigator<T extends AbstractScreen> {

    private Class<T> clazz;
    private T screen;
    private T parentScreen;


    public Navigator(T... t) {
        clazz = (Class<T>) t.getClass().getComponentType();
        try {
            screen = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void navigate(int width, int height, Map<String, Object> data) {
        screen.setSize(width, height);
        screen.setData(data);
        screen.onCreateView();
        screen.addEventListener();

        addGoBackListener();
        ScreenStackManager.getInstance().pushScreen(screen);
    }

    public void navigate(Map<String, Object> data, boolean hideParent) {
        screen.setData(data);
        screen.onCreateView();
        screen.addEventListener();

        addGoBackListener();
        ScreenStackManager.getInstance().pushScreen(screen, hideParent);
    }

    public void navigate(Map<String, Object> data) {
        navigate(data, true);
    }


    public void navigate() {
        screen.onCreateView();
        screen.addEventListener();

        addGoBackListener();
        ScreenStackManager.getInstance().pushScreen(screen);
    }


    private void addGoBackListener() {
        try {
            for (Field field : screen.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                Annotation[] annotations = field.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation instanceof BackButton) {
                        JButton button = (JButton) field.get(screen);
                        button.addActionListener(e -> ScreenStackManager.getInstance().popTopScreen());
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

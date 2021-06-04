package com.appchat.client;

import com.appchat.client.context.CApplicationContext;
import com.appchat.client.view.LoginScreen;
import com.appchat.utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class CAppRunner
{
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, InvocationTargetException
    {
        Class.forName(CApplicationContext.class.getName());


        SwingUtilities.invokeAndWait(() -> new Navigator<LoginScreen>().navigate());

        CApplicationContext.service.submit(() -> {
            CApplicationContext.tcpClient.connectToServer();
        });


        Thread.currentThread().join();

    }
}

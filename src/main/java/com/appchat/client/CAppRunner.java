package com.appchat.client;

import com.appchat.client.context.CApplicationContext;
import com.appchat.client.view.LoginScreen;
import com.appchat.utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static com.appchat.utils.Constaints.TRY_COUNT;

public class CAppRunner
{
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, InvocationTargetException
    {

        Class.forName(CApplicationContext.class.getName());

        CApplicationContext.tcpClient.tryConnectAsync(TRY_COUNT);
        try
        {
            SwingUtilities.invokeAndWait(() -> new Navigator<LoginScreen>().navigate());
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}

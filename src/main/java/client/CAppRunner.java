package client;


import client.context.CApplicationContext;
import client.view.LoginScreen;
import utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static client.context.CApplicationContext.tcpClient;

public class CAppRunner {
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, InvocationTargetException {
        Class.forName(CApplicationContext.class.getName());
        try {
            SwingUtilities.invokeAndWait(() -> new Navigator<LoginScreen>().navigate());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

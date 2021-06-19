package client;


import client.context.CApplicationContext;
import client.view.LoginScreen;
import utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static client.context.CApplicationContext.tcpClient;

public class CAppRunner {
    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, InvocationTargetException {
        CApplicationContext.start();
        try {

            Map<String, Object> data = new HashMap<>();
            SwingUtilities.invokeAndWait(() -> new Navigator<LoginScreen>().navigate(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

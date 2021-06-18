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
        Class.forName(CApplicationContext.class.getName());
        try {

            Map<String,Object> data = new HashMap<>();
            if(args.length>0){
                System.out.println(Arrays.toString(args));
                data.put("USERNAME",args[0]);
                data.put("PASSWORD",args[1]);
            }
            SwingUtilities.invokeAndWait(() -> new Navigator<LoginScreen>().navigate(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

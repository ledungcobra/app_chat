package server;

import server.view.ServerConfigScreen;
import utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class ServerRunner {
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {

        SwingUtilities.invokeAndWait(() -> new Navigator<ServerConfigScreen>().navigate());
    }

}

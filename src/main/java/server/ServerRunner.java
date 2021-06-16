package server;

import common.dto.ObjectMapper;
import common.dto.UserDto;
import server.context.SApplicationContext;
import server.core.TCPServer;
import server.entities.User;
import server.view.ServerConfigScreen;
import utils.Navigator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

public class ServerRunner {
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {

        SwingUtilities.invokeAndWait(() -> new Navigator<ServerConfigScreen>().navigate());
    }

}

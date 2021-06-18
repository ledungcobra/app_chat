/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.Command;
import common.dto.CommandObject;
import common.dto.UserAuthDto;
import common.dto.UserDto;
import lombok.val;
import lombok.var;
import utils.Navigator;
import utils.PropertiesFileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static client.context.CApplicationContext.*;
import static utils.Constants.CLIENT_PROPERTIES_FILE;
import static utils.Constants.SERVER_DEFAULT;
import static utils.FileHelper.readObjectToFile;
import static utils.FileHelper.writeObjectToFileAsync;

/**
 * @author ledun
 */
public class LoginScreen extends AbstractScreen implements ResponseHandler, AbstractScreen.NetworkListener {

    public static final String AUTH_TXT = "AUTH.txt";
    public static final String USER = "USER";
    public static final String REMEMBER_ME = "REMEMBER_ME";

    private GroupLayout layout;

    public LoginScreen() throws HeadlessException {
    }

    @Override
    public void onCreateView() {
        initComponents();
        ImageIcon icon = new ImageIcon("loading.gif");
        this.loadingLbl.setIcon(icon);

//        this.loadingLbl.setText("loading ....");
        this.loadingLbl.setVisible(false);
        this.displayNameLbl.setVisible(false);
        this.displayNameTextField.setVisible(false);
        if (data.containsKey("USERNAME")) {
            this.userNameTextField.setText("te");
            this.passwordTextField.setText("te");
        } else {
            this.userNameTextField.setText("dun");
            this.passwordTextField.setText("dun");
        }


        connectToDefaultServer(CLIENT_PROPERTIES_FILE);

        readObjectToFile(AUTH_TXT).thenAccept((r) -> {
            if (r instanceof UserDto) {
                Map<String, Object> data = new HashMap<>();
                data.put(USER, r);
                new Navigator<ChatScreen>().navigate(data);
            }
        });

    }

    private void connectToDefaultServer(String fileName) {
        networkThreadService.submit(() -> {
            Properties properties = PropertiesFileUtils.readPropertiesFile(fileName);
            String[] tokens = properties.getProperty(SERVER_DEFAULT).split(":");

            String host = tokens[0];
            int port = Integer.parseInt(tokens[1]);
            init(host, port);
            tcpClient.connectAsync().thenApply(s -> {
                System.out.println("Connected");
                tcpClient.listeningOnEventAsync();
                return s;
            }).handle((s, e) -> {
                e.printStackTrace();
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "An error occur"));
                return s;
            });
        });
    }


    @Override
    public void addEventListener() {

        this.loginBtn.addActionListener(this::loginBtnActionPerformed);
        this.registerBtn.addActionListener(this::registerActionPerformed);
        this.isRegisterCheck.addActionListener(e -> {
            if (this.isRegisterCheck.isSelected()) {
                this.displayNameLbl.setVisible(true);
                this.displayNameTextField.setVisible(true);
            } else {
                this.displayNameLbl.setVisible(false);
                this.displayNameTextField.setVisible(false);
            }
        });
        configServer.addActionListener((e) -> {
            Map<String, Object> data = new HashMap<>();
            data.put(LoginScreen.class.getSimpleName(), this);
            new Navigator<ConfigServerScreen>().navigate(data);
        });

    }

    public void onConfigDone() {
        connectToDefaultServer(CLIENT_PROPERTIES_FILE);
    }

    private void registerActionPerformed(ActionEvent actionEvent) {
        registerAsync();
    }

    private void loginBtnActionPerformed(ActionEvent e) {
        loginAsync();
    }

    public void registerAsync() {

        UserAuthDto user = new UserAuthDto();
        user.setUserName(userNameTextField.getText());
        user.setPassword(new String(passwordTextField.getPassword()));
        user.setDisplayName(displayNameTextField.getText());

        if (user.getUserName() == null || user.getUserName().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be blank");
            return;
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be blank");
            return;
        }

        if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Display name cannot be blank");
            return;
        }


        uiThreadService.submit(() -> {

            try {
                if (!tcpClient.stillAlive()) {
                    tcpClient.reconnect();
                }
                try {
                    tcpClient.sendRequestAsync(new CommandObject(Command.C2S_REGISTER, user));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        JOptionPane.showMessageDialog(LoginScreen.this, e.getMessage());
                    });
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

    public void loginAsync() {

        UserAuthDto user = new UserAuthDto();

        user.setUserName(userNameTextField.getText());
        user.setPassword(new String(passwordTextField.getPassword()));

        if (user.getUserName() == null || user.getUserName().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be blank");
            return;
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be blank");
            return;
        }

        uiThreadService.submit(() -> {
            try {
                if (!tcpClient.stillAlive()) {
                    tcpClient.reconnect();
                }
                var success = tcpClient.sendRequest(new CommandObject(Command.C2S_LOGIN, user));
                if (!success) {
                    tcpClient.reconnect();
                    success = tcpClient.sendRequest(new CommandObject(Command.C2S_LOGIN, user));
                    if (!success) {
                        throw new Exception("Server is offline");
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(LoginScreen.this, e.getMessage());
                });
                e.printStackTrace();
            }
        });

    }


    @Override
    public void listenOnNetworkEvent(CommandObject commandObject) {
        System.out.println("------------------------------------------");
        switch (commandObject.getCommand()) {
            case S2C_LOGIN_NACK:
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Login fail"));
                break;
            case S2C_LOGIN_ACK:
                this.data = new HashMap<>();

                if (rememberMeCheck.isSelected()) {
                    try {
                        writeObjectToFileAsync(AUTH_TXT, commandObject.getPayload());
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            JOptionPane.showMessageDialog(this, e.getMessage());
                        });
                        e.printStackTrace();
                    }
                    data.put(REMEMBER_ME, true);
                }

                data.put(USER, commandObject.getPayload());
                new Navigator<ChatScreen>().navigate(data, true);

                break;
            case S2C_REGISTER_ACK:
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Register success"));
                break;
            case S2C_REGISTER_NACK:
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Register fail" + commandObject.getPayload()));
                break;
        }
    }


    @Override
    public void registerNetworkListener() {
        tcpClient.registerListener(this);
    }

    @Override
    public void closeHandler() {
        tcpClient.closeHandler(this);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        userNameTextField = new javax.swing.JTextField();
        passwordTextField = new javax.swing.JPasswordField();
        loginBtn = new javax.swing.JButton();
        rememberMeCheck = new javax.swing.JCheckBox();
        registerBtn = new javax.swing.JButton();
        loadingLbl = new javax.swing.JLabel();
        displayNameTextField = new javax.swing.JTextField();
        displayNameLbl = new javax.swing.JLabel();
        isRegisterCheck = new javax.swing.JCheckBox();
        configServer = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Times New Roman", 3, 24)); // NOI18N
        jLabel1.setText("Quick Chat");

        jLabel2.setText("Username");

        jLabel3.setText("Password");

        loginBtn.setText("Login");

        rememberMeCheck.setText("Remember me");

        registerBtn.setText("Register");

        displayNameLbl.setText("Display name");

        isRegisterCheck.setText("Register form");

        configServer.setText("Config server");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2)
                                        .addComponent(jLabel3)
                                        .addComponent(displayNameLbl))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(isRegisterCheck)
                                                        .addComponent(configServer))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 389, Short.MAX_VALUE)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(loginBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(registerBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(rememberMeCheck, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(loadingLbl)
                                                .addGap(198, 198, 198))
                                        .addComponent(passwordTextField)
                                        .addComponent(userNameTextField)
                                        .addComponent(displayNameTextField)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(rememberMeCheck)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(registerBtn))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel2)
                                                        .addComponent(userNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(passwordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(9, 9, 9)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(displayNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(displayNameLbl))
                                                .addGap(45, 45, 45)
                                                .addComponent(isRegisterCheck)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(loginBtn)
                                                .addComponent(configServer))
                                        .addComponent(loadingLbl, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addContainerGap(19, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton configServer;
    private javax.swing.JLabel displayNameLbl;
    private javax.swing.JTextField displayNameTextField;
    private javax.swing.JCheckBox isRegisterCheck;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel loadingLbl;
    private javax.swing.JButton loginBtn;
    private javax.swing.JPasswordField passwordTextField;
    private javax.swing.JButton registerBtn;
    private javax.swing.JCheckBox rememberMeCheck;
    private javax.swing.JTextField userNameTextField;
    // End of variables declaration//GEN-END:variables
}

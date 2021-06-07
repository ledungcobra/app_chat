/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.context.CApplicationContext;
import client.core.ResponseHandler;
import common.dto.Command;
import common.dto.CommandObject;
import common.dto.UserDto;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.val;
import server.entities.User;
import utils.Navigator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static client.context.CApplicationContext.tcpClient;
import static utils.FileHelper.readObjectToFile;
import static utils.FileHelper.writeObjectToFileAsync;

/**
 * @author ledun
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class LoginScreen extends AbstractScreen implements ResponseHandler
{

    public static final String AUTH_TXT = "AUTH.txt";
    public static final String USER = "USER";
    @EqualsAndHashCode.Include
    private int id = 0;


    public LoginScreen() throws HeadlessException
    {
    }

    @Override
    public void onCreateView()
    {
        initComponents();
        ImageIcon icon = new ImageIcon("loading.gif");
        this.loadingLbl.setIcon(icon);
//        this.loadingLbl.setText("loading ....");
        this.loadingLbl.setVisible(false);
        this.displayNameLbl.setVisible(false);
        this.displayNameTextField.setVisible(false);

        readObjectToFile(AUTH_TXT).thenAccept((r) -> {
            if (r instanceof UserDto)
            {
                Map<String, Object> data = new HashMap<>();
                data.put(USER, r);
                new Navigator<ChatScreen>().navigate(data);
            }
        });

    }

    @Override
    public void addEventListener()
    {

        tcpClient.addListener(this);

        this.loginBtn.addActionListener(this::loginBtnActionPerformed);
        this.registerBtn.addActionListener(this::registerActionPerformed);
        this.isRegisterCheck.addActionListener(e -> {
            if (this.isRegisterCheck.isSelected())
            {
                this.displayNameLbl.setVisible(true);
                this.displayNameTextField.setVisible(true);
            } else
            {
                this.displayNameLbl.setVisible(false);
                this.displayNameTextField.setVisible(false);
            }
        });
    }

    private void registerActionPerformed(ActionEvent actionEvent)
    {
        registerAsync();
    }

    @SneakyThrows
    private void loginBtnActionPerformed(ActionEvent e)
    {
        loginAsync();
    }

    public void registerAsync()
    {

        User user = new User();
        user.setUserName(userNameTextField.getText());
        user.setPassword(new String(passwordTextField.getPassword()));
        user.setDisplayName(displayNameTextField.getText());

        if (user.getUserName() == null || user.getUserName().isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Username cannot be blank");
            return;
        }

        if (user.getPassword() == null || user.getPassword().isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Password cannot be blank");
            return;
        }

        if (user.getDisplayName() == null || user.getDisplayName().isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Display name cannot be blank");
            return;
        }


        CApplicationContext.service.submit(() -> {

            try
            {
                if (!tcpClient.stillAlive())
                {
                    System.out.println("Reconnect sync");
                    tcpClient.connect();
                    System.out.println("Reconnect success");
                }

                try
                {
                    tcpClient.sendRequestAsync(new CommandObject(Command.C2S_REGISTER, user));
                } catch (Exception e)
                {
                    runOnUiThread(() -> {
                        JOptionPane.showMessageDialog(LoginScreen.this, e.getMessage());
                    });
                    e.printStackTrace();
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        });

    }

    public void loginAsync()
    {

        User user = new User();
        user.setUserName(userNameTextField.getText());
        user.setPassword(new String(passwordTextField.getPassword()));

        if (user.getUserName() == null || user.getUserName().isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Username cannot be blank");
            return;
        }

        if (user.getPassword() == null || user.getPassword().isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Password cannot be blank");
            return;
        }

        CApplicationContext.service.submit(() -> {
            try
            {

                if (!tcpClient.stillAlive())
                {
                    System.out.println("Reconnect sync");
                    tcpClient.reconnect();
                    System.out.println("Reconnect success");
                }


                val success = tcpClient.sendRequest(new CommandObject(Command.C2S_LOGIN, user));
                if (!success)
                {
                    throw new Exception("Login fail because the server is offline");
                }
            } catch (Exception e)
            {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(LoginScreen.this, e.getMessage());
                });
                e.printStackTrace();
            }
        });

    }


    @Override
    public void listen(CommandObject commandObject)
    {
        if (commandObject.getCommand().equals(Command.S2C_EXIT))
        {
            runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Receive exit signal"));
            closeHandler();
        } else if (commandObject.getCommand().equals(Command.S2C_LOGIN_NACK))
        {
            runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Login fail"));
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_EXIT));

        } else if (commandObject.getCommand().equals(Command.S2C_LOGIN_ACK))
        {
            if (rememberMeCheck.isSelected())
            {
                try
                {
                    writeObjectToFileAsync(AUTH_TXT, commandObject.getPayload());
                } catch (IOException e)
                {
                    runOnUiThread(() -> {
                        JOptionPane.showMessageDialog(this, e.getMessage());
                    });
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Login success"));
            Map<String, Object> data = new HashMap<>();
            data.put(USER, commandObject.getPayload());

            new Navigator<ChatScreen>().navigate(data);
            closeHandler();

        } else if (commandObject.getCommand().equals(Command.S2C_REGISTER_ACK))
        {
            runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Register success"));
            System.out.println(commandObject.getPayload());
        } else if (commandObject.getCommand().equals(Command.S2C_REGISTER_NACK))
        {
            runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Register success fail" + commandObject.getPayload()));
        }
    }


    @Override
    public void closeHandler()
    {
        tcpClient.closeHandler(this);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

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
                                .addGap(27, 27, 27)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(isRegisterCheck)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(loginBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(registerBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(rememberMeCheck, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 485, Short.MAX_VALUE)
                                                .addComponent(loadingLbl)
                                                .addGap(198, 198, 198))
                                        .addComponent(passwordTextField)
                                        .addComponent(userNameTextField)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(displayNameTextField))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(103, 103, 103)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(rememberMeCheck)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(registerBtn)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(loginBtn))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel2)
                                                        .addComponent(userNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(passwordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(9, 9, 9)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(displayNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(displayNameLbl))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(isRegisterCheck)
                                                .addGap(64, 64, 64)
                                                .addComponent(loadingLbl)))
                                .addContainerGap(19, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
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

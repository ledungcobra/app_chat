/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.view;

import client.context.CApplicationContext;
import client.view.AbstractScreen;
import common.dto.UserDto;
import server.context.SApplicationContext;
import server.entities.User;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.swing.JFileChooser.*;
import static utils.Constants.*;
import static utils.UIUtils.checkRequired;

/**
 * @author ledun
 */
public class ServerConfigScreen extends AbstractScreen {


    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_URL = "server.url";
    public static final String SERVER_THREADS = "server.threads";
    public static final String SERVER_DATABASE_URL = "server.database_url";
    public static final String SERVER_DATABASE_USERNAME = "server.database_username";
    public static final String SERVER_DATABASE_PASSWORD = "server.database_password";
    public static final String SERVER_CONFIG_FILE = "./config.properties";
    public static ExecutorService executorService;

    @Override
    public void onCreateView() {
        initComponents();

        Properties properties = loadConfigFromFile();
        fallbackIfLoadFailed(properties);
        updateUIWithProperties(properties);

        executorService = Executors.newFixedThreadPool(2);
        SApplicationContext.configScreen = this;

        onStartBtnActionPerformed(null);

    }

    private void updateUIWithProperties(Properties properties) {
        serverPortTextField.setText(properties.getProperty(SERVER_PORT));
        noThreadsTextField.setText(properties.getProperty(SERVER_THREADS));
        serverUrlTextField.setText(properties.getProperty(SERVER_URL));

        connectionStringTextField.setText(properties.getProperty(SERVER_DATABASE_URL));
        usernameTextField.setText(properties.getProperty(SERVER_DATABASE_USERNAME));
        databasePasswordTextField.setText(properties.getProperty(SERVER_DATABASE_PASSWORD));
    }

    private Properties getConfigFromUI() {
        Properties properties = new Properties();
        properties.setProperty(SERVER_URL, serverUrlTextField.getText());
        properties.setProperty(SERVER_THREADS, noThreadsTextField.getText());
        properties.setProperty(SERVER_PORT, serverPortTextField.getText());

        properties.setProperty(SERVER_DATABASE_URL, connectionStringTextField.getText());
        properties.setProperty(SERVER_DATABASE_PASSWORD, String.valueOf(databasePasswordTextField.getPassword()));
        return properties;
    }

    private Properties loadConfigFromFile() {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(SERVER_CONFIG_FILE);) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private void fallbackIfLoadFailed(Properties properties) {
        if (properties.getProperty(SERVER_PORT) == null) {
            properties.setProperty(SERVER_PORT, String.valueOf(PORT));
        }

        if (properties.getProperty(SERVER_URL) == null) {
            properties.setProperty(SERVER_URL, HOST);
        }

        if (properties.getProperty(SERVER_THREADS) == null) {
            properties.setProperty(SERVER_THREADS, String.valueOf(NO_THREADS));
        }

        if (properties.getProperty(SERVER_DATABASE_URL) == null) {
            properties.setProperty(SERVER_DATABASE_URL, DATABASE_URL);
        }

        if (properties.getProperty(SERVER_DATABASE_USERNAME) == null) {
            properties.setProperty(SERVER_DATABASE_USERNAME, DATABASE_USERNAME);
        }

        if (properties.getProperty(SERVER_DATABASE_PASSWORD) == null) {
            properties.setProperty(SERVER_DATABASE_PASSWORD, DATABASE_PASSWORD);
        }
    }

    @Override
    public void addEventListener() {
        startBtn.addActionListener(this::onStartBtnActionPerformed);
        loadFromFileBtn.addActionListener(this::onLoadFromFileBtnActionPerformed);
        connectDatabaseBtn.addActionListener(this::onConnectBtnActionPerformed);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {
                SApplicationContext.stopServer();
                executorService.shutdown();
            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
    }

    private void onConnectBtnActionPerformed(ActionEvent actionEvent) {
        executorService.submit(() -> {
            try {
                SApplicationContext.connectDb(this.connectionStringTextField.getText(), this.usernameTextField.getText(), String.valueOf(this.databasePasswordTextField.getPassword()));
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(ServerConfigScreen.this, "Connect success");
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(ServerConfigScreen.this, "Cannot connect to the database");
                });
            }

        });
    }

    private void onLoadFromFileBtnActionPerformed(ActionEvent actionEvent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);

        fileChooser.setFileFilter(new FileNameExtensionFilter("properties", "properties"));
        int result = fileChooser.showOpenDialog(this);
        if (result == APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            Properties properties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(selectedFile)) {
                properties.load(fileInputStream);
                updateUIWithProperties(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (result == ERROR_OPTION) {
            JOptionPane.showMessageDialog(this, "An error occur when open file");
        }


    }

    private void onStartBtnActionPerformed(ActionEvent actionEvent) {
        if (startBtn.getText().equals("Start")) {
            String port = serverPortTextField.getText();
            String url = serverUrlTextField.getText();
            String threads = noThreadsTextField.getText();

            String dbUrl = connectionStringTextField.getText();
            String username = usernameTextField.getText();
            String password = String.valueOf(databasePasswordTextField.getPassword());

            if (checkRequired(port, "Please enter port to perform this action")) return;
            if (checkRequired(url, "Please enter url to perform this action")) return;
            if (checkRequired(threads, "Please enter number of threads to continues")) return;
            if (checkRequired(dbUrl, "Please enter connection string to perform this action")) return;
            if (checkRequired(username, "Please enter username to perform this action")) return;
            if (checkRequired(password, "Please enter password to perform this action")) return;

            if (!port.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Port must be a number");
                return;
            }

            if (!threads.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Number of threads must be a number");
                return;
            }

            int portInt = Integer.parseInt(port);
            int threadsInt = Integer.parseInt(threads);

            Properties properties = getConfigFromUI();
            writeConfigToFileAsync(properties);
            runServerAsync(portInt, url, threadsInt, dbUrl, username, password);
            this.startBtn.setText("Stop");

        } else {
            try {
                SApplicationContext.stopServer();
                this.startBtn.setText("Start");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
                e.printStackTrace();
            }

        }

    }

    private void writeConfigToFileAsync(Properties properties) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(SERVER_CONFIG_FILE)) {
            properties.store(fileOutputStream, "Information to start server");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            e.printStackTrace();
        }
    }

    private void runServerAsync(int portInt, String url, int threadsInt, String dbUrl, String username, String password) {
        executorService.submit(() -> {
            try {
                SApplicationContext.init(portInt, url, threadsInt, dbUrl, username, password);
                SApplicationContext.startServer();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, e.getMessage());
                });
            }
        });
    }

    public void updateOnlineList(Map<Socket, User> clients) {
        runOnUiThread(() -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            clients.forEach((s, u) -> {
                if (u == null) {
                    u = new User();
                    u.setDisplayName("Anonymous");
                }
                model.addElement(u.getDisplayName() + " Port: " + s.getPort());
            });
            connectedClientJList.setModel(model);
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        serverPortTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        startBtn = new javax.swing.JButton();
        noThreadsTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        serverUrlTextField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        usernameTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        connectionStringTextField = new javax.swing.JTextField();
        connectDatabaseBtn = new javax.swing.JButton();
        databasePasswordTextField = new javax.swing.JPasswordField();
        jPanel3 = new javax.swing.JPanel();
        loadFromFileBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        connectedClientJList = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Config"));

        jLabel3.setText("No Threads");

        jLabel2.setText("Port");

        startBtn.setBackground(new java.awt.Color(0, 204, 204));
        startBtn.setText("Start");

        jLabel1.setText("Url");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                .addComponent(startBtn))
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel2)
                                                                        .addComponent(jLabel3))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(serverPortTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                                                                        .addComponent(noThreadsTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                                                                        .addComponent(serverUrlTextField))))
                                                .addGap(21, 21, 21))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel2)
                                                        .addComponent(serverPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(noThreadsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel1))
                                        .addComponent(serverUrlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                                .addComponent(startBtn)
                                .addGap(22, 22, 22))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Database config"));

        jLabel6.setText("Password");

        jLabel5.setText("Username");

        jLabel4.setText("Connection String");

        connectDatabaseBtn.setText("Connect");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(connectDatabaseBtn))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel5)
                                                        .addComponent(jLabel6))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(usernameTextField, javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(connectionStringTextField, javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(databasePasswordTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
                                        .addComponent(connectionStringTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5)
                                        .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(databasePasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(connectDatabaseBtn))
                                        .addComponent(jLabel6))
                                .addGap(8, 8, 8))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Load config"));

        loadFromFileBtn.setText("Load from file");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(loadFromFileBtn)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(loadFromFileBtn)
                                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Connected Clients"));

        jScrollPane1.setViewportView(connectedClientJList);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(13, 13, 13)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(62, 62, 62)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(21, 21, 21))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerConfigScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerConfigScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerConfigScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerConfigScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ServerConfigScreen().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton connectDatabaseBtn;
    private javax.swing.JList<String> connectedClientJList;
    private javax.swing.JTextField connectionStringTextField;
    private javax.swing.JPasswordField databasePasswordTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton loadFromFileBtn;
    private javax.swing.JTextField noThreadsTextField;
    private javax.swing.JTextField serverPortTextField;
    private javax.swing.JTextField serverUrlTextField;
    private javax.swing.JButton startBtn;
    private javax.swing.JTextField usernameTextField;
    // End of variables declaration//GEN-END:variables
}

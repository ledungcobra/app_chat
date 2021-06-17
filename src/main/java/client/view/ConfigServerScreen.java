/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.context.CApplicationContext;
import utils.PropertiesFileUtils;
import utils.ScreenStackManager;
import utils.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static utils.Constants.*;
import static utils.PropertiesFileUtils.*;

/**
 * @author ledun
 */
public class ConfigServerScreen extends AbstractScreen {


    private List<String> servers = new ArrayList<>();
    private DefaultComboBoxModel<String> model;
    private String defaultServer;
    private Integer currentIndex;

    @Override
    public void onCreateView() {
        initComponents();
        model = new DefaultComboBoxModel<>();
        this.serverList.setModel(model);

        CApplicationContext.networkThreadService.submit(() -> {
            Properties properties = readPropertiesFile(CLIENT_PROPERTIES_FILE);
            if (properties != null) {
                String[] serversString = properties.getProperty(SERVERS).split(",");

                defaultServer = properties.getProperty(SERVER_DEFAULT);
                servers.addAll(Arrays.asList(serversString));
                runOnUiThread(this::updateListServer);
            } else {
            }
        });

    }

    // Call once
    private void updateListServer() {
        if (this.servers != null) {
            this.servers.forEach(this.model::addElement);
        }
    }

    private void updateListServer(String newServer) {
        this.servers.add(newServer);
        this.model.addElement(newServer);
    }

    private void updateListServer(String newServer, Integer index) {
        this.servers.set(index, newServer);
        this.model.insertElementAt(newServer, index);
        this.model.removeElementAt(index + 1);
    }

    @Override
    public void addEventListener() {

        this.editBtn.addActionListener(this::editActionPerformed);
        this.deleteBtn.addActionListener(this::deleteActionPerformed);
        this.insertBtn.addActionListener(this::insertActionPerformed);
        this.setDefaultBtn.addActionListener(this::setDefaultBtnActionPerformed);
        this.backBtn.addActionListener(this::onBack);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {
                ((LoginScreen) getData().get(LoginScreen.class.getSimpleName())).onConfigDone();
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

    private void onBack(ActionEvent actionEvent) {
        ((LoginScreen) getData().get(LoginScreen.class.getSimpleName())).onConfigDone();
        ScreenStackManager.getInstance().popTopScreen();
    }

    private void setDefaultBtnActionPerformed(ActionEvent actionEvent) {
        this.defaultServer = (String) this.serverList.getSelectedItem();
        syncChangesToFileAsync();
    }

    private void insertActionPerformed(ActionEvent actionEvent) {
        String url = this.urlTextField.getText();
        String port = this.portTextField.getText();
        if (UIUtils.checkRequired(url, "You must enter url to perform this action")) return;
        if (UIUtils.checkRequired(port, "You must enter port to perform this action")) return;
        if (!port.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Port must be a numeric");
            return;
        }

        String newServer = url + ":" + port;

        if (insertBtn.getText().equals("Insert")) {
            if (servers.contains(newServer)) {
                JOptionPane.showMessageDialog(this, "Server must be unique");

            } else {
                updateListServer(newServer);
                syncChangesToFileAsync();
            }
        } else {
            updateListServer(newServer, currentIndex);
            syncChangesToFileAsync();

        }

        currentIndex = null;
        insertBtn.setText("Insert");

    }


    private void syncChangesToFileAsync() {
        if (defaultServer == null) {
            if (servers.size() > 0) {
                defaultServer = serverList.getItemAt(0);
            }
        }

        CApplicationContext.networkThreadService.submit(() -> {
            Properties properties = new Properties();
            properties.setProperty(SERVERS, String.join(",", servers));
            if (servers.size() > 0) {
                properties.setProperty(SERVER_DEFAULT, defaultServer);
            }
            PropertiesFileUtils.writePropertiesFile(CLIENT_PROPERTIES_FILE, properties);
        });
    }

    private void deleteActionPerformed(ActionEvent actionEvent) {
        int selectedServerIndex = this.serverList.getSelectedIndex();
        if (selectedServerIndex == -1) {
            JOptionPane.showMessageDialog(this, "You have to selected at least one server to delete");
            return;
        }
        servers.remove(selectedServerIndex);
        this.model.removeElementAt(selectedServerIndex);
        syncChangesToFileAsync();
    }

    private void editActionPerformed(ActionEvent actionEvent) {
        int selectedServerIndex = this.serverList.getSelectedIndex();
        if (selectedServerIndex == -1) {
            JOptionPane.showMessageDialog(this, "You have to selected at least one server to delete");
            return;
        }
        currentIndex = selectedServerIndex;
        insertBtn.setText("Update");
        String updateItem = this.model.getElementAt(selectedServerIndex);
        String[] tokens = updateItem.split(":");

        urlTextField.setText(tokens[0]);
        portTextField.setText(tokens[1]);

    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        configPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        urlTextField = new javax.swing.JTextField();
        insertBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        portTextField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        serverList = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        editBtn = new javax.swing.JButton();
        deleteBtn = new javax.swing.JButton();
        setDefaultBtn = new javax.swing.JButton();
        backBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        configPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Config"));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Add server"));

        insertBtn.setText("Insert");

        jLabel6.setText("Port");

        jLabel5.setText("Server url");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(urlTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)
                                        .addComponent(portTextField))
                                .addGap(18, 18, 18)
                                .addComponent(insertBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5)
                                        .addComponent(urlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(insertBtn))
                                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("List server"));

        jLabel4.setText("Server");

        editBtn.setText("Edit");

        deleteBtn.setText("Delete");

        setDefaultBtn.setText("Set Default");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4)
                                .addGap(34, 34, 34)
                                .addComponent(serverList, javax.swing.GroupLayout.PREFERRED_SIZE, 474, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(31, 31, 31)
                                .addComponent(deleteBtn)
                                .addGap(18, 18, 18)
                                .addComponent(editBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(setDefaultBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(25, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(serverList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4)
                                        .addComponent(editBtn)
                                        .addComponent(deleteBtn)
                                        .addComponent(setDefaultBtn))
                                .addContainerGap())
        );

        javax.swing.GroupLayout configPanelLayout = new javax.swing.GroupLayout(configPanel);
        configPanel.setLayout(configPanelLayout);
        configPanelLayout.setHorizontalGroup(
                configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(configPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        configPanelLayout.setVerticalGroup(
                configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(configPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        backBtn.setText("Back");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(configPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(backBtn)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(backBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(configPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backBtn;
    private javax.swing.JPanel configPanel;
    private javax.swing.JButton deleteBtn;
    private javax.swing.JButton editBtn;
    private javax.swing.JButton insertBtn;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField portTextField;
    private javax.swing.JComboBox<String> serverList;
    private javax.swing.JButton setDefaultBtn;
    private javax.swing.JTextField urlTextField;
    // End of variables declaration//GEN-END:variables
}

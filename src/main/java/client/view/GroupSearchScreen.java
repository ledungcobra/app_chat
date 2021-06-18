/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.CommandObject;
import common.dto.GroupDto;
import lombok.EqualsAndHashCode;
import lombok.val;
import server.entities.Group;
import utils.ScreenStackManager;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static client.context.CApplicationContext.tcpClient;
import static client.view.ChatScreen.*;
import static common.dto.Command.*;

/**
 * @author ledun
 */

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupSearchScreen extends AbstractScreen implements ResponseHandler, NetworkListener {


    @EqualsAndHashCode.Include
    public String screenName = getClass().getSimpleName();

    private Set<GroupDto> selectedGroups = new HashSet<>();
    private List<GroupDto> loadedGroupDtos = new ArrayList<>();

    @Override
    public void onCreateView() {
        initComponents();
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    public void addEventListener() {
        this.joinGroupBtn.addActionListener(e -> onJoinGroupActionPerformed());
        Consumer<Set<Object>> onSearchDone = (Consumer<Set<Object>>) getData().get(SEARCH_DONE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {
                onSearchDone.accept(null);
                GroupSearchScreen.this.closeHandler();
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

    private void onJoinGroupActionPerformed() {
        if (loadedGroupDtos == null || loadedGroupDtos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no group");
            return;
        }
        int[] indices = resultList.getSelectedIndices();

        for (int i : indices) {
            selectedGroups.add(loadedGroupDtos.get(i));
        }

        val groupId = selectedGroups
                .stream().map(GroupDto::getId).toArray(Long[]::new);

        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_JOIN_GROUPS, groupId));
    }


    @Override
    public void listenOnNetworkEvent(CommandObject commandObject) {
        System.out.println(getClass().getSimpleName());
        switch (commandObject.getCommand()) {
            case S2C_FIND_GROUP_BY_KEYWORD_ACK: {
                List<GroupDto> groupDtoList = (List<GroupDto>) commandObject.getPayload();
                this.loadedGroupDtos = groupDtoList;
                runOnUiThread(this::updateFriendList);
                break;
            }

            case SERVER_STOP_SIGNAL: {
                runOnUiThread(() -> {
                    ScreenStackManager.getInstance().popTo(LoginScreen.class);
                });
                break;
            }

            case S2C_SEND_JOIN_GROUPS_ACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Send an invitation to group " + commandObject.getPayload() + " success");
                });
                break;
            }

            case S2C_SEND_JOIN_GROUPS_NACK:
            case S2C_FIND_GROUP_BY_KEYWORD_NACK: {
                runOnUiThread(() -> JOptionPane.showMessageDialog(GroupSearchScreen.this, commandObject.getPayload()));
                break;
            }

        }
    }

    private void updateFriendList() {
        DefaultListModel<GroupDto> model = new DefaultListModel<GroupDto>();
        loadedGroupDtos.forEach(model::addElement);
        this.resultList.setModel(model);

    }


    @Override
    public void registerNetworkListener() {
        tcpClient.registerListener(this);
        tcpClient.sendRequestAsync(new CommandObject(C2S_FIND_GROUP_BY_KEYWORD, getData().get(SEARCH_KEYWORD)));
    }

    @Override
    public void closeHandler() {
        tcpClient.closeHandler(this);
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        resultList = new javax.swing.JList<>();
        joinGroupBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);


        jScrollPane1.setViewportView(resultList);

        joinGroupBtn.setBackground(new java.awt.Color(204, 255, 51));
        joinGroupBtn.setText("Join");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(joinGroupBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(32, 32, 32)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 461, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(43, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(joinGroupBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 510, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(40, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton joinGroupBtn;
    private javax.swing.JList<GroupDto> resultList;
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.*;
import lombok.val;

import javax.swing.*;
import javax.swing.event.ListDataListener;

import java.util.List;

import static client.context.CApplicationContext.tcpClient;
import static client.view.LoginScreen.USER;
import static common.dto.Command.*;

/**
 * @author ledun
 */
public class ChatScreen extends AbstractScreen implements ResponseHandler
{

    @Override
    public void onCreateView()
    {
        initComponents();

        UserDto userDto = (UserDto) getData().get(USER);

        this.displayNameLbl.setText(userDto.getDisplayName());

        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_GROUP_LIST, userDto.getId()));

    }

    @Override
    public void addEventListener()
    {
        tcpClient.addListener(this);
    }

    private void updateFriendList(List<FriendDto> friendDtoList)
    {
        val model = new DefaultListModel<FriendDto>();
        friendDtoList.forEach(f -> model.addElement(f));
        this.friendsList.setModel(model);
    }


    private void updateGroupList(List<GroupDto> groupDtos)
    {
        val model = new DefaultListModel<GroupDto>();
        groupDtos.forEach(g -> model.addElement(g));
        this.groupsList.setModel(model);
    }

    @Override
    public void listen(CommandObject commandObject)
    {
        Command command = commandObject.getCommand();
        if (command.equals(S2C_GET_FRIEND_LIST_ACK))
        {
            List<FriendDto> friendDtoList = (List<FriendDto>) commandObject.getPayload();
            runOnUiThread(() -> updateFriendList(friendDtoList));
        } else if (command.equals(S2C_GET_GROUP_LIST_ACK))
        {
            List<GroupDto> groupDtos = (List<GroupDto>) commandObject.getPayload();
            runOnUiThread(() -> updateGroupList(groupDtos));
        } else if (command.equals(S2C_USER_NOT_FOUND))
        {
            runOnUiThread(() -> {
                JOptionPane.showMessageDialog(ChatScreen.this, "Cannot identify your account");
            });
        }
    }

    @Override
    public void closeHandler()
    {
        tcpClient.closeHandler(this);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        searchFriend = new javax.swing.JTextField();
        searchBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        chatTextArea = new javax.swing.JTextArea();
        sendBtn = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        chatInputTextField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        groupsList = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        friendsList = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        unfriendBtn = new javax.swing.JButton();
        removeGroupBtn = new javax.swing.JButton();
        showNotificationBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        notificationTextArea = new javax.swing.JTextArea();
        backBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        displayNameLbl = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setText("Search username");

        searchBtn.setText("Search");

        chatTextArea.setColumns(20);
        chatTextArea.setRows(5);
        jScrollPane1.setViewportView(chatTextArea);

        sendBtn.setText("Send");

        jButton3.setText("File");

        groupsList.setModel(new javax.swing.AbstractListModel<GroupDto>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(groupsList);

        friendsList.setModel(new javax.swing.AbstractListModel<FriendDto>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(friendsList);

        jLabel2.setText("Friend");

        jLabel3.setText("Group");

        unfriendBtn.setBackground(new java.awt.Color(255, 0, 0));
        unfriendBtn.setForeground(new java.awt.Color(255, 0, 51));
        unfriendBtn.setText("Unfriend");

        removeGroupBtn.setBackground(new java.awt.Color(255, 51, 0));
        removeGroupBtn.setForeground(new java.awt.Color(255, 51, 51));
        removeGroupBtn.setText("Remove");

        showNotificationBtn.setText("Show Notification");

        notificationTextArea.setColumns(20);
        notificationTextArea.setRows(5);
        jScrollPane2.setViewportView(notificationTextArea);

        backBtn.setText("Logout");

        jLabel4.setText("Hi");

        displayNameLbl.setText("display name");

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Logout");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Group Chat");
        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 622, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(chatInputTextField)
                        .addGap(18, 18, 18)
                        .addComponent(sendBtn))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(searchFriend, javax.swing.GroupLayout.PREFERRED_SIZE, 494, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton3)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel3)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(removeGroupBtn))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(unfriendBtn))
                                .addComponent(searchBtn, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(showNotificationBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(displayNameLbl)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                                .addComponent(backBtn)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backBtn)
                    .addComponent(jLabel4)
                    .addComponent(displayNameLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(searchFriend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchBtn)
                    .addComponent(showNotificationBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(removeGroupBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(17, 17, 17)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(unfriendBtn))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2)))
                    .addComponent(jScrollPane1))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chatInputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sendBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backBtn;
    private javax.swing.JTextField chatInputTextField;
    private javax.swing.JTextArea chatTextArea;
    private javax.swing.JLabel displayNameLbl;
    private javax.swing.JList<FriendDto> friendsList;
    private javax.swing.JList<GroupDto> groupsList;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea notificationTextArea;
    private javax.swing.JButton removeGroupBtn;
    private javax.swing.JButton searchBtn;
    private javax.swing.JTextField searchFriend;
    private javax.swing.JButton sendBtn;
    private javax.swing.JButton showNotificationBtn;
    private javax.swing.JButton unfriendBtn;
    // End of variables declaration//GEN-END:variables
}

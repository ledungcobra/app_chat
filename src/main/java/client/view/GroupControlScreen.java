/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.*;
import jdk.nashorn.internal.scripts.JO;
import lombok.EqualsAndHashCode;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Objects;

import static client.context.CApplicationContext.tcpClient;

/**
 * @author ledun
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupControlScreen extends AbstractScreen implements AbstractScreen.NetworkListener, ResponseHandler {
    @EqualsAndHashCode.Include
    public String screenName = getClass().getSimpleName();

    private DefaultComboBoxModel<GroupDto> groupComboModel;
    private DefaultListModel<UserDto> memberListModel;
    private DefaultListModel<UserPendingDto> userPendingListModel;
    private DefaultComboBoxModel<FriendDto> friendDtoModel;

    @Override
    public void onCreateView() {
        initComponents();

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    public void addEventListener() {
        createGroupBtn.addActionListener((e) -> onAddNewGroupActionListener());
        this.addUserBtn.addActionListener((e) -> onAddUserActionListener());
        this.removeUserBtn.addActionListener((e) -> onRemoveUserActionListener());
        this.setAdmin.addActionListener(e -> onSetAdminActionListener());
        this.removeAdmin.addActionListener(e -> onRemoveAdminActionListener());
        this.acceptPendingMemberBtn.addActionListener((e) -> onAcceptPendingMemberActionListener());
        this.declinePendingMember.addActionListener(e -> onDeclineMemberActionListener());
        this.searchBtn.addActionListener(e -> onSearchUserActionListener());
        this.groupComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                GroupDto item = (GroupDto) e.getItem();
                tcpClient.sendRequestAsync(new CommandObject(Command.C2S_GET_ALL_MEMBERS_IN_GROUP, item.getId()));
                tcpClient.sendRequestAsync(new CommandObject(Command.C2S_GET_PENDING_USER_GROUP_LIST, item.getId()));
            }
        });

        this.backBtn.addActionListener((e) -> {
            finish();
        });
    }

    private void onSearchUserActionListener() {
        String keyword = searchTextField.getText();
        if (keyword == null || keyword.isEmpty()) {
            return;
        }
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_FIND_FRIEND_BY_KEYWORD, keyword));


    }

    private void onDeclineMemberActionListener() {
        if (userPendingListModel == null) {
            JOptionPane.showMessageDialog(this, "You have to select a group to continue");
            return;
        }
        if (userPendingList.getSelectedIndex() == -1) {
            return;
        }
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_DECLINE_USER, userPendingList.getSelectedValue().getId()));

    }

    private void onAcceptPendingMemberActionListener() {
        if (userPendingListModel == null) {
            JOptionPane.showMessageDialog(this, "You have to select a group to continue");
            return;
        }
        if (userPendingList.getSelectedIndex() == -1) {
            return;
        }
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_ACCEPT_A_MEMBER_TO_GROUP, userPendingList.getSelectedValue().getId()));

    }

    private void onRemoveAdminActionListener() {
        if (groupComboBox.getSelectedIndex() == -1) return;
        if (memberList.getSelectedIndex() == -1) return;

        Long userId = memberList.getSelectedValue().getId();
        Long groupID = ((GroupDto) Objects.requireNonNull(groupComboBox.getSelectedItem(), "groupComboBox ")).getId();
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_REMOVE_ADMIN, new Long[]{
                userId, groupID
        }));
    }

    private void onSetAdminActionListener() {
        if (groupComboBox.getSelectedIndex() == -1) return;
        if (memberList.getSelectedIndex() == -1) return;

        Long userId = memberList.getSelectedValue().getId();
        Long groupID = ((GroupDto) Objects.requireNonNull(groupComboBox.getSelectedItem(), "groupComboBox ")).getId();
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_ADD_ADMIN, new Long[]{
                userId, groupID
        }));
    }

    private void onRemoveUserActionListener() {
        if (groupComboBox.getSelectedIndex() == -1) return;
        if (memberList.getSelectedIndex() == -1) return;

        Long userId = memberList.getSelectedValue().getId();
        Long groupID = ((GroupDto) Objects.requireNonNull(groupComboBox.getSelectedItem(), "groupComboBox ")).getId();
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_REMOVE_MEMBER, new Long[]{
                userId, groupID
        }));
    }

    private void onAddUserActionListener() {
        FriendDto selectedItem = (FriendDto) resultFriendCombo.getSelectedItem();
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "You must choose a user to continue");
            return;
        }

        if (groupComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "You must select a group to continue");
            return;
        }

        Long userID = selectedItem.getId();
        Long groupId = ((GroupDto) groupComboBox.getSelectedItem()).getId();

        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_ADD_MEMBER_TO_GROUP, new Long[]{userID, groupId}));

    }

    private void onAddNewGroupActionListener() {

        String groupName = JOptionPane.showInputDialog("Enter group you want to add");
        if (groupName == null && groupName.isEmpty()) return;
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_CREATE_NEW_GROUP, new GroupDto(groupName)));

    }


    @Override
    @SuppressWarnings({"All"})
    public void listenOnNetworkEvent(CommandObject commandObject) {

        switch (commandObject.getCommand()) {
            case S2C_FIND_FRIEND_BY_KEYWORD_ACK: {
                List<FriendDto> friendDtoList = (List<FriendDto>) commandObject.getPayload();
                runOnUiThread(() -> updateFriendCombo(friendDtoList));
                break;
            }


            case S2C_GET_GROUP_LIST_ACK: {
                List<GroupDto> groupDtoList = (List<GroupDto>) commandObject.getPayload();
                runOnUiThread(() -> updateGroupDtoComboBox(groupDtoList));
                break;
            }

            case S2C_GET_ALL_MEMBERS_IN_GROUP_ACK: {
                List<UserDto> members = (List<UserDto>) commandObject.getPayload();
                runOnUiThread(() -> updateMemberGroup(members));
                break;
            }

            case S2C_GET_PENDING_USER_GROUP_LIST_ACK: {
                List<UserPendingDto> userPendingDtos = (List<UserPendingDto>) commandObject.getPayload();
                runOnUiThread(() -> {
                    updateUserPendingList(userPendingDtos);
                });
                break;
            }

            case S2C_ADD_ADMIN_ACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Set admin success");
                });
                break;
            }

            case S2C_REMOVE_MEMBER_ACK: {
                Long userId = (Long) commandObject.getPayload();
                runOnUiThread(() -> removeMemberFromJList(userId));
                break;
            }
            case S2C_REMOVE_ADMIN_ACK: {
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Remove admin success"));
                break;
            }

            case S2C_USER_NOT_FOUND:
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, "Server cannot find this user"));
                break;

            case S2C_CREATE_NEW_GROUP_ACK: {
                GroupDto groupDto = (GroupDto) commandObject.getPayload();
                runOnUiThread(() -> {
                    addNewGroupToList(groupDto);
                    JOptionPane.showMessageDialog(this, "Add new group success");
                });
                break;
            }
            case S2C_ACCEPT_A_MEMBER_TO_GROUP_ACK: {
                UserDto newMember = (UserDto) commandObject.getPayload();
                runOnUiThread(() -> {
                    addNewMemberToGroup(newMember);
                });
                break;
            }
            case S2C_ADD_MEMBER_TO_GROUP_ACK: {
                Long[] ids = (Long[]) commandObject.getPayload();
                Long userId = ids[0];
                Long groupId = ids[1];

                runOnUiThread(() -> {
                    addUserToGroup(userId, groupId);
                    JOptionPane.showMessageDialog(this, "Add new member success");
                });
                break;
            }
            case S2C_DECLINE_USER_ACK: {
                Long pendingId = (Long) commandObject.getPayload();
                runOnUiThread(() -> {
                    removeAnItemPendingList(pendingId);
                });
                break;
            }
            case S2C_ADD_MEMBER_TO_GROUP_NACK:
            case S2C_FIND_FRIEND_BY_KEYWORD_NACK:
            case S2C_DECLINE_USER_NACK:
            case S2C_ACCEPT_A_MEMBER_TO_GROUP_NACK:
            case S2C_CREATE_NEW_GROUP_NACK:
            case S2C_REMOVE_ADMIN_NACK:
            case S2C_REMOVE_MEMBER_NACK:
            case S2C_ADD_ADMIN_NACK:
            case S2C_GET_GROUP_LIST_NACK:
            case S2C_GET_ALL_MEMBERS_IN_GROUP_NACK:
            case S2C_GET_PENDING_USER_GROUP_LIST_NACK: {
                runOnUiThread(() -> JOptionPane.showMessageDialog(this, commandObject.getPayload()));
                break;
            }
        }
    }

    private void addUserToGroup(Long userId, Long groupId) {
        GroupDto groupDto = (GroupDto) groupComboBox.getSelectedItem();
        if (groupDto.getId().equals(groupId)) {
            FriendDto found = null;
            for (int i = 0; i < friendDtoModel.getSize(); i++) {
                if (friendDtoModel.getElementAt(i).getId().equals(userId)) {
                    found = friendDtoModel.getElementAt(i);
                    break;
                }
            }

            UserDto user = Mapper2.map(found, UserDto.class);
            getMemberListModel().addElement(user);
        }

    }

    DefaultListModel<UserDto> getMemberListModel() {
        if (memberListModel == null) {
            memberListModel = new DefaultListModel<>();
            this.memberList.setModel(memberListModel);
        }
        return memberListModel;
    }

    private void updateFriendCombo(List<FriendDto> friendDtoList) {
        friendDtoModel = new DefaultComboBoxModel<FriendDto>();
        friendDtoList.forEach(friendDtoModel::addElement);
        resultFriendCombo.setModel(friendDtoModel);

    }

    private void removeAnItemPendingList(Long pendingId) {
        int found = -1;
        for (int i = 0; i < userPendingListModel.size(); i++) {
            if (pendingId.equals(userPendingListModel.elementAt(i).getId())) {
                found = i;
                break;
            }
        }
        userPendingListModel.remove(found);
    }

    private void addNewMemberToGroup(UserDto newMember) {
        getMemberListModel().addElement(newMember);
    }

    private void addNewGroupToList(GroupDto groupDto) {
        groupComboBox.setSelectedItem(groupDto);
        groupComboModel.addElement(groupDto);
    }

    private void removeMemberFromJList(Long userId) {
        int idx = 0;
        for (int i = 0; i < getMemberListModel().size(); i++) {
            UserDto elementAt = getMemberListModel().getElementAt(i);
            if (elementAt.getId().equals(userId)) {
                idx = i;
                break;
            }
        }

        getMemberListModel().remove(idx);
    }

    private void updateUserPendingList(List<UserPendingDto> userPendingDtos) {
        userPendingListModel = new DefaultListModel<UserPendingDto>();
        userPendingDtos.forEach(userPendingListModel::addElement);
        userPendingList.setModel(userPendingListModel);

    }

    private void updateMemberGroup(List<UserDto> members) {
        memberListModel = new DefaultListModel<>();
        members.forEach(memberListModel::addElement);
        memberList.setModel(memberListModel);
    }

    private void updateGroupDtoComboBox(List<GroupDto> groupDtoList) {
        groupComboModel = new DefaultComboBoxModel<>();
        groupDtoList.forEach(groupComboModel::addElement);
        groupComboBox.setModel(groupComboModel);

        if(groupDtoList.size()>0){
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_GET_PENDING_USER_GROUP_LIST, ((GroupDto)groupComboBox.getSelectedItem()).getId()));
        }
    }

    @Override
    public void closeHandler() {
        tcpClient.closeHandler(this);
    }

    @Override
    public void registerNetworkListener() {
        tcpClient.registerListener(this);
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_GET_GROUP_LIST));
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backBtn = new javax.swing.JButton();
        declinePendingMember = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        userPendingList = new javax.swing.JList<>();
        acceptPendingMemberBtn = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        memberList = new javax.swing.JList<>();
        removeUserBtn = new javax.swing.JButton();
        removeAdmin = new javax.swing.JButton();
        setAdmin = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        groupComboBox = new javax.swing.JComboBox<>();
        createGroupBtn = new javax.swing.JButton();
        addUserBtn = new javax.swing.JButton();
        resultFriendCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        searchTextField = new javax.swing.JTextField();
        searchBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        backBtn.setText("Back");

        declinePendingMember.setText("Decline");

        jScrollPane2.setViewportView(userPendingList);

        acceptPendingMemberBtn.setText("Accept");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Member management"));

        jScrollPane1.setViewportView(memberList);

        removeUserBtn.setText("Remove");

        removeAdmin.setText("Remove admin");

        setAdmin.setText("Set admin");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(removeUserBtn)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(setAdmin)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(removeAdmin))
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 474, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(19, 19, 19))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(removeUserBtn)
                                        .addComponent(setAdmin)
                                        .addComponent(removeAdmin))
                                .addGap(6, 6, 6)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                                .addContainerGap())
        );

        jLabel1.setText("Group");

        createGroupBtn.setText("Create");

        addUserBtn.setText("Add");

        jLabel2.setText("Result");

        jLabel3.setText("Search");

        searchBtn.setText("Search");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(backBtn)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabel1)
                                                                .addGap(50, 50, 50)
                                                                .addComponent(groupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 355, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(createGroupBtn)))
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(jLabel2)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                                        .addComponent(resultFriendCombo, 0, 255, Short.MAX_VALUE)
                                                                                        .addComponent(searchTextField))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(searchBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                        .addComponent(addUserBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                        .addGroup(layout.createSequentialGroup()
                                                                                                .addComponent(acceptPendingMemberBtn)
                                                                                                .addGap(31, 31, 31)
                                                                                                .addComponent(declinePendingMember))
                                                                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 417, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                                .addGap(0, 20, Short.MAX_VALUE))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel3)
                                                                .addGap(0, 0, Short.MAX_VALUE))))))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(backBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(groupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(createGroupBtn)
                                                .addComponent(jLabel3)
                                                .addComponent(searchBtn)
                                                .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(21, 21, 21)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(addUserBtn)
                                                        .addComponent(resultFriendCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel2))
                                                .addGap(72, 72, 72)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(acceptPendingMemberBtn)
                                                        .addComponent(declinePendingMember))
                                                .addGap(18, 18, 18)
                                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(23, 23, 23))
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
            java.util.logging.Logger.getLogger(GroupControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GroupControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GroupControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GroupControlScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GroupControlScreen().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptPendingMemberBtn;
    private javax.swing.JButton addUserBtn;
    private javax.swing.JButton backBtn;
    private javax.swing.JButton createGroupBtn;
    private javax.swing.JButton declinePendingMember;
    private javax.swing.JComboBox<GroupDto> groupComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList<UserDto> memberList;
    private javax.swing.JButton removeAdmin;
    private javax.swing.JButton removeUserBtn;
    private javax.swing.JComboBox<FriendDto> resultFriendCombo;
    private javax.swing.JButton searchBtn;
    private javax.swing.JTextField searchTextField;
    private javax.swing.JButton setAdmin;
    private javax.swing.JList<UserPendingDto> userPendingList;
    // End of variables declaration//GEN-END:variables
}

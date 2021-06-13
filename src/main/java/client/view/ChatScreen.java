/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.*;
import lombok.val;
import utils.Navigator;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static client.context.CApplicationContext.tcpClient;
import static client.view.LoginScreen.*;
import static common.dto.Command.*;
import static utils.FileHelper.deleteFileAsync;

/**
 * @author ledun
 */
public class ChatScreen extends AbstractScreen implements ResponseHandler, NetworkListener
{

    public static final String SEARCH_KEYWORD = "SEARCH_KEYWORD";
    public static final String SEARCH_DONE = "SEARCH_DONE";
    private DefaultListModel<NotificationDto> notifyModel;
    private DefaultListModel<FriendOfferDto> friendOfferListModel;
    private Map<FriendDto, List<PrivateMessageDto>> friendMessageMap;
    private Map<GroupDto, List<GroupMessageDto>> groupMessageDtoMap;

    private DefaultListModel<FriendDto> friendListModel;

    private String message;
    private UserDto userDto;

    @Override
    public void onCreateView()
    {
        initComponents();

        this.userDto = (UserDto) getData().get(USER);
        this.displayNameLbl.setText(userDto.getDisplayName());

        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_GROUP_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_UNSEEN_NOTIFICATIONS, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(Command.C2S_GET_UNSEEN_FRIEND_OFFERS, userDto.getId()));
    }

    @Override
    public void addEventListener()
    {
        searchFriend.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
            }

            @Override
            public void keyPressed(KeyEvent e)
            {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    openSearchResultScreen();

                }
            }
        });

        logoutBtn.addActionListener(e -> onLogoutBtnActionPerformed());
        searchBtn.addActionListener(e -> openSearchResultScreen());

        acceptFriendBtn.addActionListener(e -> onAddFriendBtnActionPerformed());
        ignoreFriendBtn.addActionListener(e -> onIgnoreFriendBtnActionPerformed());

        unfriendBtn.addActionListener(e -> onUnfriendBtnActionPerformed());

    }

    private void onUnfriendBtnActionPerformed()
    {
        int[] friendsIds = friendsList.getSelectedIndices();
        if (friendsIds.length == 0)
        {
            JOptionPane.showMessageDialog(this, "You have to select at least a friend to continue");
            return;
        }

        List<Long> friendIds = Arrays.stream(friendsIds).mapToObj(i -> friendListModel.get(i).getId()).collect(Collectors.toList());
        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_UNFRIEND_REQUEST, friendIds));
    }


    private void onIgnoreFriendBtnActionPerformed()
    {
        int[] indices = friendOfferJList.getSelectedIndices();
        if (indices.length == 0)
        {
            JOptionPane.showMessageDialog(this, "You have to select at least an offer to perform this action");
            return;
        }

        List<FriendOfferDto> friends = Arrays.stream(indices).mapToObj(i -> {
            FriendOfferDto friendOfferDto = friendOfferListModel.get(i);
            friendOfferDto.setAccepted(false);
            return friendOfferDto;
        }).collect(Collectors.toList());

        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_IGNORE_FRIEND_OFFERS, friends));
    }

    private void onAddFriendBtnActionPerformed()
    {
        int[] indices = friendOfferJList.getSelectedIndices();
        if (indices.length == 0)
        {
            JOptionPane.showMessageDialog(this, "You have to select at least an offer to perform this action");
            return;
        }

        List<FriendOfferDto> friends = Arrays.stream(indices).mapToObj(i -> {
            FriendOfferDto friendOfferDto = friendOfferListModel.get(i);
            friendOfferDto.setAccepted(true);
            return friendOfferDto;
        }).collect(Collectors.toList());
        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_ACCEPT_FRIEND_OFFERS, friends));

    }

    private void onLogoutBtnActionPerformed()
    {
        Boolean rememberMe = (Boolean) getData().get(REMEMBER_ME);
        if (rememberMe != null && rememberMe == true)
        {
            deleteFileAsync(AUTH_TXT).thenAcceptAsync(r -> {
                if (r)
                {
                    System.out.println("DELETE FILE SUCCESS");
                }
            });
        }

        finish();
    }

    public void openSearchResultScreen()
    {
        getData().put(SEARCH_KEYWORD, searchFriend.getText());

        Consumer<Set<FriendDto>> onSearchDone = this::onSearchDone;
        getData().put(SEARCH_DONE, onSearchDone);

        new Navigator<FriendSearchScreen>().navigate(getData(), false);
    }

    private void onSearchDone(Set<FriendDto> friendDtoList)
    {
        getData().remove(SEARCH_DONE);
        getData().remove(SEARCH_KEYWORD);
    }

    @Override
    public void listenOnNetworkEvent(CommandObject commandObject)
    {
        Command command = commandObject.getCommand();
        switch (command)
        {
            // DONE
            case S2C_GET_FRIEND_LIST_ACK:
            {
                List<FriendDto> friendDtoList = (List<FriendDto>) commandObject.getPayload();
                runOnUiThread(() -> updateFriendList(friendDtoList));
                break;
            }
            // DONE
            case S2C_GET_GROUP_LIST_ACK:
            {
                List<GroupDto> groupDtos = (List<GroupDto>) commandObject.getPayload();
                runOnUiThread(() -> updateGroupList(groupDtos));
                break;
            }
            // DONE

            case S2C_USER_NOT_FOUND:
            {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(ChatScreen.this, "Cannot identify your account");
                });
                break;
            }
            case S2C_GET_UNSEEN_NOTIFICATION_ACK:
            {
                List<NotificationDto> notificationDtos = (List<NotificationDto>) commandObject.getPayload();
                List<Long> markSeenNotification = notificationDtos.stream()
                        .map(n -> n.getId()).collect(Collectors.toList());

                tcpClient.sendRequestAsync(new CommandObject(C2S_MARK_SEEN_NOTIFICATIONS, markSeenNotification));
                runOnUiThread(() -> updateNotification(notificationDtos));
                break;
            }
            case S2C_NOTIFY_NEW_FRIEND_OFFER:
            {
                FriendOfferDto notificationDto = (FriendOfferDto) commandObject.getPayload();
                runOnUiThread(() -> addNewFriendOfferToNotification(notificationDto));
                break;
            }

            case S2C_GET_UNSEEN_FRIEND_OFFERS_ACK:
            {
                List<FriendOfferDto> friendOfferDtos = (List<FriendOfferDto>) commandObject.getPayload();
                runOnUiThread(() -> updateFriendOfferList(friendOfferDtos));
                break;
            }

            case S2C_SEND_ACCEPT_FRIEND_OFFERS_ACK:
            case S2C_SEND_UNFRIEND_REQUEST_ACK:
            {
                tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
                break;
            }

            case S2C_GET_FRIEND_LIST_NACK:
            case S2C_SEND_IGNORE_FRIEND_OFFERS_NACK:
            case S2C_SEND_UNFRIEND_REQUEST_NACK:
            case S2C_GET_UNSEEN_FRIEND_OFFERS_NACK:
            case S2C_GET_UNSEEN_NOTIFICATION_NACK:
            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_NACK:
            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_FAIL:
                runOnUiThread(() -> JOptionPane.showMessageDialog(ChatScreen.this, commandObject.getPayload()));
                break;
        }
    }

    private void renderGroupMessages(FriendDto friend)
    {
        List<PrivateMessageDto> friendMessages = friendMessageMap.get(friend);
        UserDto user = (UserDto) getData().get(USER);

        if (friendMessages != null)
        {
            StringBuilder builder = new StringBuilder();
            friendMessages.forEach(m -> {

                // Message from you
                if (user.getId().equals(m.getSender().getId()))
                {
                    String message = "You: " + m.getContent();
                    builder.append(message);
                } else
                {
                    builder.append(m);
                }

                builder.append("\n");

            });

            this.chatTextArea.setText(builder.toString());
        } else
        {
            // Fetch message online
            System.out.println("Dont have messages");
        }
    }

    private void renderGroupMessages(GroupDto group)
    {
        List<GroupMessageDto> groupMessages = groupMessageDtoMap.get(group);
        if (groupMessages != null)
        {
            StringBuilder builder = new StringBuilder();
            groupMessages.forEach(m -> {


                builder.append(m.toString());

                builder.append("\n");
            });

            chatTextArea.setText(builder.toString());

        } else
        {

        }
    }


    private void updateNotification(List<NotificationDto> notificationDtos)
    {
        this.notifyModel = new DefaultListModel<NotificationDto>();
        notificationDtos.forEach(notifyModel::addElement);
        this.notificationJList.setModel(notifyModel);
    }

    private void updateFriendOfferList(List<FriendOfferDto> friendOfferDtos)
    {
        if (friendOfferListModel == null)
        {
            this.friendOfferListModel = new DefaultListModel<FriendOfferDto>();
        }
        friendOfferDtos.forEach(this.friendOfferListModel::addElement);
        this.friendOfferJList.setModel(friendOfferListModel);
    }

    private void addNewFriendOfferToNotification(FriendOfferDto friendOfferDto)
    {
        if (friendOfferListModel == null)
        {
            friendOfferListModel = new DefaultListModel<>();
        }
        friendOfferListModel.addElement(friendOfferDto);
    }

    private void addNewFriendIntoFriendList(FriendDto friendDto)
    {
        this.friendListModel.addElement(friendDto);
    }

    private void updateGroupList(List<GroupDto> groupDtos)
    {
        val model = new DefaultListModel<GroupDto>();
        groupDtos.forEach(model::addElement);
        this.groupsList.setModel(model);
    }

    private void updateFriendList(List<FriendDto> friendDtoList)
    {
        this.friendListModel = new DefaultListModel<FriendDto>();
        friendDtoList.forEach(friendListModel::addElement);
        this.friendsList.setModel(friendListModel);
    }


    @Override
    public void registerNetworkListener()
    {
        tcpClient.registerListener(this);
    }

    @Override
    public void closeHandler()
    {
        tcpClient.closeHandler(this);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

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
        logoutBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        displayNameLbl = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        notificationJList = new javax.swing.JList<>();
        jLabel5 = new javax.swing.JLabel();
        markSeenAllNotification = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        friendOfferJList = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        acceptFriendBtn = new javax.swing.JButton();
        ignoreFriendBtn = new javax.swing.JButton();
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


        jScrollPane3.setViewportView(groupsList);

        jScrollPane4.setViewportView(friendsList);

        jLabel2.setText("Friend");

        jLabel3.setText("Group");

        unfriendBtn.setBackground(new java.awt.Color(255, 0, 0));
        unfriendBtn.setForeground(new java.awt.Color(255, 0, 51));
        unfriendBtn.setText("Unfriend");

        removeGroupBtn.setBackground(new java.awt.Color(255, 51, 0));
        removeGroupBtn.setForeground(new java.awt.Color(255, 51, 51));
        removeGroupBtn.setText("Remove");

        showNotificationBtn.setText("Hide Notification");

        logoutBtn.setText("Logout");

        jLabel4.setText("Hi");

        displayNameLbl.setText("display name");

        jScrollPane5.setViewportView(notificationJList);

        jLabel5.setText("Notification");

        markSeenAllNotification.setText("Mark all seen");

        jScrollPane2.setViewportView(friendOfferJList);

        jLabel6.setText("Friend offer");

        acceptFriendBtn.setText("Accept");

        ignoreFriendBtn.setText("Ignore");

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
                                                                .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(searchBtn, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(showNotificationBtn))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                                                .addGap(32, 32, 32)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(jLabel5)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addComponent(markSeenAllNotification))
                                                                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(displayNameLbl)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                                                                                .addComponent(logoutBtn))
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(jLabel6)
                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(acceptFriendBtn)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addComponent(ignoreFriendBtn)))))))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(logoutBtn)
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
                                                        .addComponent(removeGroupBtn)
                                                        .addComponent(jLabel5)
                                                        .addComponent(markSeenAllNotification))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jScrollPane5)
                                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
                                                .addGap(17, 17, 17)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel2)
                                                        .addComponent(unfriendBtn)
                                                        .addComponent(jLabel6))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(13, 13, 13)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                        .addComponent(acceptFriendBtn)
                                                                        .addComponent(ignoreFriendBtn)))
                                                        .addComponent(jScrollPane4)))
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
    private javax.swing.JButton acceptFriendBtn;
    private javax.swing.JTextField chatInputTextField;
    private javax.swing.JTextArea chatTextArea;
    private javax.swing.JLabel displayNameLbl;
    private javax.swing.JList<FriendOfferDto> friendOfferJList;
    private javax.swing.JList<FriendDto> friendsList;
    private javax.swing.JList<GroupDto> groupsList;
    private javax.swing.JButton ignoreFriendBtn;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JButton logoutBtn;
    private javax.swing.JButton markSeenAllNotification;
    private javax.swing.JList<NotificationDto> notificationJList;
    private javax.swing.JButton removeGroupBtn;
    private javax.swing.JButton searchBtn;
    private javax.swing.JTextField searchFriend;
    private javax.swing.JButton sendBtn;
    private javax.swing.JButton showNotificationBtn;
    private javax.swing.JButton unfriendBtn;
    // End of variables declaration//GEN-END:variables
}

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
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static client.context.CApplicationContext.service;
import static client.context.CApplicationContext.tcpClient;
import static client.view.LoginScreen.*;
import static common.dto.Command.*;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.ERROR_OPTION;
import static javax.swing.JOptionPane.*;
import static utils.FileHelper.deleteFileAsync;

/**
 * @author ledun
 */
public class ChatScreen extends AbstractScreen implements ResponseHandler, NetworkListener {

    public static final String SEARCH_KEYWORD = "SEARCH_KEYWORD";
    public static final String SEARCH_DONE = "SEARCH_DONE";
    private DefaultListModel<NotificationDto> notifyModel;
    private DefaultListModel<FriendOfferDto> friendOfferListModel;
    private DefaultListModel<FriendDto> friendListModel;

    private UserDto userDto;
    private Map<FriendDto, List<PrivateMessageDto>> friendChatTabMap;

    @Override
    public void onCreateView() {
        initComponents();

        friendChatTabMap = new HashMap<>();
        enterToSubmitCheck.setSelected(true);

        this.userDto = (UserDto) getData().get(USER);
        this.displayNameLbl.setText(userDto.getDisplayName());

        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_GROUP_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_UNSEEN_NOTIFICATIONS, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_UNSEEN_FRIEND_OFFERS, userDto.getId()));
    }

    @Override
    public void addEventListener() {

        logoutBtn.addActionListener(e -> onLogoutBtnActionPerformed());
        searchBtn.addActionListener(e -> openSearchResultScreen());

        acceptFriendBtn.addActionListener(e -> onAddFriendBtnActionPerformed());
        ignoreFriendBtn.addActionListener(e -> onIgnoreFriendBtnActionPerformed());
        unfriendBtn.addActionListener(e -> onUnfriendBtnActionPerformed());
        sendBtn.addActionListener(e -> performSendMessage());
        jButton3.addActionListener(e -> sendFileActionPerform());
        chatInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (enterToSubmitCheck.isSelected() && KeyEvent.VK_ENTER == e.getKeyCode()) {
                    performSendMessage();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        searchFriend.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSearchResultScreen();

                }
            }
        });

        this.friendsList.addListSelectionListener(e -> addNewChatFriendChatTab(friendsList.getSelectedValue()));
    }

    private void sendFileActionPerform() {
        FriendDto currentSelectedFriend = friendsList.getSelectedValue();
        if (currentSelectedFriend == null) {
            showMessageDialog(this, "You must select a least one friend to use this action");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        int result = fileChooser.showOpenDialog(this);

        if (result == APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                showMessageDialog(this, "Send directory is not supported");
                return;
            }

            readFileAsync(selectedFile, (data) -> {
                tcpClient.sendRequestAsync(new CommandObject(S2S_SEND_PRIVATE_FILE,
                        new SendFileRequestDto(data, userDto, currentSelectedFriend,
                                selectedFile.getName(), data.length / 1024 / 1024, true)));
            }, error -> {
                showMessageDialog(this, error.getMessage());
            });
        } else if (result == JFileChooser.ERROR_OPTION) {
            showMessageDialog(this, "An error occur");
        }
    }

    private void readFileAsync(File file, Consumer<byte[]> consumer, Consumer<Exception> exceptionHandler) {
        service.submit(() -> {
            Path path = Paths.get(file.getAbsolutePath());
            try {
                byte[] data = Files.readAllBytes(path);
                consumer.accept(data);
            } catch (IOException e) {
                exceptionHandler.accept(e);
                e.printStackTrace();
            }
        });
    }

    private void performSendMessage() {

        FriendDto friendDto = friendsList.getSelectedValue();
        if (friendDto == null) {
            showMessageDialog(this, "Cannot send message reason friend dto is null");
            return;
        }

        String content = chatInput.getText();
        if (content == null || content.isEmpty()) return;

        List<PrivateMessageDto> messageDtos = friendChatTabMap.get(friendDto);
        Long prevMessId = null;
        if (messageDtos.size() > 0) {
            PrivateMessageDto lastMessage = messageDtos.get(messageDtos.size() - 1);
            prevMessId = lastMessage.getId();

        }

        val sender = new UserDto();
        sender.setId(userDto.getId());

        String processedInput = chatInput.getText().trim();
        PrivateMessageDto privateMessageDto = new PrivateMessageDto(prevMessId, processedInput, sender, friendDto, false);

        System.out.println(privateMessageDto);
        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_PRIVATE_MESSAGE, privateMessageDto));
        chatInput.setText("");
    }

    private String generateTabName(FriendDto friendDto) {
        return "   " + friendDto.getDisplayName() + " - " + friendDto.getId() + "    ";
    }

    /**
     * @param friendDto
     */

    private void addNewChatFriendChatTab(FriendDto friendDto) {
        String name = generateTabName(friendDto);
        if (friendChatTabMap.containsKey(friendDto)) {
            int index = chatTabs.indexOfTab(name);

            if (index != -1) {
                chatTabs.setSelectedIndex(index);
                return;
            }
        }

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane();
        textArea.setColumns(20);
        textArea.setRows(5);
        scrollPane.setViewportView(textArea);

        chatTabs.addTab(name, scrollPane);
        int index = chatTabs.indexOfTab(name);

        JPanel newPanel = new JPanel(new GridBagLayout());
        newPanel.setOpaque(false);
        JLabel lblTitle = new JLabel(name);

        JButton btnClose = new JButton("x");
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusPainted(false);
        btnClose.setOpaque(false);
        GridBagConstraints constraint = new GridBagConstraints();

        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.weightx = 1;

        newPanel.add(lblTitle, constraint);

        constraint.gridx += 3;
        constraint.weightx = 3;

        newPanel.add(btnClose, constraint);

        chatTabs.setTabComponentAt(index, newPanel);
        chatTabs.setSelectedIndex(index);
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component selected = chatTabs.getSelectedComponent();

                if (selected != null) {
                    ((JButton) e.getSource()).removeActionListener(this);
                    chatTabs.remove(selected);
                    friendChatTabMap.remove(friendDto);
                }
            }
        };
        btnClose.addActionListener(actionListener);
        friendChatTabMap.put(friendDto, new ArrayList<>());
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_PRIVATE_MESSAGES, new RequestPrivateMessageDto(userDto.getId(), friendDto.getId(), 0, 100)));

    }


    private void onUnfriendBtnActionPerformed() {
        int[] friendsIds = friendsList.getSelectedIndices();
        if (friendsIds.length == 0) {
            showMessageDialog(this, "You have to select at least a friend to continue");
            return;
        }

        List<Long> friendIds = Arrays.stream(friendsIds).mapToObj(i -> friendListModel.get(i).getId()).collect(Collectors.toList());
        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_UNFRIEND_REQUEST, friendIds));
    }


    private void onIgnoreFriendBtnActionPerformed() {
        int[] indices = friendOfferJList.getSelectedIndices();
        if (indices.length == 0) {
            showMessageDialog(this, "You have to select at least an offer to perform this action");
            return;
        }

        List<FriendOfferDto> friends = Arrays.stream(indices).mapToObj(i -> {
            FriendOfferDto friendOfferDto = friendOfferListModel.get(i);
            friendOfferDto.setAccepted(false);
            return friendOfferDto;
        }).collect(Collectors.toList());

        for (FriendOfferDto friend : friends) {
            this.friendOfferListModel.removeElement(friend);
        }

        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_IGNORE_FRIEND_OFFERS, friends));
    }

    private void onAddFriendBtnActionPerformed() {
        int[] indices = friendOfferJList.getSelectedIndices();
        if (indices.length == 0) {
            showMessageDialog(this, "You have to select at least an offer to perform this action");
            return;
        }

        List<FriendOfferDto> friends = Arrays.stream(indices).mapToObj(i -> {
            FriendOfferDto friendOfferDto = friendOfferListModel.get(i);
            friendOfferDto.setAccepted(true);
            return friendOfferDto;
        }).collect(Collectors.toList());
        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_ACCEPT_FRIEND_OFFERS, friends));


    }

    private void onLogoutBtnActionPerformed() {
        Boolean rememberMe = (Boolean) getData().get(REMEMBER_ME);
        if (rememberMe != null && rememberMe) {
            deleteFileAsync(AUTH_TXT).thenAcceptAsync(r -> {
                if (r) {
                    System.out.println("DELETE FILE SUCCESS");
                }
            });
        }

        tcpClient.sendRequestAsync(new CommandObject(C2S_LOGOUT));
        finish();
    }

    public void openSearchResultScreen() {
        getData().put(SEARCH_KEYWORD, searchFriend.getText());

        Consumer<Set<FriendDto>> onSearchDone = this::onSearchDone;
        getData().put(SEARCH_DONE, onSearchDone);

        new Navigator<FriendSearchScreen>().navigate(getData(), false);
    }

    private void onSearchDone(Set<FriendDto> friendDtoList) {
        getData().remove(SEARCH_DONE);
        getData().remove(SEARCH_KEYWORD);
    }

    @Override
    public void listenOnNetworkEvent(CommandObject commandObject) {
        Command command = commandObject.getCommand();
        switch (command) {
            // DONE
            case S2C_GET_FRIEND_LIST_ACK: {
                List<FriendDto> friendDtoList = (List<FriendDto>) commandObject.getPayload();
                runOnUiThread(() -> updateFriendList(friendDtoList));
                break;
            }
            // DONE
            case S2C_GET_GROUP_LIST_ACK: {
                List<GroupDto> groupDtos = (List<GroupDto>) commandObject.getPayload();
                runOnUiThread(() -> updateGroupList(groupDtos));
                break;
            }
            // DONE

            case S2C_USER_NOT_FOUND: {
                runOnUiThread(() -> {
                    showMessageDialog(ChatScreen.this, "Cannot identify your account");
                });
                break;
            }
            case S2C_GET_UNSEEN_NOTIFICATION_ACK: {
                List<NotificationDto> notificationDtos = (List<NotificationDto>) commandObject.getPayload();
                List<Long> markSeenNotification = notificationDtos.stream()
                        .map(n -> n.getId()).collect(Collectors.toList());

                tcpClient.sendRequestAsync(new CommandObject(C2S_MARK_SEEN_NOTIFICATIONS, markSeenNotification));
                runOnUiThread(() -> updateNotification(notificationDtos));
                break;
            }
            case S2C_NOTIFY_NEW_FRIEND_OFFER: {
                FriendOfferDto notificationDto = (FriendOfferDto) commandObject.getPayload();
                runOnUiThread(() -> addNewFriendOfferToNotification(notificationDto));
                break;
            }

            case S2C_GET_UNSEEN_FRIEND_OFFERS_ACK: {
                List<FriendOfferDto> friendOfferDtos = (List<FriendOfferDto>) commandObject.getPayload();
                runOnUiThread(() -> updateFriendOfferList(friendOfferDtos));
                break;
            }

            case S2C_SEND_ACCEPT_FRIEND_OFFERS_ACK:
                int selectedIndex = this.friendOfferJList.getSelectedIndex();
                this.friendOfferListModel.remove(selectedIndex);
            case S2C_SEND_UNFRIEND_REQUEST_ACK: {
                tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
                break;
            }
            case S2C_PRIVATE_MESSAGE: {
                PrivateMessageDto receiveMessage = (PrivateMessageDto) commandObject.getPayload();
                List<PrivateMessageDto> privateMessages = friendChatTabMap.get(receiveMessage.getReceiver());
                privateMessages.add(receiveMessage);
                runOnUiThread(() -> updatePrivateMessageFor(receiveMessage.getReceiver()));
                break;
            }
            case S2C_GET_PRIVATE_MESSAGES_ACK: {
                ResponsePrivateMessageDto responsePrivateMessageDto = (ResponsePrivateMessageDto) commandObject.getPayload();
                if (this.friendChatTabMap.containsKey(responsePrivateMessageDto.getFriendDto())) {
                    this.friendChatTabMap
                            .get(responsePrivateMessageDto.getFriendDto())
                            .addAll(responsePrivateMessageDto.getMessageDtoList());
                } else {
                    this.friendChatTabMap.put(responsePrivateMessageDto.getFriendDto(), new ArrayList<>());
                }
                runOnUiThread(() -> updatePrivateMessageFor(responsePrivateMessageDto.getFriendDto()));

                break;
            }

            case S2C_RECEIVE_FILE: {
                receiveFileHandle(commandObject);
                break;
            }
            case C2S_SEND_PRIVATE_MESSAGE:
            case S2S_SEND_PRIVATE_FILE_ACK:
            case S2S_SEND_PRIVATE_FILE_NACK:
            case S2C_GET_PRIVATE_MESSAGES_NACK:
            case S2C_SEND_PRIVATE_MESSAGE_NACK:
            case S2C_GET_FRIEND_LIST_NACK:
            case S2C_SEND_IGNORE_FRIEND_OFFERS_NACK:
            case S2C_SEND_UNFRIEND_REQUEST_NACK:
            case S2C_GET_UNSEEN_FRIEND_OFFERS_NACK:
            case S2C_GET_UNSEEN_NOTIFICATION_NACK:
            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_NACK:
            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_FAIL:
                runOnUiThread(() -> showMessageDialog(ChatScreen.this, commandObject.getPayload()));
                break;
        }

    }

    private void receiveFileHandle(CommandObject commandObject) {
        SendFileRequestDto sendFileRequestDto = (SendFileRequestDto) commandObject.getPayload();
        runOnUiThread(() -> {
            int result = showConfirmDialog(this,
                    "You receive a file named " + sendFileRequestDto.getFileName() +
                            ", " + sendFileRequestDto.fileSize + " from " + sendFileRequestDto.getSender().getDisplayName() +
                            " do you want to save this file", "Notification", YES_NO_OPTION);
            if (result == YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "Director";
                    }
                });

                int saveResult = fileChooser.showSaveDialog(ChatScreen.this);
                if (saveResult == APPROVE_OPTION) {

                    saveFileAsync(sendFileRequestDto.getFileContent(), sendFileRequestDto.getFileName(),
                            fileChooser.getSelectedFile(),
                            () -> {
                                runOnUiThread(() -> {
                                    showMessageDialog(ChatScreen.this, "Save file success");
                                });
                            },
                            err -> {
                                runOnUiThread(() -> {
                                    showMessageDialog(ChatScreen.this, err.getMessage());
                                });
                            }
                    );
                } else if (saveResult == ERROR_OPTION) {
                    JOptionPane.showMessageDialog(this, "An error occur");
                }

            }

        });
    }

    private void saveFileAsync(byte[] fileContent, String fileName, File folderFile, Runnable success, Consumer<Exception> error) {
        File newFile = new File(folderFile, fileName);
        if (newFile.exists()) {
            int result = showConfirmDialog(ChatScreen.this,
                    "This file is exists click yes to overwrite this, no to save with unique name", "Confirm", YES_NO_OPTION);
            if (result == NO_OPTION) {
                String[] split = fileName.split("\\.");
                if (split.length >= 2) {
                    int beforeLastStringIdx = split.length - 2;
                    split[beforeLastStringIdx] = split[beforeLastStringIdx] + LocalDate.now().toString();
                }
                StringJoiner joiner = new StringJoiner("");
                Arrays.stream(split).forEach(joiner::add);
                System.out.println("FILE name" + joiner.toString());
                newFile = new File(folderFile, joiner.toString());
            }

            File finalNewFile = newFile;
            service.submit(() -> {
                try (FileOutputStream fileOutputStream = new FileOutputStream(finalNewFile)) {
                    fileOutputStream.write(fileContent);
                    success.run();
                } catch (Exception e) {
                    error.accept(e);
                }
            });
        }
    }

    private void updatePrivateMessageFor(FriendDto receiver) {
        String name = generateTabName(receiver);
        int index = chatTabs.indexOfTab(name);
        List<PrivateMessageDto> messageDtos = this.friendChatTabMap.get(receiver);

        JScrollPane selectedComponent = (JScrollPane) chatTabs.getComponentAt(index);
        Component[] components = selectedComponent.getComponents();
        for (Component component : components) {
            if (component instanceof JViewport) {
                for (Component component1 : ((JViewport) component).getComponents()) {
                    if (component1 instanceof JTextArea) {
                        String messagesTransformed = String.join("\n", messageDtos.stream().map(m -> {
                            String messageText = "";
                            if (m.getSender().getId().equals(userDto.getId())) {
                                messageText = "You";
                            } else {
                                messageText = m.getSender().getDisplayName();
                            }
                            messageText += " : " + m.getContent();
                            return messageText;
                        }).collect(Collectors.toList()));
                        ((JTextArea) component1).setText(messagesTransformed);
                    }
                }

            }
        }

    }

    private void updateNotification(List<NotificationDto> notificationDtos) {
        this.notifyModel = new DefaultListModel<NotificationDto>();
        notificationDtos.forEach(notifyModel::addElement);
        this.notificationJList.setModel(notifyModel);
    }

    private void updateFriendOfferList(List<FriendOfferDto> friendOfferDtos) {
        if (friendOfferListModel == null) {
            this.friendOfferListModel = new DefaultListModel<FriendOfferDto>();
        }
        friendOfferDtos.forEach(this.friendOfferListModel::addElement);
        this.friendOfferJList.setModel(friendOfferListModel);
    }

    private void addNewFriendOfferToNotification(FriendOfferDto friendOfferDto) {
        if (friendOfferListModel == null) {
            friendOfferListModel = new DefaultListModel<>();
        }
        friendOfferListModel.addElement(friendOfferDto);
    }

    private void updateGroupList(List<GroupDto> groupDtos) {
        val model = new DefaultListModel<GroupDto>();
        groupDtos.forEach(model::addElement);
        this.groupsList.setModel(model);
    }

    private void updateFriendList(List<FriendDto> friendDtoList) {
        this.friendListModel = new DefaultListModel<FriendDto>();
        friendDtoList.forEach(friendListModel::addElement);
        this.friendsList.setModel(friendListModel);
    }

    @Override
    public void registerNetworkListener() {
        tcpClient.registerListener(this);
    }

    @Override
    public void closeHandler() {
        tcpClient.closeHandler(this);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        searchFriend = new javax.swing.JTextField();
        searchBtn = new javax.swing.JButton();
        sendBtn = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
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
        enterToSubmitCheck = new javax.swing.JCheckBox();
        chatTabs = new javax.swing.JTabbedPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        chatInput = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setText("Search username");

        searchBtn.setText("Search");

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

        enterToSubmitCheck.setText("Enter to submit");

        chatInput.setColumns(20);
        chatInput.setRows(5);
        jScrollPane6.setViewportView(chatInput);

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
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(18, 18, 18)
                                                .addComponent(searchFriend, javax.swing.GroupLayout.PREFERRED_SIZE, 494, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(35, 35, 35)
                                                .addComponent(searchBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(chatTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 755, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 612, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(enterToSubmitCheck)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(sendBtn)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jButton3)))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
                                                .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)))
                                .addGap(32, 32, 32)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(acceptFriendBtn)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(ignoreFriendBtn))
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel6)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(displayNameLbl)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(logoutBtn))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                .addComponent(showNotificationBtn)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(jLabel5)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addComponent(markSeenAllNotification))
                                                                        .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE))))))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                                        .addComponent(chatTabs))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(enterToSubmitCheck)
                                                .addGap(18, 18, Short.MAX_VALUE)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(sendBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 1, Short.MAX_VALUE)
                                                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptFriendBtn;
    private javax.swing.JTextArea chatInput;
    private javax.swing.JTabbedPane chatTabs;
    private javax.swing.JLabel displayNameLbl;
    private javax.swing.JCheckBox enterToSubmitCheck;
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
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
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

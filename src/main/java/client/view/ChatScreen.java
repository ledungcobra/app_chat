/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.*;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import utils.Navigator;
import utils.ScreenStackManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
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

import static client.context.CApplicationContext.networkThreadService;
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

@Getter
@Setter
class ContainerObject {
    private FriendDto friendDto;
    private List<PrivateMessageDto> messageDtos = new ArrayList<>();

    public ContainerObject(FriendDto friendDto) {
        this.friendDto = friendDto;
    }
}

public class ChatScreen extends AbstractScreen implements ResponseHandler, NetworkListener {

    public static final String SEARCH_KEYWORD = "SEARCH_KEYWORD";
    public static final String SEARCH_DONE = "SEARCH_DONE";
    public static final String USER_COMMAND = "USER_COMMAND";
    public static final String GROUP_COMMAND = "GROUP_COMMAND";
    public static final String SEARCH_COMMAND = "SEARCH_COMMAND";
    private DefaultListModel<FriendOfferDto> friendOfferListModel;
    private DefaultListModel<FriendDto> friendListModel;

    private UserDto userDto;
    private Map<FriendDto, ContainerObject> friendChatTabMap;
    ButtonGroup buttonGroup;


    @Override
    public void onCreateView() {
        initComponents();
        buttonGroup = new ButtonGroup();

        buttonGroup.add(userRadio);
        buttonGroup.add(groupRadio);

        userRadio.setActionCommand(USER_COMMAND);
        groupRadio.setActionCommand(GROUP_COMMAND);

        userRadio.setSelected(true);

        friendChatTabMap = new HashMap<>();
        enterToSubmitCheck.setSelected(true);

        this.userDto = (UserDto) getData().get(USER);
        this.displayNameLbl.setText(userDto.getDisplayName());

        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_FRIEND_LIST, userDto.getId()));
        tcpClient.sendRequestAsync(new CommandObject(C2S_GET_GROUP_LIST, userDto.getId()));
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

        this.friendsList.addListSelectionListener(e -> addNewChatFriendChatTab(e));
        this.callVoiceBtn.addActionListener((e) -> chatVoiceActionPerformed());

    }

    private void chatVoiceActionPerformed() {
        int selectedIndex = friendsList.getSelectedIndex();

        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "You must select at least a friend to perform this action");
            return;
        }

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
        networkThreadService.submit(() -> {
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

//        FriendDto friendDto = friendsList.getSelectedValue();
        int index = chatTabs.getSelectedIndex();
        if (index == -1) {
            showMessageDialog(this, "You must select a least a friend to send message");
            return;
        }

        String selectedTab = chatTabs.getTitleAt(index);
        FriendDto friendDto = this.friendChatTabMap.entrySet()
                .stream()
                .filter(e -> selectedTab.equals(this.generateTabName(e.getKey())))
                .findFirst().map(e -> e.getKey())
                .orElse(null);

        if (friendDto == null) {
            showMessageDialog(this, "Friend cannot be null");
            return;
        }
        String content = chatInput.getText();
        if (content == null || content.isEmpty()) return;

        List<PrivateMessageDto> messageDtos = friendChatTabMap.get(friendDto).getMessageDtos();
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

    private void addNewChatFriendChatTab(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) return;
        JList friendList = (JList) e.getSource();
        FriendDto friendDto = (FriendDto) friendList.getSelectedValue();
        String tabName = generateTabName(friendDto);
        if (friendChatTabMap.containsKey(friendDto)) {

            // If contain tab
            int index = chatTabs.indexOfTab(tabName);
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

        chatTabs.addTab(tabName, scrollPane);
        int index = chatTabs.indexOfTab(tabName);

        JPanel newPanel = new JPanel(new GridBagLayout());
        newPanel.setOpaque(false);
        JLabel lblTitle = new JLabel(tabName);

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
                    chatTabs.remove(selected);
                    friendChatTabMap.remove(friendDto);
                    ((JButton) e.getSource()).removeActionListener(this);

                    if (friendChatTabMap.size() == 0) {
                        ChatScreen.this.friendsList.clearSelection();
                    }

                }
            }
        };

        btnClose.addActionListener(actionListener);
        friendChatTabMap.put(friendDto, new ContainerObject(friendDto));
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
        if (buttonGroup.getSelection().getActionCommand().equals(USER_COMMAND)) {
            Consumer<Set<FriendDto>> onSearchDone = this::onSearchDone;
            getData().put(SEARCH_DONE, onSearchDone);
        } else {

        }

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
            case S2C_SEND_PRIVATE_MESSAGE_ACK: {
                PrivateMessageDto receiveMessage = (PrivateMessageDto) commandObject.getPayload();
                if (friendChatTabMap.get(receiveMessage.getReceiver()) == null) {
                    friendChatTabMap.put(receiveMessage.getReceiver(), new ContainerObject(receiveMessage.getReceiver()));
                }
                friendChatTabMap.get(receiveMessage.getReceiver()).getMessageDtos()
                        .add(receiveMessage);
                runOnUiThread(() ->
                        updatePrivateMessageFor(receiveMessage.getReceiver())
                );
                break;
            }
            case S2C_RECEIVE_A_PRIVATE_MESSAGE: {
                PrivateMessageDto receiveMessage = (PrivateMessageDto) commandObject.getPayload();
                FriendDto sender = Mapper.map(receiveMessage.getSender());
                if (!friendChatTabMap.containsKey(sender)) {
                    friendChatTabMap.put(sender, new ContainerObject(sender));
                }
                friendChatTabMap.get(sender).getMessageDtos()
                        .add(receiveMessage);
                runOnUiThread(() ->
                        updatePrivateMessageFor(sender)
                );
                break;
            }
            case S2C_GET_PRIVATE_MESSAGES_ACK: {
                ResponsePrivateMessageDto responsePrivateMessageDto = (ResponsePrivateMessageDto) commandObject.getPayload();
                if (!this.friendChatTabMap.containsKey(responsePrivateMessageDto.getFriendDto())) {
                    this.friendChatTabMap
                            .put(responsePrivateMessageDto.getFriendDto(), new ContainerObject(responsePrivateMessageDto.getFriendDto()));
                }
                this.friendChatTabMap
                        .get(responsePrivateMessageDto.getFriendDto())
                        .getMessageDtos()
                        .addAll(responsePrivateMessageDto.getMessageDtoList());
                runOnUiThread(() -> updatePrivateMessageFor(responsePrivateMessageDto.getFriendDto()));

                break;
            }

            case S2C_RECEIVE_FILE: {
                receiveFileHandle(commandObject);
                break;
            }

            case SERVER_STOP_SIGNAL: {
                runOnUiThread(() -> {
                    ScreenStackManager.getInstance().popTo(LoginScreen.class);
                });
                break;
            }

            case S2S_SEND_PRIVATE_FILE_ACK:
            case S2S_SEND_PRIVATE_FILE_NACK:
            case S2C_GET_PRIVATE_MESSAGES_NACK:
            case S2C_SEND_PRIVATE_MESSAGE_NACK:
            case S2C_GET_FRIEND_LIST_NACK:
            case S2C_SEND_IGNORE_FRIEND_OFFERS_NACK:
            case S2C_SEND_UNFRIEND_REQUEST_NACK:
            case S2C_GET_UNSEEN_FRIEND_OFFERS_NACK:
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
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);

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
        File finalFile = null;

        if (newFile.exists()) {
            int result = showConfirmDialog(ChatScreen.this,
                    "This file is exists click yes to overwrite this, no to save with unique name", "Confirm", YES_NO_OPTION);
            if (result == NO_OPTION) {
                String[] split = fileName.split("\\.");
                if (split.length >= 2) {
                    int beforeLastStringIdx = split.length - 2;
                    split[beforeLastStringIdx] = split[beforeLastStringIdx] + LocalDate.now().toString();
                }
                StringJoiner joiner = new StringJoiner(".");
                Arrays.stream(split).forEach(joiner::add);
                System.out.println("FILE name" + joiner.toString());
                newFile = new File(folderFile, joiner.toString());
            }
            finalFile = newFile;


        } else {
            try {
                if (newFile.createNewFile()) {
                    System.out.println("Create file sucess");
                    finalFile = newFile;
                }
            } catch (IOException e) {
                error.accept(e);
                return;
            }
        }

        File finalFile1 = finalFile;
        networkThreadService.submit(() -> {
            try (FileOutputStream fileOutputStream = new FileOutputStream(finalFile1)) {
                fileOutputStream.write(fileContent);
                success.run();
            } catch (Exception e) {
                error.accept(e);
            }
        });
    }

    private void updatePrivateMessageFor(FriendDto receiver) {
        String tabName = generateTabName(receiver);
        int index = chatTabs.indexOfTab(tabName);
        List<PrivateMessageDto> messageDtos = this.friendChatTabMap.get(receiver).getMessageDtos();

        JScrollPane selectedComponent = (JScrollPane) chatTabs.getComponentAt(index);
        Component[] components = selectedComponent.getComponents();

        out:
        for (Component component : components) {
            if (component instanceof JViewport) {
                for (Component component1 : ((JViewport) component).getComponents()) {
                    if (component1 instanceof JTextArea) {
                        String messagesTransformed = String.join("\n", messageDtos.stream().map(m -> {
                            String messageText = "";
                            if (m.getSender().getId().equals(userDto.getId())) {
                                messageText = "you";
                            } else {
                                messageText = m.getSender().getDisplayName();
                            }
                            messageText += " : " + m.getContent();
                            return messageText;
                        }).collect(Collectors.toList()));
                        ((JTextArea) component1).setText(messagesTransformed);
                        break out;
                    }
                }

            }
        }

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

        logoutBtn = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        displayNameLbl = new javax.swing.JLabel();
        chatTabs = new javax.swing.JTabbedPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        chatInput = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        searchFriend = new javax.swing.JTextField();
        userRadio = new javax.swing.JRadioButton();
        groupRadio = new javax.swing.JRadioButton();
        searchBtn = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        sendBtn = new javax.swing.JButton();
        enterToSubmitCheck = new javax.swing.JCheckBox();
        jButton3 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        acceptFriendBtn = new javax.swing.JButton();
        ignoreFriendBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        friendOfferJList = new javax.swing.JList<>();
        jPanel4 = new javax.swing.JPanel();
        removeGroupBtn = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        groupsList = new javax.swing.JList<>();
        jPanel5 = new javax.swing.JPanel();
        unfriendBtn = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        friendsList = new javax.swing.JList<>();
        callVoiceBtn = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        logoutBtn.setText("Logout");

        jLabel4.setText("Hi");

        displayNameLbl.setText("display name");

        chatInput.setColumns(20);
        chatInput.setRows(5);
        jScrollPane6.setViewportView(chatInput);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Search"));

        userRadio.setText("User");

        groupRadio.setText("Group");
        groupRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupRadioActionPerformed(evt);
            }
        });

        searchBtn.setText("Search");

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setText("Keyword");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(searchFriend, javax.swing.GroupLayout.PREFERRED_SIZE, 494, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(23, 23, 23)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(userRadio)
                                        .addComponent(groupRadio))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(searchBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                        .addComponent(searchFriend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(jLabel1))
                                                                .addGap(13, 13, 13))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                                .addComponent(userRadio)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(groupRadio))))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(21, 21, 21)
                                                .addComponent(searchBtn)))
                                .addContainerGap())
        );

        sendBtn.setText("Send");

        enterToSubmitCheck.setText("Enter to submit");

        jButton3.setText("File");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(enterToSubmitCheck)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(sendBtn)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jButton3)))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(enterToSubmitCheck)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sendBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Friend offer"));

        acceptFriendBtn.setText("Accept");

        ignoreFriendBtn.setText("Ignore");

        jScrollPane2.setViewportView(friendOfferJList);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(acceptFriendBtn)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(ignoreFriendBtn)))
                                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ignoreFriendBtn)
                                        .addComponent(acceptFriendBtn))
                                .addGap(0, 0, 0))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Group"));

        removeGroupBtn.setBackground(new java.awt.Color(255, 51, 0));
        removeGroupBtn.setForeground(new java.awt.Color(255, 51, 51));
        removeGroupBtn.setText("Leave");

        jScrollPane3.setViewportView(groupsList);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(removeGroupBtn)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(removeGroupBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Friend"));

        unfriendBtn.setBackground(new java.awt.Color(255, 0, 0));
        unfriendBtn.setForeground(new java.awt.Color(255, 0, 51));
        unfriendBtn.setText("Unfriend");

        jScrollPane4.setViewportView(friendsList);

        callVoiceBtn.setText("Call voice");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(unfriendBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(callVoiceBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(unfriendBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(callVoiceBtn)
                                .addGap(13, 13, 13)
                                .addComponent(jScrollPane4)
                                .addContainerGap())
        );

        jMenu3.setText("Group management");
        jMenuBar1.add(jMenu3);

        jMenu2.setText("Logout");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(chatTabs)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 628, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(3, 3, 3)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel4)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(displayNameLbl)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(logoutBtn))
                                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(logoutBtn)
                                                .addComponent(jLabel4)
                                                .addComponent(displayNameLbl)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(chatTabs, javax.swing.GroupLayout.PREFERRED_SIZE, 548, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                                                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGap(0, 0, Short.MAX_VALUE))))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void groupRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupRadioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_groupRadioActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptFriendBtn;
    private javax.swing.JButton callVoiceBtn;
    private javax.swing.JTextArea chatInput;
    private javax.swing.JTabbedPane chatTabs;
    private javax.swing.JLabel displayNameLbl;
    private javax.swing.JCheckBox enterToSubmitCheck;
    private javax.swing.JList<FriendOfferDto> friendOfferJList;
    private javax.swing.JList<FriendDto> friendsList;
    private javax.swing.JRadioButton groupRadio;
    private javax.swing.JList<GroupDto> groupsList;
    private javax.swing.JButton ignoreFriendBtn;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JButton logoutBtn;
    private javax.swing.JButton removeGroupBtn;
    private javax.swing.JButton searchBtn;
    private javax.swing.JTextField searchFriend;
    private javax.swing.JButton sendBtn;
    private javax.swing.JButton unfriendBtn;
    private javax.swing.JRadioButton userRadio;
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.CommandObject;
import common.dto.FriendDto;
import common.dto.FriendOfferDto;
import lombok.val;
import utils.ScreenStackManager;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static client.context.CApplicationContext.tcpClient;
import static client.view.ChatScreen.*;
import static common.dto.Command.*;

/**
 * @author ledun
 */
public class FriendSearchScreen extends AbstractScreen implements ResponseHandler, AbstractScreen.NetworkListener
{


    public static final String LIST_FRIEND_RESULT = "LIST_FRIEND_RESULT";
    private Set<FriendDto> selectedFriend = new HashSet<>();
    private List<FriendDto> loadedFriendDtoList = new ArrayList<>();

    @Override
    public void onCreateView()
    {
        initComponents();
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    public void addEventListener()
    {
        this.addFriendBtn.addActionListener(e -> onAddFriendBtnActionPerformed());
        Consumer<Set<FriendDto>> onSearchDone = (Consumer<Set<FriendDto>>) getData().get(SEARCH_DONE);
        this.addWindowListener(new WindowListener()
        {
            @Override
            public void windowOpened(WindowEvent e)
            {

            }

            @Override
            public void windowClosing(WindowEvent e)
            {

            }

            @Override
            public void windowClosed(WindowEvent e)
            {
                onSearchDone.accept(FriendSearchScreen.this.selectedFriend);
                FriendSearchScreen.this.closeHandler();
            }

            @Override
            public void windowIconified(WindowEvent e)
            {

            }

            @Override
            public void windowDeiconified(WindowEvent e)
            {

            }

            @Override
            public void windowActivated(WindowEvent e)
            {

            }

            @Override
            public void windowDeactivated(WindowEvent e)
            {

            }
        });
    }

    private void onAddFriendBtnActionPerformed()
    {
        if (loadedFriendDtoList == null || loadedFriendDtoList.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "There are no friend");
            return;
        }
        int[] indices = resultList.getSelectedIndices();

        for (int i : indices)
        {
            selectedFriend.add(loadedFriendDtoList.get(i));
        }


        val friendIds = (selectedFriend
                .stream().map(f -> f.getId())
                .collect(Collectors.toList()).toArray(new Long[0]));


        tcpClient.sendRequestAsync(new CommandObject(C2S_SEND_ADD_FRIEND_OFFER_TO_FRIENDS, friendIds));
    }


    @Override
    public void listenOnNetworkEvent(CommandObject commandObject)
    {
        System.out.println(getClass().getSimpleName());
        switch (commandObject.getCommand())
        {
            case S2C_FIND_FRIEND_BY_KEYWORD_ACK:
            {
                List<FriendDto> friendDtoList = (List<FriendDto>) commandObject.getPayload();
                this.loadedFriendDtoList = friendDtoList;
                runOnUiThread(this::updateFriendList);
                break;
            }
            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_ACK:
            {
                runOnUiThread(() -> JOptionPane.showMessageDialog(FriendSearchScreen.this, "Sent add friend offer to your peer success"));
                break;
            }

            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_NACK:
            {
                runOnUiThread(() -> JOptionPane.showMessageDialog(FriendSearchScreen.this, "Sent add friend offer to your peer not accept"));
                break;
            }

            case SERVER_STOP_SIGNAL: {
                runOnUiThread(() -> {
                    ScreenStackManager.getInstance().popTo(LoginScreen.class);
                });
                break;
            }

            case S2C_SEND_ADD_FRIEND_OFFERS_TO_FRIENDS_FAIL:
            case S2C_FIND_FRIEND_BY_KEYWORD_NACK:
            {
                runOnUiThread(() -> JOptionPane.showMessageDialog(FriendSearchScreen.this, commandObject.getPayload()));
                break;
            }

        }
    }

    private void updateFriendList()
    {
        val model = new DefaultListModel<FriendDto>();
        loadedFriendDtoList.forEach(model::addElement);
        this.resultList.setModel(model);

    }


    @Override
    public void registerNetworkListener()
    {
        tcpClient.registerListener(this);
        tcpClient.sendRequestAsync(new CommandObject(C2S_FIND_FRIEND_BY_KEYWORD, getData().get(SEARCH_KEYWORD)));
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

        jScrollPane1 = new javax.swing.JScrollPane();
        resultList = new javax.swing.JList<>();
        addFriendBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);


        jScrollPane1.setViewportView(resultList);

        addFriendBtn.setBackground(new java.awt.Color(204, 255, 51));
        addFriendBtn.setText("Add Friend");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(addFriendBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(32, 32, 32)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 461, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(43, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(addFriendBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 510, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(40, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFriendBtn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<FriendDto> resultList;

    // End of variables declaration//GEN-END:variables
}

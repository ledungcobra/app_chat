/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.view;

import client.core.ResponseHandler;
import common.dto.Command;
import common.dto.CommandObject;
import common.dto.FriendDto;
import common.dto.ReceiverVoiceData;
import utils.SoundUtils;
import utils.VoiceCallUtils;

import javax.swing.*;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static client.context.CApplicationContext.tcpClient;

/**
 * @author ledun
 */


public class CallVoiceScreen extends AbstractScreen implements AbstractScreen.NetworkListener, ResponseHandler {


    public static final String CALL_FRIEND_OBJECT = "CALL_FRIEND_OBJECT";
    public static final String ACCEPT = "Accept";
    public static final String MUTE = "Mute";
    public static final String UNMUTE = "Unmute";
    public static final String DECLINE = "Decline";
    public static final String STOP_CALL = "Stop call";
    public static final String CALL_MODE = "CALL_MODE";
    private Runnable stopSoundHandler;
    private Runnable stopSendHandler;
    private Runnable stopPlayHandler;


    private ExecutorService callingService = Executors.newFixedThreadPool(2);
    private FriendDto friendDto;

    public enum CallMode {
        SEND_A_VOICE_CALL,
        RECEIVE_A_VOICE_CALL
    }

    @Override
    public void onCreateView() {
        initComponents();
        if (getData() == null) throw new RuntimeException();
        friendDto = (FriendDto) getData().get(CALL_FRIEND_OBJECT);
        CallMode mode = (CallMode) getData().get(CALL_MODE);

        if (friendDto == null) {
            throw new RuntimeException();
        }

        if (mode.equals(CallMode.SEND_A_VOICE_CALL)) {
            // TODO C2S_MAKE_A_VOICE_CALL
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_MAKE_A_VOICE_CALL));
        } else if (mode.equals(CallMode.RECEIVE_A_VOICE_CALL)) {
            jButton1.setText(ACCEPT);
            jButton2.setText(DECLINE);
            stopSoundHandler = SoundUtils.playSound("callsound.mp3");
        }

        this.jLabel2.setText(friendDto.getDisplayName());

    }

    @Override
    public void addEventListener() {
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
                stopSoundHandler.run();
                if (stopSendHandler != null) {
                    stopSendHandler.run();
                }
                if (stopPlayHandler != null)
                    stopPlayHandler.run();

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
        this.jButton1.addActionListener((e) -> onLeftButtonClick());
        this.jButton2.addActionListener((e) -> onRightButtonClick());
    }

    private void onRightButtonClick() {
        if (jButton2.getText().equals(DECLINE)) {
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_DECLINE_A_VOICE_CALL, friendDto));
        } else if (jButton2.getText().equals(STOP_CALL)) {
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_STOP_VOICE_CHAT, friendDto));
        }
        finish();

    }

    private void onLeftButtonClick() {
        if (jButton1.getText().equals(ACCEPT)) {
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_ACCEPT_A_VOICE_CALL, friendDto));
            updateUIForCalling();
        } else if (jButton1.getText().equals(MUTE)) {
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_MUTE_VOICE, friendDto));
            stopSendHandler.run();
            jButton1.setText(UNMUTE);
        } else if (jButton1.getText().equals(UNMUTE)) {
            tcpClient.sendRequestAsync(new CommandObject(Command.C2S_UNMUTE_VOICE, friendDto));

            jButton1.setText(MUTE);
        }
    }

    private void updateUIForCalling() {
        jButton1.setText(MUTE);
        jButton2.setText(STOP_CALL);
    }


    @Override
    public void listenOnNetworkEvent(CommandObject commandObject) {
        switch (commandObject.getCommand()) {
            case S2C_MAKE_A_VOICE_CALL_ACK: {
                runOnUiThread(() -> {
                    runOnUiThread(this::updateUIForCalling);
                    JOptionPane.showMessageDialog(this, "Wait for your friend accept");
                });
                break;
            }

            case S2C_FRIEND_ACCEPT_A_VOICE_CALL:
            case S2C_START_A_VOICE_CALL: {
                // Set up for start a voice call
                startSendAudio();
                break;
            }

            case S2C_RECEIVE_VOICE_CHUNK: {
                startPlayAudio(commandObject);
                break;
            }

            case S2C_SEND_VOICE_CHUNK_NACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Error when sending voice data");
                });
                break;
            }

            case S2C_FRIEND_DECLINE_A_VOICE_CALL: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Your friend decline a call");
                    finish();
                });
                break;
            }

            case S2C_MUTE_VOICE_ACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Your friend's microphone is muted ");
                });
                break;
            }

            case S2C_UNMUTE_VOICE_ACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Your friend's microphone is un muted ");
                });
                break;
            }

            case S2C_STOP_VOICE_CHAT_ACK: {
                runOnUiThread(() -> {
                    JOptionPane.showMessageDialog(this, "Your friend stop a call");
                });
                break;
            }

            case S2C_STOP_VOICE_CHAT_NACK:
            case S2C_UNMUTE_VOICE_NACK:
            case S2C_MUTE_VOICE_NACK:
            case S2C_DECLINE_A_VOICE_CALL_NACK:
            case S2C_ACCEPT_A_VOICE_CALL_NACK:
            case S2C_MAKE_A_VOICE_CALL_NACK: {
                runOnUiThread(() ->
                        JOptionPane.showMessageDialog(this, commandObject.getPayload()));
                break;
            }

        }
    }

    private void startPlayAudio(CommandObject commandObject) {
        callingService.submit(() -> {
            ReceiverVoiceData receiverVoiceData = (ReceiverVoiceData) commandObject.getPayload();
            stopPlayHandler = VoiceCallUtils.playOnSpeaker(receiverVoiceData.getData());
        });
    }

    private void startSendAudio() {
        runOnUiThread(this::updateUIForCalling);
        callingService.submit(() -> {
            stopSendHandler = VoiceCallUtils.voiceCall(friendDto);
        });
    }

    @Override
    public void closeHandler() {
        tcpClient.closeHandler(this);
    }

    @Override
    public void registerNetworkListener() {
        tcpClient.registerListener(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton1.setText("Mute");

        jButton2.setText("Stop call");

        jLabel1.setText("Your friend");

        jLabel2.setText("FriendName");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jButton1)
                                                .addGap(69, 69, 69)
                                                .addComponent(jButton2))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel2)))
                                .addContainerGap(27, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(41, 41, 41)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton2)
                                        .addComponent(jButton1))
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
            java.util.logging.Logger.getLogger(CallVoiceScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CallVoiceScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CallVoiceScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CallVoiceScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CallVoiceScreen().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;


    // End of variables declaration//GEN-END:variables
}

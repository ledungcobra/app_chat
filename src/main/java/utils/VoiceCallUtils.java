package utils;

import common.dto.Command;
import common.dto.CommandObject;
import common.dto.FriendDto;
import common.dto.SenderVoiceData;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static client.context.CApplicationContext.tcpClient;

public class VoiceCallUtils {
    private static final float sampleRate = 8000;
    private static final int sampleSizeInBits = 8;
    private static final int channels = 1;
    public static final AudioFormat FORMAT = new AudioFormat(sampleRate, sampleSizeInBits, channels, true, true);

    public static Runnable voiceCall(FriendDto receiver) {
        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Unsupported");
        }
        AtomicBoolean stopped = new AtomicBoolean(false);

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int bufferSize = line.getBufferSize() / 5;
            byte[] data = new byte[bufferSize];
            // Begin audio capture.
            line.start();

            while (!stopped.get()) {
                line.read(data, 0, data.length);
                tcpClient.sendRequestAsync(new CommandObject(Command.C2S_SEND_VOICE_CHUNK, new SenderVoiceData(data, receiver)));
            }
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }


        TargetDataLine finalLine = line;
        return () -> {
            if (finalLine == null) return;
            stopped.set(true);
            finalLine.close();
        };
    }

    public static Runnable playOnSpeaker(byte[] bytes) {
        SourceDataLine speakers = null;
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            speakers.open(FORMAT);
            speakers.start();

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        SourceDataLine finalSpeakers = speakers;
        return () -> {
            if (finalSpeakers == null) return;
            finalSpeakers.drain();
            finalSpeakers.close();
        };

    }
}

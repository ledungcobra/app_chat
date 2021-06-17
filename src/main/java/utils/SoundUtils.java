package utils;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SoundUtils {

    public static Runnable playSound(String filePath) {
        AudioStream as = null;
        try (InputStream in = new FileInputStream(filePath);) {
            as = new AudioStream(in);
            AudioPlayer.player.start(as);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AudioStream finalAs = as;
        return () -> {
            AudioPlayer.player.stop(finalAs);
        };
    }

}

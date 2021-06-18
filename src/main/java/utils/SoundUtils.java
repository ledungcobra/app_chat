package utils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class SoundUtils {

    public static Runnable playSound(String filePath) {

        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new File(filePath)));
            clip.start();

        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }

        Clip finalClip = clip;
        return finalClip::stop;
    }

}

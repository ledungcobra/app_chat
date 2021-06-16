package utils;

import java.io.*;
import java.util.Properties;

public class PropertiesFileUtils {

    public static Properties readPropertiesFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(filename)) {
            properties.load(fileInputStream);
            return properties;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writePropertiesFile(String filename, Properties properties) {
        File file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileOutputStream fileInputStream = new FileOutputStream(filename)) {

            properties.store(fileInputStream, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

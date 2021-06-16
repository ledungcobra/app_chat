package utils;

import javax.swing.*;

public class UIUtils {
    public static boolean checkRequired(String port, String s) {
        if (port == null || port.isEmpty()) {
            JOptionPane.showMessageDialog(null , s);
            return true;
        }
        return false;
    }

}

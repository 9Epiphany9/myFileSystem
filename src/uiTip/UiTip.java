package uiTip;

import javax.swing.JOptionPane;

public class UiTip {
    public static void tip(String message) {
        JOptionPane.showMessageDialog(null, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }
}
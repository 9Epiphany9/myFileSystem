package uiTip;

import javax.swing.JOptionPane;

/**
 * 提示窗口
 */
public class UiTip {
    public static void tip(String message) {
        JOptionPane.showMessageDialog(null, message, "警告", JOptionPane.WARNING_MESSAGE);
    }
}
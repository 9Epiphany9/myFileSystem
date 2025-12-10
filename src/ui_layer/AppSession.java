package ui_layer;

import sys_core.FCB;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.HashMap;

public class AppSession {
    private static JPanel rootPanel;
    private static ArrayList<FCB> currentPath;
    private static HashMap<FCB, JButton> iconMap;

    static {
        currentPath = new ArrayList<>();
        iconMap = new HashMap<>();
    }

    public static JPanel getRootPanel() { return rootPanel; }
    public static void setRootPanel(JPanel panel) { rootPanel = panel; }
    public static ArrayList<FCB> getCurrentPath() { return currentPath; }
    public static void setCurrentPath(ArrayList<FCB> path) { currentPath = path; }
    public static HashMap<FCB, JButton> getIconMap() { return iconMap; }
}
package application;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

import kernel.FileManage;
import kernel.FileMsg;

public class Contain {
    public static JPanel rootPanel;
    public static ArrayList<FileMsg> path = new ArrayList<>();
    public static HashMap<FileMsg, JButton> map = new HashMap<>();

    public static void refreshDirectory() {
        rootPanel.removeAll();
        HashMap<FileMsg, JButton> currentMap = FileManage.dir(path, map);
        map.clear();
        map.putAll(currentMap);

        for (JButton btn : map.values()) {
            rootPanel.add(btn);
        }

        rootPanel.revalidate();
        rootPanel.repaint();
    }
    public Contain(JPanel rootPanel) {
        this.rootPanel = rootPanel;
        path = new ArrayList<>();
        map = new HashMap<>();
    }

}

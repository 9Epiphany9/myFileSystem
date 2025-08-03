package application;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JButton;
import kernel.FileMsg;

public class Contain {
    private static JPanel rootPane;
    private static ArrayList<FileMsg> path;
    private static HashMap<FileMsg, JButton> map;

    public Contain(JPanel rootPane) {
        Contain.rootPane = rootPane;
        Contain.path = new ArrayList<>();
        Contain.map = new HashMap<>();
    }

    public static JPanel getRootPane() {
        return rootPane;
    }

    public static void setRootPane(JPanel rootPane) {
        Contain.rootPane = rootPane;
    }

    public static ArrayList<FileMsg> getPath() {
        return path;
    }

    public static void setPath(ArrayList<FileMsg> path) {
        Contain.path = path;
    }

    public static HashMap<FileMsg, JButton> getMap() {
        return map;
    }

    public static void setMap(HashMap<FileMsg, JButton> map) {
        Contain.map = map;
    }
}
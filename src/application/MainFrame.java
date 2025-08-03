package application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import kernel.Disk;
import kernel.FileManage;
import kernel.FileMsg;
import kernel.Oftle;
import uiTip.UiTip;

public class MainFrame extends JFrame {
    private JPanel filePanel;
    private JTable openFilesTable;
    private JButton backButton;
    private JButton newButton;
    private JButton printDiskButton; // 新增调试按钮

    public MainFrame() {
        setTitle("磁盘文件系统");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        // 初始化 Contain
        filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 35, 35));
        filePanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        Contain.setRootPane(filePanel);
        Contain.setPath(new java.util.ArrayList<>());
        Contain.setMap(new HashMap<>());

        // 初始化磁盘和根目录
        Disk.start();
        FileMsg root = new FileMsg("roo".getBytes(), "  ".getBytes(), (byte) 8, (byte) 2, (byte) 0);
        Contain.getPath().add(root);
        updateFilePanel();

        // 工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("后退");
        newButton = new JButton("新建");
        printDiskButton = new JButton("打印磁盘"); // 新增按钮
        toolBar.add(backButton);
        toolBar.add(newButton);
        toolBar.add(printDiskButton); // 添加到工具栏
        add(toolBar, BorderLayout.NORTH);

        // 文件浏览器区域
        JScrollPane fileScrollPane = new JScrollPane(filePanel);
        add(fileScrollPane, BorderLayout.CENTER);

        // 打开文件表
        openFilesTable = new JTable(FileManage.getOfList().getOpenedFilesTable());
        JScrollPane tableScrollPane = new JScrollPane(openFilesTable);
        tableScrollPane.setPreferredSize(new Dimension(800, 150));
        add(tableScrollPane, BorderLayout.SOUTH);

        // 后退按钮事件
        backButton.addActionListener(e -> {
            if (Contain.getPath().size() == 1) {
                UiTip.tip("当前已经是根目录，无法后退。");
                return;
            }
            Contain.getPath().remove(Contain.getPath().size() - 1);
            updateFilePanel();
        });

        // 新建按钮事件
        newButton.addActionListener(e -> {
            BuildDialog dialog = new BuildDialog(this);
            dialog.setVisible(true);
        });

        // 打印磁盘按钮事件
        printDiskButton.addActionListener(e -> {
            Disk.diskPrint(); // 调用调试方法
        });

        setVisible(true);
    }

    private void updateFilePanel() {
        filePanel.removeAll();
        HashMap<FileMsg, JButton> map = FileManage.dir(Contain.getPath(), Contain.getMap());
        Set<Map.Entry<FileMsg, JButton>> entryAll = Contain.getMap().entrySet();
        for (Map.Entry<FileMsg, JButton> ent : entryAll) {
            ent.getValue().setVisible(false);
        }
        for (Map.Entry<FileMsg, JButton> ent : map.entrySet()) {
            JButton button = ent.getValue();
            button.setVisible(true);
            filePanel.add(button);
        }
        filePanel.revalidate();
        filePanel.repaint();
    }

    public static void main(String[] args) {
        // 程序启动时创建data.txt
        try {
            new File("data.txt").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 注册关闭钩子，程序退出时删除data.txt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File dataFile = new File("data.txt");
            if (dataFile.exists()) {
                if (dataFile.delete()) {
                    System.out.println("data.txt 已删除");
                } else {
                    System.err.println("无法删除 data.txt");
                }
            }
        }));

        SwingUtilities.invokeLater(MainFrame::new);
    }
}
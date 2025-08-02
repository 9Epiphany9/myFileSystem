package application;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import kernel.Disk;
import kernel.FileManage;
import kernel.FileMsg;
import uiTip.UiTip;

public class SamplePane extends JFrame {
    private JButton btnBack;
    private JButton btnNewBuild;
    private JTable tableView;
    public static JPanel rootPanel;
    public static JFrame frame;

    public SamplePane() {
        frame = this;
        setLayout(new BorderLayout(5, 5)); // Add some gap between components

        // Create rootPanel with larger size
        rootPanel = new JPanel();
        rootPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 25, 25)); // Reduced gaps between items
        rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JScrollPane scrollPane = new JScrollPane(rootPanel);
        scrollPane.setPreferredSize(new Dimension(800, 400)); // Larger scrollable area

        new Contain(rootPanel);

        // Button panel with some styling
        btnBack = new JButton("后退");
        btnNewBuild = new JButton("新建");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(btnBack);
        buttonPanel.add(btnNewBuild);

        // Initialize disk and root directory
        Disk.start();
        FileMsg roo = new FileMsg("roo".getBytes(), "  ".getBytes(), (byte) 8, (byte) 2, (byte) 0);
        Contain.path.add(roo);

        // Button actions
        btnNewBuild.addActionListener(e -> {
            Build buildFrame = new Build();
            buildFrame.setVisible(true);
        });

        btnBack.addActionListener(e -> {
            if (Contain.path.size() == 1) {
                UiTip.tip("当前已经是根目录，无法后退。");
                return;
            }
            Contain.path.remove(Contain.path.size() - 1);
            HashMap<FileMsg, JButton> map = FileManage.dir(Contain.path, Contain.map);
            for (Map.Entry<FileMsg, JButton> ent : Contain.map.entrySet()) {
                ent.getValue().setVisible(false);
            }
            for (Map.Entry<FileMsg, JButton> ent : map.entrySet()) {
                ent.getValue().setVisible(true);
            }
            rootPanel.revalidate();
            rootPanel.repaint();
        });

        // Table configuration
        tableView = new JTable(FileManage.getOfList().getTableModel());
        tableView.setEnabled(false);
        JScrollPane tableScrollPane = new JScrollPane(tableView);
        tableScrollPane.setPreferredSize(new Dimension(800, 150)); // Smaller table area

        // Add components to frame with adjusted proportions
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER); // This gets most space
        add(tableScrollPane, BorderLayout.SOUTH);

        Disk.start(); // 确保磁盘文件存在

        // 尝试加载根目录
        FileMsg root = Disk.loadRootDirectory();
        if (root == null) {
            // 如果是全新启动，创建默认根目录
            root = new FileMsg("roo".getBytes(), "  ".getBytes(), (byte)8, (byte)2, (byte)0);
            // 将根目录信息写入磁盘块2
            byte[] rootBlock = new byte[64];
            System.arraycopy("roo".getBytes(), 0, rootBlock, 0, 3);
            System.arraycopy("  ".getBytes(), 0, rootBlock, 3, 2);
            rootBlock[5] = 8; // 目录属性
            rootBlock[6] = 2; // 起始块号
            rootBlock[7] = 0; // 长度
            Disk.writeBlock(2, rootBlock);
        }

        Contain.path.clear();
        Contain.path.add(root);

        // 加载目录内容
        HashMap<FileMsg, JButton> map = FileManage.dir(Contain.path, Contain.map);
        for (Map.Entry<FileMsg, JButton> ent : map.entrySet()) {
            Contain.rootPanel.add(ent.getValue());
        }
    }
}
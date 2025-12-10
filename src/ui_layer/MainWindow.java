package ui_layer;

import sys_core.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class MainWindow extends JFrame {

    private JPanel fileGridPanel;
    private JTable stateTable;
    private JButton btnBack;
    private JButton btnCreate;
    private JButton btnDebug;

    public MainWindow() {
        initUI();
        initSystem();
    }

    private void initUI() {
        setTitle("OS File System Simulator v2.0");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnBack = new JButton("返回上一级");
        btnCreate = new JButton("新建文件/目录");
        btnDebug = new JButton("检查磁盘状态");
        toolbar.add(btnBack); toolbar.add(btnCreate); toolbar.add(btnDebug);
        add(toolbar, BorderLayout.NORTH);

        // 文件展示区
        fileGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        fileGridPanel.setBackground(Color.WHITE);

        // 增加滚动面板，防止文件多了显示不全
        JScrollPane scrollPane = new JScrollPane(fileGridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // 去掉边框更好看
        add(scrollPane, BorderLayout.CENTER);

        AppSession.setRootPanel(fileGridPanel);

        // 底部状态栏
        stateTable = new JTable(FileSystemKernel.getOpenTable().getUiTableModel());
        JScrollPane tableScroll = new JScrollPane(stateTable);
        tableScroll.setPreferredSize(new Dimension(900, 150));
        add(tableScroll, BorderLayout.SOUTH);

        bindEvents();
    }

    private void initSystem() {
        VirtualDisk.init();
        FCB root = new FCB("root".getBytes(), "  ".getBytes(), (byte)8, (byte)2, (byte)0);
        AppSession.getCurrentPath().add(root);
        refreshView();
    }

    private void bindEvents() {
        btnBack.addActionListener(e -> {
            ArrayList<FCB> path = AppSession.getCurrentPath();
            if (path.size() > 1) {
                path.remove(path.size() - 1);
                refreshView();
            } else JOptionPane.showMessageDialog(this, "已经是根目录了！");
        });

        btnCreate.addActionListener(e -> new FileOperationDialog(this).setVisible(true));

        btnDebug.addActionListener(e -> {
            VirtualDisk.debugPrint();
            JOptionPane.showMessageDialog(this, "磁盘状态已打印到控制台");
        });
    }

    public void refreshView() {
        fileGridPanel.removeAll();
        AppSession.getIconMap().clear();
        int currentBlock = FileSystemKernel.getTargetBlockIndex(AppSession.getCurrentPath());
        byte[] data = VirtualDisk.readBlock(currentBlock);

        for (int i = 0; i < 8; i++) {
            FCB file = new FCB(data, i);
            String name = file.getFullName();

            // 跳过空文件 ($)
            if (name.startsWith("$")) continue;

            JButton btn = createIcon(file);
            fileGridPanel.add(btn);
            AppSession.getIconMap().put(file, btn);
        }
        fileGridPanel.revalidate();
        fileGridPanel.repaint();
    }

    // === 【核心修复】图标生成逻辑 ===
    private JButton createIcon(FCB file) {
        // 1. 创建按钮并设置文字
        JButton btn = new JButton(file.getFullName());

        // 2. 关键布局设置：文字在下，图标居中
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);

        // 3. 稍微调大按钮尺寸，防止文字被切掉
        btn.setPreferredSize(new Dimension(110, 100));
        btn.setMargin(new Insets(5, 5, 5, 5)); // 设置内部边距
        btn.setFocusPainted(false); // 去掉点击时的虚线框
        btn.setForeground(Color.BLACK); // 强制文字黑色

        // 4. 加载并缩放图标
        boolean iconLoaded = false;
        try {
            String iconPath = (file.getAttribute() == 8) ? "/image/catalogue.png" : "/image/file.png";
            URL imgUrl = getClass().getResource(iconPath);
            if (imgUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imgUrl);
                // 【重点】图片缩放！限制为 48x48 大小，给文字留空间
                Image img = originalIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(img));
                iconLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("Load icon error: " + e.getMessage());
        }

        // 5. 样式处理
        if (iconLoaded) {
            // 有图：背景设为透明，看起来更像操作系统
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false); // 去掉边框，只有鼠标放上去才有效果(视LookFeel而定)
            btn.setOpaque(false);
        } else {
            // 没图：使用颜色块兜底
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
            if (file.getAttribute() == 8) {
                btn.setBackground(new Color(255, 228, 181)); // 目录黄
                btn.setText("<html><center>[DIR]<br>" + file.getFullName() + "</center></html>"); // HTML换行确保显示
            } else {
                btn.setBackground(new Color(240, 248, 255)); // 文件蓝
                btn.setText("<html><center>[FILE]<br>" + file.getFullName() + "</center></html>");
            }
        }

        // 绑定右键菜单
        FileOperationDialog.attachRightMenu(btn, file, this);
        return btn;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File f = new File("vdisk_storage.dat");
            if (f.exists()) f.delete();
        }));

        // 设置系统风格，让按钮更好看
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
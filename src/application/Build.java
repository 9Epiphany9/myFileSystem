package application;

import java.awt.*;
import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import kernel.Disk;
import kernel.FileManage;
import kernel.FileMsg;
import uiTip.UiTip;

public class Build extends JFrame {
    private JTextField textName;
    private JTextField textTypeName;
    private JRadioButton dir;
    private JRadioButton file;
    private JRadioButton onlyReadFile;
    private JButton btnSubmit;
    public JButton btn;
    public JFrame readFrame;
    public JFrame writeFrame;
    public FileMsg msg;
    public ArrayList<FileMsg> filePath;

    public Build() {
        setTitle("新建");
        setSize(300, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        textName = new JTextField(10);
        textTypeName = new JTextField(10);
        dir = new JRadioButton("目录", true);
        file = new JRadioButton("普通文件");
        onlyReadFile = new JRadioButton("只读文件");
        btnSubmit = new JButton("提交");

        ButtonGroup group = new ButtonGroup();
        group.add(dir);
        group.add(file);
        group.add(onlyReadFile);

        panel.add(new JLabel("名称:"));
        panel.add(textName);
        panel.add(new JLabel("类型:"));
        panel.add(textTypeName);
        panel.add(dir);
        panel.add(file);
        panel.add(onlyReadFile);
        panel.add(new JLabel()); // 空标签占位
        panel.add(btnSubmit);

        // 限制输入长度
        textName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (textName.getText().length() >= 3) {
                    e.consume();
                }
            }
        });

        textTypeName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (textTypeName.getText().length() >= 2) {
                    e.consume();
                }
            }
        });

        // 提交按钮事件
        btnSubmit.addActionListener(e -> {
            String textNameStr = textName.getText().trim();
            String textTypeNameStr = textTypeName.getText().trim();

            if(textNameStr.contains("$") || textNameStr.contains(".") || textNameStr.contains("/")) {
                //UiTip.tip("文件名不能包含"$"、"."、"/"字符。");
                return;
            }

            int attribute = 0;
            if (dir.isSelected()) {
                attribute = 8;
            } else if (file.isSelected()) {
                attribute = 4;
            } else if (onlyReadFile.isSelected()) {
                attribute = 3;
            }

            newFileAndFolder(textNameStr, textTypeNameStr, attribute);
            dispose();
        });

        add(panel);
    }

    public void newFileAndFolder(String textNameStr, String textTypeNameStr, int attribute) {
        if (textNameStr.length() == 0) {
            textNameStr = "aut";
        }
        if (textTypeNameStr.length() == 0 && attribute != 8) {
            textTypeNameStr = "au";
        }

        if (attribute == 8) {
            if ((msg = FileManage.md(textNameStr, Contain.path)) != null) {
                try {
                    filePath = FileManage.clonePath(Contain.path);
                    filePath.add(msg);

                    // 创建目录按钮
                    btn = createButton(msg.getAllName(), "/image/catalogue.png");
                    setRightMenuFolder(btn);

                    Contain.map.put(msg, btn);
                    Contain.rootPanel.add(btn);
                    Contain.rootPanel.revalidate();
                    Contain.rootPanel.repaint();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    UiTip.tip("创建目录失败：克隆路径错误。");
                }
            }
        } else {
            if ((msg = FileManage.create_file(textNameStr, textTypeNameStr, (byte) attribute, Contain.path)) != null) {
                try {
                    filePath = FileManage.clonePath(Contain.path);
                    filePath.add(msg);

                    // 创建文件按钮
                    btn = createButton(msg.getAllName(), "/image/file.png");
                    setRightMenuFile(btn);

                    Contain.map.put(msg, btn);
                    Contain.rootPanel.add(btn);
                    Contain.rootPanel.revalidate();
                    Contain.rootPanel.repaint();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    UiTip.tip("创建文件失败：克隆路径错误。");
                }
            }
        }
    }

    private JButton createButton(String text, String iconPath) {
        JButton button = new JButton(text);
        ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
        Image scaledImage = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledImage));
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setFocusPainted(false);
        return button;
    }

    private void setRightMenuFolder(JButton btn) {
        JPopupMenu rightMenu = new JPopupMenu();

        JMenuItem displayFolderItem = new JMenuItem("显示目录内容");
        displayFolderItem.addActionListener(e -> {
            Contain.path.add(msg);
            refreshDirectory();
        });

        JMenuItem removeFolderItem = new JMenuItem("删除空目录");
        removeFolderItem.addActionListener(e -> {
            try {
                if (FileManage.rd(filePath) != -1) {
                    Contain.map.remove(msg);
                    Contain.rootPanel.remove(btn);
                    refreshDirectory();
                }
            } catch (CloneNotSupportedException ex) {
                ex.printStackTrace();
                UiTip.tip("删除目录失败：克隆路径错误。");
            }
        });

        rightMenu.add(displayFolderItem);
        rightMenu.add(removeFolderItem);

        // 确保右键菜单能正确触发
        setupRightClickMenu(btn, rightMenu);
    }

    private void setRightMenuFile(JButton btn) {
        JPopupMenu rightMenu = new JPopupMenu();

        JMenuItem readFileItem = new JMenuItem("读文件");
        readFileItem.addActionListener(e -> {
            String text = null;
            try {
                int listNum = FileManage.getListNum(filePath.get(filePath.size()-1).getStartBlock(),
                        Disk.readBlock(filePath.get(filePath.size()-2).getStartBlock()));
                text = FileManage.read_file(filePath,
                        Disk.readOnlyByte(filePath.get(filePath.size()-2).getStartBlock(), listNum, 7) * 64);
            } catch (UnsupportedEncodingException | CloneNotSupportedException ex) {
                ex.printStackTrace();
                UiTip.tip("读取文件失败：" + ex.getMessage());
            }

            if (text != null) {
                readFrame = new JFrame("读取文件");
                readFrame.setSize(800, 700);
                readFrame.setLocationRelativeTo(null);

                JTextArea textArea = new JTextArea(text);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                readFrame.add(new JScrollPane(textArea));
                readFrame.setVisible(true);
            }
        });

        JMenuItem writeFileItem = new JMenuItem("写文件");
        writeFileItem.addActionListener(e -> {
            writeFrame = new JFrame("写入文件");
            writeFrame.setSize(800, 700);
            writeFrame.setLocationRelativeTo(null);

            JTextArea textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JButton writeBtn = new JButton("写入");
            writeBtn.addActionListener(ev -> {
                String text = textArea.getText();
                try {
                    FileManage.write_file(filePath, text.getBytes("utf8"), text.getBytes().length);
                    UiTip.tip("文件写入成功！");
                    writeFrame.dispose();
                } catch (UnsupportedEncodingException | CloneNotSupportedException ex) {
                    ex.printStackTrace();
                    UiTip.tip("写入文件失败：" + ex.getMessage());
                }
            });

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            panel.add(writeBtn, BorderLayout.SOUTH);

            writeFrame.add(panel);
            writeFrame.setVisible(true);
        });

        JMenuItem closeFileItem = new JMenuItem("关闭文件");
        closeFileItem.addActionListener(e -> {
            FileManage.close_file(filePath);
            if (readFrame != null) {
                readFrame.dispose();
            }
            if (writeFrame != null) {
                writeFrame.dispose();
            }
            UiTip.tip("文件已关闭");
        });

        JMenuItem deleteFileItem = new JMenuItem("删除文件");
        deleteFileItem.addActionListener(e -> {
            try {
                if (FileManage.delete_file(filePath) != -1) {
                    Contain.map.remove(msg);
                    Contain.rootPanel.remove(btn);
                    refreshDirectory();
                    UiTip.tip("文件已删除");
                }
            } catch (CloneNotSupportedException ex) {
                ex.printStackTrace();
                UiTip.tip("删除文件失败：克隆路径错误。");
            }
        });

        JMenuItem typeFileItem = new JMenuItem("显示文件内容");
        typeFileItem.addActionListener(e -> {
            String text = null;
            try {
                text = FileManage.typeFile(filePath);
            } catch (CloneNotSupportedException ex) {
                ex.printStackTrace();
                UiTip.tip("显示文件内容失败：" + ex.getMessage());
            }

            if (text != null) {
                readFrame = new JFrame("文件内容");
                readFrame.setSize(800, 700);
                readFrame.setLocationRelativeTo(null);

                JTextArea textArea = new JTextArea(text);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                readFrame.add(new JScrollPane(textArea));
                readFrame.setVisible(true);
            }
        });

        JMenuItem changeFileItem = new JMenuItem("改变文件属性");
        changeFileItem.addActionListener(e -> {
            byte attribute = msg.getAttribute();
            if (attribute == 4) {
                attribute = 3;
            } else if (attribute == 3) {
                attribute = 4;
            }

            try {
                if (FileManage.change(filePath, attribute) != -1) {
                    msg.setAttribute(attribute);
                    UiTip.tip("已改变文件属性为：" + ((attribute == 4) ? "普通文件" : "系统只读文件"));
                }
            } catch (CloneNotSupportedException ex) {
                ex.printStackTrace();
                UiTip.tip("改变文件属性失败：" + ex.getMessage());
            }
        });

        rightMenu.add(readFileItem);
        rightMenu.add(writeFileItem);
        rightMenu.add(closeFileItem);
        rightMenu.add(deleteFileItem);
        rightMenu.add(typeFileItem);
        rightMenu.add(changeFileItem);

        // 确保右键菜单能正确触发
        setupRightClickMenu(btn, rightMenu);
    }

    private void refreshDirectory() {
        SwingUtilities.invokeLater(() -> {
            HashMap<FileMsg, JButton> updatedMap = FileManage.dir(Contain.path, new HashMap<>());
            Contain.map.clear();
            Contain.map.putAll(updatedMap);

            Contain.rootPanel.removeAll();
            for (Map.Entry<FileMsg, JButton> ent : Contain.map.entrySet()) {
                Contain.rootPanel.add(ent.getValue());
            }

            Contain.rootPanel.revalidate();
            Contain.rootPanel.repaint();
        });
    }

    // 确保右键菜单能正确触发的通用方法
    private void setupRightClickMenu(JButton btn, JPopupMenu menu) {
        // 方法1: 使用setComponentPopupMenu
        btn.setComponentPopupMenu(menu);

        // 方法2: 添加鼠标监听器作为备用
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
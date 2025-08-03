package application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kernel.FileManage;
import kernel.FileMsg;
import kernel.Disk;
import uiTip.UiTip;

public class BuildDialog extends JDialog {
    private JTextField nameField;
    private JTextField typeField;
    private JRadioButton dirRadio;
    private JRadioButton fileRadio;
    private JRadioButton readOnlyRadio;
    private JButton submitButton;
    private FileMsg msg;
    private ArrayList<FileMsg> filePath;

    public BuildDialog(JFrame parent) {
        super(parent, "新建", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(5, 2, 10, 10));
        setResizable(false);

        // 输入组件
        add(new JLabel("文件名:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("类型:"));
        typeField = new JTextField();
        add(typeField);

        add(new JLabel("属性:"));
        dirRadio = new JRadioButton("目录");
        fileRadio = new JRadioButton("普通文件", true);
        readOnlyRadio = new JRadioButton("只读文件");
        ButtonGroup group = new ButtonGroup();
        group.add(dirRadio);
        group.add(fileRadio);
        group.add(readOnlyRadio);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.add(dirRadio);
        radioPanel.add(fileRadio);
        radioPanel.add(readOnlyRadio);
        add(radioPanel);

        submitButton = new JButton("提交");
        add(new JLabel());
        add(submitButton);

        // 限制输入长度和非法字符
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (nameField.getText().length() >= 3) {
                    e.consume();
                }
                if (String.valueOf(e.getKeyChar()).matches("[\\$\\./]")) {
                    e.consume();
                    UiTip.tip("文件名不能包含“$”、“.”、“/”字符。");
                }
            }
        });
        typeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (typeField.getText().length() >= 2) {
                    e.consume();
                }
            }
        });

        // 提交按钮事件
        submitButton.addActionListener(e -> {
            String textNameStr = nameField.getText().trim();
            String textTypeNameStr = typeField.getText().trim();
            if (textNameStr.contains("$") || textNameStr.contains(".") || textNameStr.contains("/")) {
                UiTip.tip("文件名不能包含“$”、“.”、“/”字符。");
                return;
            }
            int attribute = dirRadio.isSelected() ? 8 : fileRadio.isSelected() ? 4 : 3;
            newFileAndFolder(textNameStr, textTypeNameStr, attribute);
            dispose();
        });
    }

    private void newFileAndFolder(String textNameStr, String textTypeNameStr, int attribute) {
        if (textNameStr.isEmpty()) {
            textNameStr = "aut";
        }
        if (textTypeNameStr.isEmpty()) {
            textTypeNameStr = "au";
        }
        JButton btn;
        if (attribute == 8) {
            msg = FileManage.md(textNameStr, Contain.getPath());
            if (msg != null) {
                try {
                    filePath = FileManage.clonePath(Contain.getPath());
                    filePath.add(msg);
                    btn = new JButton(msg.getAllName());
                    try {
                        btn.setIcon(new ImageIcon(getClass().getResource("/image/catalogue.png")));
                        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
                        btn.setHorizontalTextPosition(SwingConstants.CENTER);
                    } catch (Exception e) {
                        btn.setToolTipText("目录（图标加载失败）");
                    }
                    setRightMenuFolder(btn);
                    Contain.getRootPane().add(btn);
                    Contain.getMap().put(msg, btn);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            msg = FileManage.create_file(textNameStr, textTypeNameStr, (byte) attribute, Contain.getPath());
            if (msg != null) {
                try {
                    filePath = FileManage.clonePath(Contain.getPath());
                    filePath.add(msg);
                    btn = new JButton(msg.getAllName());
                    try {
                        btn.setIcon(new ImageIcon(getClass().getResource("/image/file.png")));
                        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
                        btn.setHorizontalTextPosition(SwingConstants.CENTER);
                    } catch (Exception e) {
                        btn.setToolTipText("文件（图标加载失败）");
                    }
                    setRightMenuFile(btn);
                    Contain.getRootPane().add(btn);
                    Contain.getMap().put(msg, btn);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        Contain.getRootPane().revalidate();
        Contain.getRootPane().repaint();
    }

    private void setRightMenuFolder(JButton btn) {
        JPopupMenu rightMenu = new JPopupMenu();
        JMenuItem displayFolderItem = new JMenuItem("显示目录内容");
        displayFolderItem.addActionListener(a -> {
            Contain.getPath().add(msg);
            updateParentFilePanel();
        });
        JMenuItem removeFolderItem = new JMenuItem("删除空目录");
        removeFolderItem.addActionListener(a -> {
            try {
                if (FileManage.rd(filePath) != -1) {
                    Contain.getRootPane().remove(btn);
                    Contain.getMap().remove(msg);
                    Contain.getRootPane().revalidate();
                    Contain.getRootPane().repaint();
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });
        rightMenu.add(displayFolderItem);
        rightMenu.add(removeFolderItem);
        btn.setComponentPopupMenu(rightMenu);
    }

    private void setRightMenuFile(JButton btn) {
        JPopupMenu rightMenu = new JPopupMenu();
        JDialog readDialog = null;
        JDialog writeDialog = null;

        JMenuItem readFileItem = new JMenuItem("读文件");
        readFileItem.addActionListener(a -> {
            try {
                int listNum = FileManage.getListNum(filePath.get(filePath.size() - 1).getStartBlock(),
                        Disk.readBlock(filePath.get(filePath.size() - 2).getStartBlock()));
                String text = FileManage.read_file(filePath,
                        Disk.readOnlyByte(filePath.get(filePath.size() - 2).getStartBlock(), listNum, 7) * 64);
                if (text != null) {
                    JDialog readDialog1 = new JDialog(this, "读取文件内容", true);
                    readDialog1.setSize(800, 700);
                    readDialog1.setLocationRelativeTo(this);
                    readDialog1.add(new JScrollPane(new JLabel(text)));
                    readDialog1.setVisible(true);
                }
            } catch (UnsupportedEncodingException | CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        JMenuItem writeFileItem = new JMenuItem("写文件");
        writeFileItem.addActionListener(a -> {
            JDialog writeDialog1 = new JDialog(this, "写入文件", true);
            writeDialog1.setSize(400, 300);
            writeDialog1.setLocationRelativeTo(this);
            JTextArea textArea = new JTextArea();
            textArea.setLineWrap(true);
            JButton writeBtn = new JButton("写入");
            writeBtn.addActionListener(e -> {
                String text = textArea.getText();
                try {
                    FileManage.write_file(filePath, text.getBytes("utf8"), text.getBytes().length);
                } catch (CloneNotSupportedException | UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
            });
            writeDialog1.setLayout(new BorderLayout());
            writeDialog1.add(new JScrollPane(textArea), BorderLayout.CENTER);
            writeDialog1.add(writeBtn, BorderLayout.SOUTH);
            writeDialog1.setVisible(true);
        });

        JMenuItem closeFileItem = new JMenuItem("关闭文件");
        closeFileItem.addActionListener(a -> {
            FileManage.close_file(filePath);
            if (readDialog != null) {
                readDialog.dispose();
            }
            if (writeDialog != null) {
                writeDialog.dispose();
            }
        });

        JMenuItem deleteFileItem = new JMenuItem("删除文件");
        deleteFileItem.addActionListener(a -> {
            try {
                if (FileManage.delete_file(filePath) != -1) {
                    Contain.getRootPane().remove(btn);
                    Contain.getMap().remove(msg);
                    Contain.getRootPane().revalidate();
                    Contain.getRootPane().repaint();
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        JMenuItem typeFileItem = new JMenuItem("显示文件内容");
        typeFileItem.addActionListener(a -> {
            try {
                String text = FileManage.typeFile(filePath);
                if (text != null) {
                    JDialog readDialog1 = new JDialog(this, "文件信息", true);
                    readDialog1.setSize(800, 700);
                    readDialog1.setLocationRelativeTo(this);
                    readDialog1.add(new JScrollPane(new JLabel(text)));
                    readDialog1.setVisible(true);
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        JMenuItem changeFileItem = new JMenuItem("改变文件属性");
        changeFileItem.addActionListener(a -> {
            byte attribute = msg.getAttribute();
            byte newAttribute = (attribute == 4) ? (byte) 3 : (byte) 4;
            try {
                if (FileManage.change(filePath, newAttribute) != -1) {
                    msg.setAttribute(newAttribute);
                    UiTip.tip("已改变文件属性为：" + (newAttribute == 4 ? "普通文件" : "系统只读文件"));
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        rightMenu.add(readFileItem);
        rightMenu.add(writeFileItem);
        rightMenu.add(closeFileItem);
        rightMenu.add(deleteFileItem);
        rightMenu.add(typeFileItem);
        rightMenu.add(changeFileItem);
        btn.setComponentPopupMenu(rightMenu);
    }

    private void updateParentFilePanel() {
        JPanel parentPanel = Contain.getRootPane();
        parentPanel.removeAll();
        HashMap<FileMsg, JButton> map = FileManage.dir(Contain.getPath(), Contain.getMap());
        for (Map.Entry<FileMsg, JButton> ent : Contain.getMap().entrySet()) {
            ent.getValue().setVisible(false);
        }
        for (Map.Entry<FileMsg, JButton> ent : map.entrySet()) {
            JButton button = ent.getValue();
            button.setVisible(true);
            parentPanel.add(button);
        }
        parentPanel.revalidate();
        parentPanel.repaint();
    }
}
package ui_layer;

import sys_core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class FileOperationDialog extends JDialog {

    private JTextField txtName;
    private JTextField txtType;
    private JRadioButton rbDir, rbFile, rbReadOnly;
    private MainWindow parentFrame;

    public FileOperationDialog(MainWindow parent) {
        super(parent, "新建项", true);
        this.parentFrame = parent;
        initLayout();
    }

    private void initLayout() {
        setSize(300, 250);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 1));

        JPanel p1 = new JPanel();
        p1.add(new JLabel("名称:"));
        txtName = new JTextField(10);
        p1.add(txtName);
        add(p1);

        JPanel p2 = new JPanel();
        p2.add(new JLabel("类型(仅文件):"));
        txtType = new JTextField(5);
        p2.add(txtType);
        add(p2);

        JPanel p3 = new JPanel();
        rbDir = new JRadioButton("目录");
        rbFile = new JRadioButton("普通文件", true);
        rbReadOnly = new JRadioButton("只读文件");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbDir); bg.add(rbFile); bg.add(rbReadOnly);
        p3.add(rbDir); p3.add(rbFile); p3.add(rbReadOnly);
        add(p3);

        JButton btnOk = new JButton("确定创建");
        btnOk.addActionListener(e -> doCreate());
        add(btnOk);
    }

    private void doCreate() {
        String name = txtName.getText().trim();
        String type = txtType.getText().trim();

        // 自动默认名
        if (name.isEmpty()) name = "aut";
        if (type.isEmpty()) type = "au";

        if (name.contains("$") || name.contains("/") || name.contains(".")) {
            JOptionPane.showMessageDialog(this, "文件名非法！");
            return;
        }

        byte attr = 4;
        if (rbDir.isSelected()) attr = 8;
        if (rbReadOnly.isSelected()) attr = 3;
        if (attr == 8) type = "  ";

        FCB newFile;
        if (attr == 8) {
            newFile = FileSystemKernel.makeDirectory(name, AppSession.getCurrentPath());
        } else {
            newFile = FileSystemKernel.createFile(name, type, attr, AppSession.getCurrentPath());
        }

        if (newFile != null) {
            parentFrame.refreshView();
            dispose();
        }
    }

    public static void attachRightMenu(JButton btn, FCB file, MainWindow mainWin) {
        JPopupMenu menu = new JPopupMenu();

        if (file.getAttribute() == 8) {
            addItem(menu, "打开目录", e -> {
                AppSession.getCurrentPath().add(file);
                mainWin.refreshView();
            });
            addItem(menu, "删除目录", e -> {
                ArrayList<FCB> targetPath = clonePathWith(file);
                if (FileSystemKernel.deleteFile(targetPath)) {
                    mainWin.refreshView();
                }
            });
        } else {
            addItem(menu, "读取文件", e -> {
                ArrayList<FCB> targetPath = clonePathWith(file);
                // 传入 0 即可，内核会自动读到 '#'
                String content = FileSystemKernel.readFile(targetPath, 0);
                if (content != null) showContentDialog(mainWin, "文件内容: " + file.getFullName(), content);
            });

            // === 【关键修改】使用大输入框进行写入 ===
            addItem(menu, "写入文件", e -> {
                JTextArea textArea = new JTextArea(10, 30);
                textArea.setLineWrap(true);
                JScrollPane scrollPane = new JScrollPane(textArea);

                int result = JOptionPane.showConfirmDialog(mainWin, scrollPane,
                        "请输入要写入的内容 (追加模式)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    String input = textArea.getText();
                    if (input != null) {
                        ArrayList<FCB> targetPath = clonePathWith(file);
                        byte[] bytes = input.getBytes();
                        FileSystemKernel.writeFile(targetPath, bytes, bytes.length);
                        mainWin.refreshView();
                    }
                }
            });

            addItem(menu, "关闭文件", e -> {
                ArrayList<FCB> targetPath = clonePathWith(file);
                FileSystemKernel.closeFile(targetPath);
                JOptionPane.showMessageDialog(mainWin, "文件已关闭");
            });

            addItem(menu, "删除文件", e -> {
                ArrayList<FCB> targetPath = clonePathWith(file);
                if (FileSystemKernel.deleteFile(targetPath)) mainWin.refreshView();
            });

            menu.addSeparator();

            addItem(menu, "改变属性 (只读/读写)", e -> {
                ArrayList<FCB> targetPath = clonePathWith(file);
                if (FileSystemKernel.changeAttribute(targetPath)) {
                    mainWin.refreshView();
                    JOptionPane.showMessageDialog(mainWin, "属性已切换");
                }
            });

            addItem(menu, "属性详情", e -> {
                String type = (file.getAttribute() == 3) ? "只读" : "普通";
                JOptionPane.showMessageDialog(mainWin, "类型: " + type + "\n大小: " + file.getLength() + "块");
            });
        }
        btn.setComponentPopupMenu(menu);
    }

    private static void addItem(JPopupMenu menu, String title, ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(action);
        menu.add(item);
    }

    private static ArrayList<FCB> clonePathWith(FCB child) {
        ArrayList<FCB> path = new ArrayList<>(AppSession.getCurrentPath());
        path.add(child);
        return path;
    }

    // 显示文件内容的弹窗
    private static void showContentDialog(Component parent, String title, String content) {
        JTextArea area = new JTextArea(content);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14)); // 设置等宽字体，看起来更像文件
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, 400)); // 更大的显示窗口
        JOptionPane.showMessageDialog(parent, scroll, title, JOptionPane.PLAIN_MESSAGE);
    }
}
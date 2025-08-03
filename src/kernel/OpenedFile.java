package kernel;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Vector;

import uiTip.UiTip;

public class OpenedFile {
    private ArrayList<Oftle> fileList;
    private DefaultTableModel openedFilesTable; // 用于 Swing 表格的数据模型

    public OpenedFile() {
        fileList = new ArrayList<>(5);
        // 初始化 Swing 表格模型
        openedFilesTable = new DefaultTableModel(new Object[]{"文件名", "操作类型"}, 0);
    }

    public int searchOpenedFile(int index) {
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).getNumber() == index) {
                return i;
            }
        }
        return -1;
    }

    public void addOftle(String name, byte attribute, int number, int length, int flag) {
        if (fileList.size() >= 5) {
            UiTip.tip("文件列表已满，打开文件失败");
            return;
        }
        Oftle oftle = new Oftle(name, attribute, number, length, flag);
        fileList.add(oftle);
        // 添加到 Swing 表格模型
        openedFilesTable.addRow(new Object[]{name, oftle.getFlagDisplay()});
    }

    public void subOftle(int n) {
        if (n >= 0 && n < fileList.size()) {
            fileList.remove(n);
            openedFilesTable.removeRow(n);
        }
    }

    public void changePointer(int n, int dnum, int bnum) {
        if (n >= 0 && n < fileList.size()) {
            Oftle oftle = fileList.get(n);
            if (oftle.getFlag() == 0) {
                oftle.setRead(new Pointer(dnum, bnum));
            } else {
                oftle.setWrite(new Pointer(dnum, bnum));
            }
        }
    }

    public Oftle getOftle(int n) {
        return n >= 0 && n < fileList.size() ? fileList.get(n) : null;
    }

    public int getLength() {
        return fileList.size();
    }

    public ArrayList<Oftle> getFileList() {
        return fileList;
    }

    public void setFileList(ArrayList<Oftle> fileList) {
        this.fileList = fileList;
        // 更新表格模型
        openedFilesTable.setRowCount(0);
        for (Oftle oftle : fileList) {
            openedFilesTable.addRow(new Object[]{oftle.getName(), oftle.getFlagDisplay()});
        }
    }

    public DefaultTableModel getOpenedFilesTable() {
        return openedFilesTable;
    }

    public void setOpenedFilesTable(DefaultTableModel openedFilesTable) {
        this.openedFilesTable = openedFilesTable;
    }
}
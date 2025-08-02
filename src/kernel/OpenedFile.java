package kernel;

import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import uiTip.UiTip;

public class OpenedFile {
    private ArrayList<Oftle> fileList;
    private DefaultTableModel tableModel; // 用于 JTable 显示

    public OpenedFile() {
        fileList = new ArrayList<>(5);
        String[] columnNames = {"名称", "标志"};
        tableModel = new DefaultTableModel(columnNames, 0);
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
        } else {
            Oftle oftle = new Oftle(name, attribute, number, length, flag);
            fileList.add(oftle);
            tableModel.addRow(new Object[]{oftle.getName(), oftle.getFlagString()});
        }
    }

    public void subOftle(int n) {
        fileList.remove(n);
        tableModel.removeRow(n);
    }

    public void changePointer(int n, int dnum, int bnum) {
        if (fileList.get(n).getFlag() == 0) {
            fileList.get(n).setRead(new Pointer(dnum, bnum));
        } else {
            fileList.get(n).setWrite(new Pointer(dnum, bnum));
        }
    }

    public Oftle getOftle(int n) {
        return fileList.get(n);
    }

    public int getLength() {
        return fileList.size();
    }

    public ArrayList<Oftle> getFileList() {
        return fileList;
    }

    public void setFileList(ArrayList<Oftle> fileList) {
        this.fileList = fileList;
        // 更新 tableModel
        tableModel.setRowCount(0);
        for (Oftle oftle : fileList) {
            tableModel.addRow(new Object[]{oftle.getName(), oftle.getFlagString()});
        }
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }
}
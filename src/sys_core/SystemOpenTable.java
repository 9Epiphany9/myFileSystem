package sys_core;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

/**
 * 系统打开文件表
 */
public class SystemOpenTable {

    public static final int MAX_OPEN_FILES = 5;
    private ArrayList<FileHandle> handleList;
    private DefaultTableModel uiTableModel;

    public SystemOpenTable() {
        this.handleList = new ArrayList<>(MAX_OPEN_FILES);
        this.uiTableModel = new DefaultTableModel(new Object[]{"文件路径", "当前状态"}, 0);
    }

    public int findHandleIndex(int startBlockIndex) {
        for (int i = 0; i < handleList.size(); i++) {
            if (handleList.get(i).getStartBlock() == startBlockIndex) {
                return i;
            }
        }
        return -1;
    }

    public boolean register(String path, byte attr, int startBlock, int size, int mode) {
        if (handleList.size() >= MAX_OPEN_FILES) return false;

        FileHandle handle = new FileHandle(path, attr, startBlock, size, mode);
        handleList.add(handle);
        uiTableModel.addRow(new Object[]{ path, handle.getModeString() });
        return true;
    }

    public void release(int index) {
        if (index >= 0 && index < handleList.size()) {
            handleList.remove(index);
            uiTableModel.removeRow(index);
        }
    }

    public void updateCursor(int index, int blockIndex, int byteOffset) {
        if (index >= 0 && index < handleList.size()) {
            FileHandle handle = handleList.get(index);
            FileHandle.Cursor newCursor = new FileHandle.Cursor(blockIndex, byteOffset);
            if (handle.getMode() == 0) handle.setReadCursor(newCursor);
            else handle.setWriteCursor(newCursor);
        }
    }

    public FileHandle getHandle(int index) {
        return (index >= 0 && index < handleList.size()) ? handleList.get(index) : null;
    }
    public DefaultTableModel getUiTableModel() { return uiTableModel; }
}
package sys_core;

import javax.swing.JOptionPane;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JButton;

/**
 * 文件系统内核 (修复版)
 * 修复了 FAT 链表断裂导致无法写入的问题，以及读取时的空指针异常。
 */
public class FileSystemKernel {

    private static SystemOpenTable openFileTable = new SystemOpenTable();

    public static SystemOpenTable getOpenTable() { return openFileTable; }

    // --- 空间管理 (保持不变) ---
    public static int allocateFreeBlock() {
        for (int fatBlock = 0; fatBlock < 2; fatBlock++) {
            byte[] buffer = VirtualDisk.readBlock(fatBlock);
            for (int offset = 0; offset < VirtualDisk.BLOCK_SIZE; offset++) {
                if (fatBlock == 0 && offset < 3) continue;
                if (buffer[offset] == 0) return fatBlock * 64 + offset;
            }
        }
        return -1;
    }

    public static int findDirectoryItem(String targetName, int parentBlockIndex) {
        byte[] dirData = VirtualDisk.readBlock(parentBlockIndex);
        for (int i = 0; i < 8; i++) {
            FCB entry = new FCB(dirData, i);
            String entryName = entry.getFullName();
            if (entryName.startsWith("$")) continue;
            if (entryName.equals(targetName)) return i;
        }
        return -1;
    }

    public static int getDirectoryUsage(int blockNum) {
        byte[] buffer = VirtualDisk.readBlock(blockNum);
        for (int i = 0; i < 8; i++) {
            if (buffer[i * 8] == '$') return i;
        }
        return 8;
    }

    public static int getTargetBlockIndex(ArrayList<FCB> path) {
        if (path == null || path.isEmpty()) return -1;
        return path.get(path.size() - 1).getStartBlock();
    }

    // --- 创建功能 (保持不变) ---
    public static FCB makeDirectory(String name, ArrayList<FCB> currentPath) {
        return createFileInternal(name, "  ", (byte)8, currentPath);
    }

    public static FCB createFile(String name, String type, byte attr, ArrayList<FCB> currentPath) {
        return createFileInternal(name, type, attr, currentPath);
    }

    private static FCB createFileInternal(String name, String type, byte attr, ArrayList<FCB> currentPath) {
        int parentBlock = getTargetBlockIndex(currentPath);
        if (parentBlock == -1) { showError("路径错误"); return null; }
        if (findDirectoryItem(name + (attr==8?"":"."+type), parentBlock) != -1) { showError("命名重复"); return null; }

        int newBlockIndex = allocateFreeBlock();
        if (newBlockIndex == -1) { showError("磁盘已满"); return null; }

        int itemIndex = getDirectoryUsage(parentBlock);
        if (itemIndex >= 8) { showError("目录已满"); return null; }

        addDirectoryEntry(parentBlock, itemIndex, name, type, attr, (byte) newBlockIndex, (byte) (attr==8?0:1));
        updateFAT(newBlockIndex, 255);

        if (attr == 8) {
            for (int k = 0; k < 8; k++) VirtualDisk.setByte(newBlockIndex, k, 0, (byte) '$');
        } else {
            VirtualDisk.setByte(newBlockIndex, 0, 0, (byte) '#');
        }

        return new FCB(VirtualDisk.readBlock(parentBlock), itemIndex);
    }

    // --- 关闭文件 ---
    public static void closeFile(ArrayList<FCB> path) {
        int targetBlock = getTargetBlockIndex(path);
        int index = openFileTable.findHandleIndex(targetBlock);
        if (index != -1) openFileTable.release(index);
    }

    // --- 修改属性---
    public static boolean changeAttribute(ArrayList<FCB> path) {
        int targetBlock = getTargetBlockIndex(path);
        if (openFileTable.findHandleIndex(targetBlock) != -1) {
            showError("文件正在使用中，无法修改属性！");
            return false;
        }
        FCB targetFile = path.get(path.size() - 1);
        byte newAttr = (targetFile.getAttribute() == 4) ? (byte) 3 : (byte) 4;

        ArrayList<FCB> parentPath = getParentPath(path);
        int parentBlock = getTargetBlockIndex(parentPath);
        int dirIndex = findDirectoryItem(targetFile.getFullName(), parentBlock);

        if (dirIndex != -1) {
            VirtualDisk.setByte(parentBlock, dirIndex, 5, newAttr);
            return true;
        }
        return false;
    }

    // === 写入文件  ===
    public static void writeFile(ArrayList<FCB> path, byte[] content, int dataLength) {
        // 1. 打开文件
        int handleIndex = openFile(path, 1);
        if (handleIndex < 0) return;

        FileHandle handle = openFileTable.getHandle(handleIndex);
        int startBlock = handle.getStartBlock();

        // 2. 找到文件的最后一个盘块 (追加模式)
        // 我们需要遍历 FAT 表直到找到值为 255 的块
        int lastBlock = startBlock;
        while (true) {
            int next = getNextBlockFromFAT(lastBlock);
            if (next == 255) break; // 找到末尾
            lastBlock = next;
        }

        // 3. 读取最后一个块的内容，寻找 '#' 结束符位置
        byte[] lastBlockData = VirtualDisk.readBlock(lastBlock);
        int appendOffset = 0;
        for (int i = 0; i < VirtualDisk.BLOCK_SIZE; i++) {
            if (lastBlockData[i] == '#') {
                appendOffset = i;
                break;
            }
        }

        // 4. 开始写入 loop
        int written = 0;
        int currentBlock = lastBlock;
        byte[] buffer = lastBlockData; // 从最后一块开始写
        int bufferOffset = appendOffset;

        while (written < dataLength) {
            // 如果当前块满了，需要申请新块
            if (bufferOffset >= VirtualDisk.BLOCK_SIZE) {
                // A. 保存旧块
                VirtualDisk.writeBlock(currentBlock, buffer);

                // B. 申请新块
                int newBlock = allocateFreeBlock();
                if (newBlock == -1) {
                    showError("磁盘空间不足，部分数据可能未写入");
                    break;
                }

                // C. 链接 FAT：旧块 -> 新块
                updateFAT(currentBlock, newBlock);
                updateFAT(newBlock, 255); // 新块是结尾

                // D. 移动指针
                currentBlock = newBlock;
                buffer = new byte[VirtualDisk.BLOCK_SIZE]; // 新的空缓存
                bufferOffset = 0;
            }

            // 写入字节
            buffer[bufferOffset] = content[written];
            bufferOffset++;
            written++;
        }

        // 5. 写入新的结束符 '#'
        if (bufferOffset < VirtualDisk.BLOCK_SIZE) {
            buffer[bufferOffset] = '#';
        } else {
            // 极少数情况：刚好填满，申请一个新块写 '#'
            VirtualDisk.writeBlock(currentBlock, buffer);
            int finalBlock = allocateFreeBlock();
            if (finalBlock != -1) {
                updateFAT(currentBlock, finalBlock);
                updateFAT(finalBlock, 255);
                currentBlock = finalBlock;
                buffer = new byte[VirtualDisk.BLOCK_SIZE];
                buffer[0] = '#';
            }
        }
        // 保存最后一块
        VirtualDisk.writeBlock(currentBlock, buffer);

        // 6. 【关键】计算总占用块数并更新目录项
        // 如果不更新这个，下次读取时系统会以为文件长度没变
        int totalBlocks = countUsedBlocks(startBlock);
        updateFileLengthInDir(path, totalBlocks);

        JOptionPane.showMessageDialog(null, "写入成功！");
    }

    // === 读文件===
    public static String readFile(ArrayList<FCB> path, int lengthIgnored) {
        // 1. 打开文件
        int handleIndex = openFile(path, 0);
        if (handleIndex < 0) return null;

        FileHandle handle = openFileTable.getHandle(handleIndex);
        int currentBlock = handle.getStartBlock();

        java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();

        try {
            // 2. 遍历 FAT 链表读取
            while (currentBlock != 255 && currentBlock != -1) {
                byte[] blockData = VirtualDisk.readBlock(currentBlock);

                // 寻找 '#' 结束符
                for (int i = 0; i < VirtualDisk.BLOCK_SIZE; i++) {
                    if (blockData[i] == '#') {
                        // 读到结束符，直接返回
                        return outStream.toString("UTF-8");
                    }
                    outStream.write(blockData[i]);
                }

                // 找下一块
                currentBlock = getNextBlockFromFAT(currentBlock);
            }
            return outStream.toString("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "读取错误";
        }
    }

    // --- 删除文件 ---
    public static boolean deleteFile(ArrayList<FCB> path) {
        int targetBlock = getTargetBlockIndex(path);
        if (openFileTable.findHandleIndex(targetBlock) != -1) {
            showError("文件正在使用"); return false;
        }
        FCB target = path.get(path.size()-1);
        if (target.getAttribute() == 8) {
            if (getDirectoryUsage(targetBlock) > 0) {
                showError("目录非空，无法删除"); return false;
            }
        }
        int current = targetBlock;
        while (current != -1 && current != 255) {
            int next = getNextBlockFromFAT(current);
            updateFAT(current, 0);
            current = next;
        }
        ArrayList<FCB> parentPath = getParentPath(path);
        int parentBlock = getTargetBlockIndex(parentPath);
        int dirIndex = findDirectoryItem(target.getFullName(), parentBlock);
        if (dirIndex != -1) {
            removeDirectoryEntry(parentBlock, dirIndex);
            return true;
        }
        return false;
    }

    // --- Helpers ---
    public static int openFile(ArrayList<FCB> path, int mode) {
        int blockIndex = getTargetBlockIndex(path);
        int existingIndex = openFileTable.findHandleIndex(blockIndex);
        if (existingIndex != -1) {
            FileHandle h = openFileTable.getHandle(existingIndex);
            if (h.getMode() == mode) return existingIndex;
            showError("文件打开模式冲突 (请先关闭文件)"); return -1;
        }
        if (mode == 1 && path.get(path.size()-1).getAttribute() % 2 != 0) {
            showError("只读文件，禁止写入"); return -1;
        }
        FCB fileInfo = path.get(path.size()-1);
        if (openFileTable.register(getPathString(path), fileInfo.getAttribute(),
                blockIndex, fileInfo.getLength(), mode)) {
            return openFileTable.findHandleIndex(blockIndex);
        } else {
            showError("打开表已满"); return -1;
        }
    }

    private static void addDirectoryEntry(int blockNum, int index, String name, String ext, byte attr, byte startBlock, byte len) {
        byte[] buffer = VirtualDisk.readBlock(blockNum);
        int offset = index * 8;
        byte[] nameB = name.getBytes();
        for(int i=0; i<3; i++) buffer[offset+i] = (i < nameB.length) ? nameB[i] : 0;
        byte[] extB = ext.getBytes();
        for(int i=0; i<2; i++) buffer[offset+3+i] = (i < extB.length) ? extB[i] : 0;
        buffer[offset+5] = attr; buffer[offset+6] = startBlock; buffer[offset+7] = len;
        VirtualDisk.writeBlock(blockNum, buffer);
    }

    private static void removeDirectoryEntry(int blockNum, int index) {
        byte[] buffer = VirtualDisk.readBlock(blockNum);
        for (int i = index; i < 7; i++) {
            System.arraycopy(buffer, (i+1)*8, buffer, i*8, 8);
        }
        buffer[56] = (byte)'$';
        VirtualDisk.writeBlock(blockNum, buffer);
    }

    private static int getNextBlockFromFAT(int currentBlock) {
        int fatBlock = currentBlock / 64;
        int fatOffset = currentBlock % 64;
        return VirtualDisk.getByte(fatBlock, 0, fatOffset);
    }

    private static void updateFAT(int currentBlock, int nextVal) {
        int fatBlock = currentBlock / 64;
        int fatOffset = currentBlock % 64;
        VirtualDisk.setByte(fatBlock, 0, fatOffset, (byte)nextVal);
    }

    public static String getPathString(ArrayList<FCB> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getFullName());
            if (i < path.size() - 1) sb.append("/");
        }
        return sb.toString();
    }

    public static ArrayList<FCB> getParentPath(ArrayList<FCB> path) {
        if (path.size() <= 1) return path;
        ArrayList<FCB> parent = new ArrayList<>(path);
        parent.remove(parent.size() - 1);
        return parent;
    }

    private static int countFreeBlocks() {
        int count = 0;
        for(int i=0; i<2; i++) {
            byte[] b = VirtualDisk.readBlock(i);
            for(byte v : b) if(v == 0) count++;
        }
        return count - 3;
    }

    // 统计一个文件占用了多少块 (遍历FAT)
    private static int countUsedBlocks(int startBlock) {
        int count = 0;
        int curr = startBlock;
        while (curr != 255 && curr != -1) {
            count++;
            curr = getNextBlockFromFAT(curr);
            if (count > 128) break; // 防止死循环
        }
        return count;
    }

    private static void updateFileLengthInDir(ArrayList<FCB> path, int totalBlocks) {
        ArrayList<FCB> parent = getParentPath(path);
        int parentBlock = getTargetBlockIndex(parent);
        String name = path.get(path.size()-1).getFullName();
        int index = findDirectoryItem(name, parentBlock);
        if (index != -1) {
            // 更新 FCB 中的 length 字段 (第7个字节)
            VirtualDisk.setByte(parentBlock, index, 7, (byte)totalBlocks);
        }
    }

    private static void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "系统错误", JOptionPane.ERROR_MESSAGE);
    }
}
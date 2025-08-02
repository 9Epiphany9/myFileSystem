package kernel;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

import uiTip.UiTip;

public class FileManage {
    private static byte[] buffer1 = new byte[64];
    private static OpenedFile ofList = new OpenedFile();

    public FileManage() {
    }

    public static OpenedFile getOfList() {
        return ofList;
    }

    public static void setOfList(OpenedFile ofList) {
        FileManage.ofList = ofList;
    }

    public static int searchFreeBlock() {
        byte[] buffer = Disk.readBlock(0);
        for (int i = 3; i < 64; i++) {
            if (buffer[i] == 0) {
                return i;
            }
        }
        buffer = Disk.readBlock(1);
        for (int i = 0; i < 64; i++) {
            if (buffer[i] == 0) {
                return i + 64;
            }
        }
        return -1;
    }

    public static int searchName(String name, int blockNum) {
        buffer1 = Disk.readBlock(blockNum);
        String str;
        for (int i = 0; i < 8; i++) {
            str = FileMsg.getAllName2(Arrays.copyOfRange(buffer1, i * 8, i * 8 + 8));
            if (str.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static int getListLength(int blockNum) {
        byte[] buffer = Disk.readBlock(blockNum);
        for (int i = 0; i < 8; i++) {
            if (buffer[i * 8] == '$') {
                return i;
            }
        }
        return 8;
    }

    public static int getListNum(int blockNum, byte[] buffer) {
        for (int i = 0; i < 8 && buffer[i * 8] != '$'; i++) {
            if (buffer[i * 8 + 6] == blockNum) {
                return i;
            }
        }
        return -1;
    }

    public static void deletElement(int blockNum, int listNum) {
        buffer1 = Disk.readBlock(blockNum);
        if (listNum < 7 && listNum >= 0) {
            for (int i = listNum; i < 7 && buffer1[i * 8] != '$'; i++) {
                for (int j = 0; j < 8; j++) {
                    buffer1[i * 8 + j] = buffer1[i * 8 + j + 8];
                }
            }
            buffer1[56] = '$';
        } else if (listNum == 7) {
            buffer1[56] = '$';
        }
        Disk.writeBlock(blockNum, buffer1);
    }

    public static void addElement(int blockNum, int listLength, String name, String typeName, byte attribute,
                                  byte startBlock, byte length) {
        if (name.length() == 0) {
            name = "aut";
        } else if (name.length() == 1) {
            name = name + "\0\0";
        } else if (name.length() == 2) {
            name = name + "\0";
        }
        if (typeName != "  ") {
            if (typeName.length() == 0) {
                typeName = "au";
            } else if (typeName.length() == 1) {
                typeName = typeName + "\0";
            }
        }
        buffer1 = Disk.readBlock(blockNum);
        buffer1[listLength * 8 + 0] = (byte) (name.getBytes()[0]);
        buffer1[listLength * 8 + 1] = (byte) (name.getBytes()[1]);
        buffer1[listLength * 8 + 2] = (byte) (name.getBytes()[2]);
        buffer1[listLength * 8 + 3] = (byte) (typeName.getBytes()[0]);
        buffer1[listLength * 8 + 4] = (byte) (typeName.getBytes()[1]);
        buffer1[listLength * 8 + 5] = attribute;
        buffer1[listLength * 8 + 6] = startBlock;
        buffer1[listLength * 8 + 7] = length;
        Disk.writeBlock(blockNum, buffer1);
    }

    public static String getPathName(ArrayList<FileMsg> path) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < path.size() - 1; i++) {
            str.append(path.get(i).getAllName());
            str.append("/");
        }
        str.append(path.get(path.size() - 1).getAllName());
        return new String(str);
    }

    public static int getBlockIndex(ArrayList<FileMsg> path) {
        return path.get(path.size() - 1).getStartBlock();
    }

    public static FileMsg create_file(String name, String type, byte attribute, ArrayList<FileMsg> path) {
        int pathBlockIndex = getBlockIndex(path);
        int blockNum = searchFreeBlock();
        int listLength;
        if (pathBlockIndex == -1) {
            UiTip.tip("未找到路径所指向的文件夹，创建文件失败。");
            return null;
        }
        if (attribute % 2 == 1) {
            UiTip.tip("属性为只读的文件不能建立，创建文件失败。");
            return null;
        }
        if (searchName(name + "." + type, pathBlockIndex) != -1) {
            UiTip.tip("命名重复，创建文件失败。");
            return null;
        }
        if (blockNum == -1) {
            UiTip.tip("磁盘空间已满，创建文件失败。");
            return null;
        }
        if ((listLength = getListLength(pathBlockIndex)) >= 8) {
            UiTip.tip("一个文件夹最多创建8个文件/文件夹，创建文件失败。");
            return null;
        }
        addElement(pathBlockIndex, listLength, name, type, attribute, (byte) blockNum, (byte) 1);
        Disk.writeOnlyByte(0, 0, blockNum, (byte) 255);
        Disk.writeOnlyByte(blockNum, 0, 0, (byte) '#');
        return new FileMsg(Disk.readBlock(pathBlockIndex), listLength);
    }

    public static int open_file(ArrayList<FileMsg> path, int operateType) throws CloneNotSupportedException {
        int pathBlockIndex = getBlockIndex(path);
        int index;
        int indexOfList;
        if (pathBlockIndex == -1) {
            UiTip.tip("未找到路径所指向的文件，打开文件失败。");
            return -1;
        }
        if ((index = ofList.searchOpenedFile(pathBlockIndex)) != -1) {
            if (operateType == ofList.getOftle(index).getFlag()) {
                UiTip.tip("该文件已按" + ((operateType == 1) ? "写" : "读") + "方式打开。打开文件成功。");
                return index;
            } else {
                UiTip.tip("该文件已打开但打开方式不同，请先关闭文件再以该方式打开文件。打开文件失败。");
                return -1;
            }
        }
        if (ofList.getLength() >= 5) {
            UiTip.tip("当前打开文件列表已满。打开文件失败。");
            return -1;
        }
        ArrayList<FileMsg> parentPath = getParentPath(path);
        buffer1 = Disk.readBlock(getBlockIndex(parentPath));
        indexOfList = searchName(path.get(path.size() - 1).getAllName(), getBlockIndex(parentPath));
        if (indexOfList == -1) {
            UiTip.tip("找不到该文件在其目录中的索引，打开文件失败。");
            return -1;
        }
        if (Disk.readOnlyByte(getBlockIndex(parentPath), indexOfList, 5) % 2 == 1 && operateType == 1) {
            UiTip.tip("不能以写方式打开只读文件，打开文件失败。");
            return -1;
        }
        int pointer = indexOfList * 8;
        ofList.addOftle(getPathName(path), buffer1[pointer + 5], buffer1[pointer + 6],
                getFileLength(buffer1[pointer + 6]), operateType);
        if (operateType == 0) {
            ofList.changePointer(ofList.getLength() - 1, ofList.getOftle(ofList.getLength() - 1).getNumber(), 0);
        } else {
            int nextDisk = ofList.getOftle(ofList.getLength() - 1).getNumber();
            int endBnum = 63;
            int endDnum;
            int flag = 1;
            do {
                endDnum = nextDisk;
                buffer1 = Disk.readBlock(endDnum);
                nextDisk = buffer1[endDnum % 64];
                for (int i = 0; i < 64; i++) {
                    if (buffer1[i] == (byte) '#') {
                        endBnum = i;
                        flag = 0;
                        break;
                    }
                }
            } while (nextDisk != -1 && flag == 1);
            ofList.changePointer(ofList.getLength() - 1, endDnum, endBnum);
        }
        return ofList.getLength() - 1;
    }

    private static int getFileLength(int startBlock) {
        int count = 0;
        int nextBlock;
        byte[] buffer = new byte[64];
        while (startBlock != -1) {
            nextBlock = Disk.readOnlyByte(0, 0, startBlock);
            if (nextBlock == -1) {
                buffer = Disk.readBlock(startBlock);
                count = count + getJingHaoIndex(buffer);
                return count;
            } else {
                count = count + 64;
            }
            startBlock = nextBlock;
        }
        return count;
    }

    public static void close_file(ArrayList<FileMsg> path) {
        int pathBlockIndex = getBlockIndex(path);
        for (int i = 0; i < ofList.getLength(); i++) {
            if (ofList.getOftle(i).getNumber() == pathBlockIndex) {
                ofList.subOftle(i);
                break;
            }
        }
    }

    public static String read_file(ArrayList<FileMsg> path, int length)
            throws CloneNotSupportedException, UnsupportedEncodingException {
        int ofIndex;
        ArrayList<FileMsg> parentPath = getParentPath(path);
        int listNum = getListNum(path.get(path.size() - 1).getStartBlock(),
                Disk.readBlock(parentPath.get(parentPath.size() - 1).getStartBlock()));
        byte[] fileBuffer = new byte[Disk.readOnlyByte(parentPath.get(parentPath.size() - 1).getStartBlock(), listNum,
                7) * 64];
        int fileBufferIndex = 0;
        if ((ofIndex = open_file(path, 0)) < 0) {
            UiTip.tip("打开文件失败。读取失败。");
            return null;
        }
        int nextBlock;
        int lastBlockIndex = 0;
        while (ofList.getOftle(ofIndex).getRead().getDnum() != -1) {
            nextBlock = Disk.readOnlyByte(0, 0, ofList.getOftle(ofIndex).getRead().getDnum());
            buffer1 = Disk.readBlock(ofList.getOftle(ofIndex).getRead().getDnum());
            System.arraycopy(buffer1, 0, fileBuffer, fileBufferIndex, 64);
            fileBufferIndex = fileBufferIndex + 64;
            if (nextBlock == -1) {
                lastBlockIndex = getJingHaoIndex(buffer1);
            }
            ofList.changePointer(ofIndex, nextBlock, 0);
        }
        ofList.changePointer(ofIndex, path.get(path.size() - 1).getStartBlock(), 0);
        int min = (length < (fileBufferIndex - 64 + lastBlockIndex) ? length : (fileBufferIndex - 64 + lastBlockIndex));
        byte[] fileContext = Arrays.copyOf(fileBuffer, min);
        return new String(fileContext, "utf8");
    }

    public static int getJingHaoIndex(byte[] buffer) {
        int i = 0;
        for (i = 0; i < 64; i++) {
            if (buffer[i] == (byte) '#') {
                break;
            }
        }
        return i;
    }

    public static int getFreeBlocks() {
        int count = 0;
        byte[] buffer = Disk.readBlock(0);
        for (int i = 0; i < 64; i++) {
            if (buffer[i] == 0) {
                count++;
            }
        }
        buffer = Disk.readBlock(1);
        for (int i = 0; i < 64; i++) {
            if (buffer[i] == 0) {
                count++;
            }
        }
        return count;
    }

    public static int getNeedBlock(int ofIndex, int length) {
        int needBlocks;
        int blockNum = ofList.getOftle(ofIndex).getWrite().getDnum();
        byte[] buffer = Disk.readBlock(blockNum);
        int lastBlockIndex = getJingHaoIndex(buffer);
        int remain = 64 - lastBlockIndex - 1;
        if (remain > length) {
            return 0;
        } else {
            int i = length - remain;
            if (i % 64 == 0) {
                needBlocks = i / 64;
            } else {
                needBlocks = i / 64 + 1;
            }
        }
        return needBlocks;
    }

    public static void modifyFAT(int needBlocks, int blockNum) {
        int nextBlock;
        for (int i = 0; i <= needBlocks; i++) {
            nextBlock = searchFreeBlock();
            if (i != needBlocks) {
                Disk.writeOnlyByte(blockNum / 64, 0, blockNum % 64, (byte) nextBlock);
            } else {
                Disk.writeOnlyByte(blockNum / 64, 0, blockNum % 64, (byte) -1);
            }
            blockNum = nextBlock;
        }
    }

    public static ArrayList<FileMsg> getParentPath(ArrayList<FileMsg> path) throws CloneNotSupportedException {
        ArrayList<FileMsg> parentPath = new ArrayList<>();
        for (FileMsg msg : path) {
            parentPath.add((FileMsg) msg.clone());
        }
        parentPath.remove(path.size() - 1);
        return parentPath;
    }

    public static ArrayList<FileMsg> clonePath(ArrayList<FileMsg> path) throws CloneNotSupportedException {
        ArrayList<FileMsg> clonePath = new ArrayList<>();
        for (FileMsg msg : path) {
            clonePath.add((FileMsg) msg.clone());
        }
        return clonePath;
    }

    public static void write_file(ArrayList<FileMsg> path, byte[] content, int length) throws CloneNotSupportedException {
        int ofIndex;
        if ((ofIndex = open_file(path, 1)) < 0) {
            UiTip.tip("打开文件失败。写入失败。");
            return;
        }
        int blockNum = ofList.getOftle(ofIndex).getWrite().getDnum();
        buffer1 = Disk.readBlock(blockNum);
        int needBlocks = getNeedBlock(ofIndex, length);
        int freeBlocks = getFreeBlocks();
        if (needBlocks > freeBlocks) {
            UiTip.tip("磁盘容量不够。写入失败。");
            return;
        }
        modifyFAT(needBlocks, blockNum);
        for (int i = 0; i < length; i++) {
            if (ofList.getOftle(ofIndex).getWrite().getBnum() == 64) {
                Disk.writeBlock(ofList.getOftle(ofIndex).getWrite().getDnum(), buffer1);
                ofList.changePointer(ofIndex, Disk.readOnlyByte(ofList.getOftle(ofIndex).getWrite().getDnum() / 64, 0,
                        ofList.getOftle(ofIndex).getWrite().getDnum() % 64), 0);
                i--;
                clearArray(buffer1);
            } else {
                buffer1[ofList.getOftle(ofIndex).getWrite().getBnum()] = content[i];
                ofList.getOftle(ofIndex).setLength(ofList.getOftle(ofIndex).getLength() + 1);
                ofList.getOftle(ofIndex).getWrite().setBnum(ofList.getOftle(ofIndex).getWrite().getBnum() + 1);
            }
        }
        if (ofList.getOftle(ofIndex).getWrite().getBnum() == 64) {
            Disk.writeBlock(ofList.getOftle(ofIndex).getWrite().getDnum(), buffer1);
            ofList.changePointer(ofIndex, Disk.readOnlyByte(ofList.getOftle(ofIndex).getWrite().getDnum() / 64, 0,
                    ofList.getOftle(ofIndex).getWrite().getDnum() % 64), 0);
            Disk.writeOnlyByte(ofList.getOftle(ofIndex).getWrite().getDnum(), 0, 0, (byte) '#');
        } else {
            buffer1[ofList.getOftle(ofIndex).getWrite().getBnum()] = (byte) '#';
            Disk.writeBlock(ofList.getOftle(ofIndex).getWrite().getDnum(), buffer1);
        }
        ArrayList<FileMsg> parentPath = getParentPath(path);
        int listNum = getListNum(path.get(path.size() - 1).getStartBlock(),
                Disk.readBlock(parentPath.get(parentPath.size() - 1).getStartBlock()));
        Disk.writeOnlyByte(parentPath.get(parentPath.size() - 1).getStartBlock(), listNum, 7,
                (byte) (Disk.readOnlyByte(parentPath.get(parentPath.size() - 1).getStartBlock(), listNum, 7)
                        + needBlocks));
    }

    private static void clearArray(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 0;
        }
    }

    public static int delete_file(ArrayList<FileMsg> path) throws CloneNotSupportedException {
        int pathBlockIndex = getBlockIndex(path);
        if (pathBlockIndex == -1) {
            UiTip.tip("未找到路径所指向的文件，删除文件失败。");
            return -1;
        }
        if ((ofList.searchOpenedFile(pathBlockIndex)) != -1) {
            UiTip.tip("该文件已经打开，无法删除，删除文件失败。");
            return -1;
        }
        int blockNum;
        int nextNum = pathBlockIndex;
        while (nextNum > 0) {
            blockNum = nextNum;
            buffer1 = Disk.readBlock(blockNum / 64);
            nextNum = buffer1[blockNum % 64];
            Disk.freeBlock(blockNum);
        }
        ArrayList<FileMsg> parentPath = getParentPath(path);
        buffer1 = Disk.readBlock(getBlockIndex(parentPath));
        int listNum;
        if ((listNum = getListNum(getBlockIndex(path), buffer1)) == -1) {
            UiTip.tip("找不到该文件在其目录中的索引，删除文件失败。");
            return -1;
        }
        deletElement(getBlockIndex(parentPath), listNum);
        return 1;
    }

    public static String typeFile(ArrayList<FileMsg> path) throws CloneNotSupportedException {
        ArrayList<FileMsg> parentPath = getParentPath(path);
        buffer1 = Disk.readBlock(getBlockIndex(parentPath));
        int listNum = getListNum(getBlockIndex(path), buffer1);
        FileMsg msg = new FileMsg(buffer1, listNum);
        String attribute;
        if (msg.getAttribute() == 8) {
            attribute = "目录";
        } else if ((msg.getAttribute() % 2) == 1) {
            attribute = "系统只读文件";
        } else {
            attribute = "普通文件";
        }
        String s = "文件名：" + msg.getAllName() + "\n属性：" + attribute + "\n起始盘块号：" + msg.getStartBlock()
                + "\n文件长度(所用盘块号个数)：" + msg.getLength();
        return s;
    }

    public static int change(ArrayList<FileMsg> path, byte newAttribute) throws CloneNotSupportedException {
        if ((ofList.searchOpenedFile(getBlockIndex(path))) != -1) {
            UiTip.tip("该文件已经打开，无法改变文件属性，改变文件属性失败。");
            return -1;
        }
        ArrayList<FileMsg> parentPath = getParentPath(path);
        buffer1 = Disk.readBlock(getBlockIndex(parentPath));
        int listNum = getListNum(getBlockIndex(path), buffer1);
        Disk.writeOnlyByte(getBlockIndex(parentPath), listNum, 5, newAttribute);
        return 1;
    }

    public static FileMsg md(String name, ArrayList<FileMsg> path) {
        int pathBlockIndex = getBlockIndex(path);
        int blockNum;
        int listLength;
        if (pathBlockIndex == -1) {
            UiTip.tip("未找到路径所指向的文件夹，创建目录失败。");
            return null;
        }
        if (searchName(name, pathBlockIndex) != -1) {
            UiTip.tip("命名重复，创建目录失败。");
            return null;
        }
        if ((blockNum = searchFreeBlock()) == -1) {
            UiTip.tip("磁盘空间已满，创建目录失败。");
            return null;
        }
        if ((listLength = getListLength(pathBlockIndex)) >= 8) {
            UiTip.tip("一个文件夹最多创建8个文件/文件夹，创建目录失败。");
            return null;
        }
        addElement(pathBlockIndex, listLength, name, "  ", (byte) 8, (byte) blockNum, (byte) 0);
        Disk.writeOnlyByte(0, 0, blockNum, (byte) 255);
        for (int i = 0; i < 8; i++) {
            Disk.writeOnlyByte(blockNum, i, 0, (byte) '$');
        }
        return new FileMsg(Disk.readBlock(pathBlockIndex), listLength);
    }

    public static ArrayList<Integer> getList(int blockNum) {
        ArrayList<Integer> arr = new ArrayList<>();
        byte[] buffer = Disk.readBlock(blockNum);
        for (int i = 0; i < 8; i++) {
            if (buffer[i * 8] == '$') {
                break;
            }
            arr.add((int) buffer[i * 8 + 6] & 0xFF); // 转换为无符号整数
        }
        return arr;
    }

    private static JButton createButtonForFile(FileMsg msg) {
        JButton btn = new JButton(msg.getAllName());
        String iconPath = msg.getAttribute() == 8 ? "/image/catalogue.png" : "/image/file.png";
        ImageIcon icon = new ImageIcon(FileManage.class.getResource(iconPath));
        btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH)));
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        return btn;
    }

    public static HashMap<FileMsg, JButton> dir(ArrayList<FileMsg> path, HashMap<FileMsg, JButton> map) {
        int pathBlockIndex = getBlockIndex(path);
        if (pathBlockIndex == -1) {
            return new HashMap<>();
        }

        HashMap<FileMsg, JButton> child = new HashMap<>();
        byte[] buffer = Disk.readBlock(pathBlockIndex);

        for (int i = 0; i < 8; i++) {
            if (buffer[i * 8] == '$') {
                break; // 空条目
            }

            // 解析文件/目录信息
            FileMsg msg = new FileMsg(buffer, i);
            JButton btn = map.getOrDefault(msg, createButtonForFile(msg));
            child.put(msg, btn);
        }
        return child;
    }

    public static int rd(ArrayList<FileMsg> path) throws CloneNotSupportedException {
        int pathBlockIndex = getBlockIndex(path);
        if ((getListLength(pathBlockIndex)) > 0) {
            UiTip.tip("非空目录无法删除，删除失败。");
            return -1;
        }
        Disk.freeBlock(pathBlockIndex);
        ArrayList<FileMsg> parentPath = getParentPath(path);
        buffer1 = Disk.readBlock(getBlockIndex(parentPath));
        int listNum;
        if ((listNum = getListNum(getBlockIndex(path), buffer1)) == -1) {
            UiTip.tip("找不到该文件在其目录中的索引，删除文件失败。");
            return -1;
        }
        deletElement(getBlockIndex(parentPath), listNum);
        return 1;
    }
}
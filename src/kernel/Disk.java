package kernel;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

public class Disk {
    private static final String DISK_FILE = "data.txt";
    private static final int BLOCK_SIZE = 64;
    private static final int TOTAL_BLOCKS = 128;

    public static FileMsg loadRootDirectory() {
        byte[] rootBlock = readBlock(2); // 根目录在块2
        if (rootBlock[0] == '$') {
            return null; // 空目录
        }

        // 解析根目录信息并创建FileMsg对象
        byte[] name = new byte[]{rootBlock[0], rootBlock[1], rootBlock[2]};
        byte[] type = new byte[]{rootBlock[3], rootBlock[4]};
        byte attribute = rootBlock[5];
        byte startBlock = rootBlock[6];
        byte length = rootBlock[7];

        return new FileMsg(name, type, attribute, startBlock, length);
    }

    // 添加方法：检查磁盘是否已初始化
    public static boolean isInitialized() {
        File file = new File(DISK_FILE);
        return file.exists() && file.length() >= TOTAL_BLOCKS * BLOCK_SIZE;
    }
    public static void start() {
        File file = new File(DISK_FILE);
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            // 创建或调整 data 文件
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                if (!file.exists() || file.length() < TOTAL_BLOCKS * BLOCK_SIZE) {
                    raf.setLength(TOTAL_BLOCKS * BLOCK_SIZE); // 设置文件大小为 8192 字节
                    // 初始化 FAT 表（块 0 和 1）
                    byte[] fat = new byte[BLOCK_SIZE * 2];
                    fat[0] = (byte) 255; // 块 0 (FAT)
                    fat[1] = (byte) 255; // 块 1 (FAT)
                    fat[2] = (byte) 255; // 块 2 (根目录)
                    raf.seek(0);
                    raf.write(fat);
                    // 初始化根目录（块 2），用 '$' 表示空条目
                    byte[] rootBlock = new byte[BLOCK_SIZE];
                    for (int i = 0; i < 8; i++) {
                        rootBlock[i * 8] = (byte) '$';
                    }
                    raf.seek(2 * BLOCK_SIZE);
                    raf.write(rootBlock);
                }
            }
        } catch (IOException e) {
            System.err.println("警告：无法初始化磁盘文件：" + e.getMessage());
            // 尝试在当前目录创建文件
            try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "rw")) {
                raf.setLength(TOTAL_BLOCKS * BLOCK_SIZE);
                byte[] fat = new byte[BLOCK_SIZE * 2];
                fat[0] = (byte) 255;
                fat[1] = (byte) 255;
                fat[2] = (byte) 255;
                raf.seek(0);
                raf.write(fat);
                byte[] rootBlock = new byte[BLOCK_SIZE];
                for (int i = 0; i < 8; i++) {
                    rootBlock[i * 8] = (byte) '$';
                }
                raf.seek(2 * BLOCK_SIZE);
                raf.write(rootBlock);
            } catch (IOException ex) {
                throw new RuntimeException("严重错误：无法创建或初始化磁盘文件：" + ex.getMessage());
            }
        }
    }

    public static byte[] readBlock(int blockNum) {
        byte[] buffer = new byte[BLOCK_SIZE];
        try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "r")) {
            raf.seek(blockNum * BLOCK_SIZE);
            int bytesRead = raf.read(buffer);
            if (bytesRead < BLOCK_SIZE && bytesRead != -1) {
                for (int i = bytesRead; i < BLOCK_SIZE; i++) {
                    buffer[i] = 0;
                }
            }
        } catch (IOException e) {
            System.err.println("警告：无法读取块 " + blockNum + "：" + e.getMessage());
            for (int i = 0; i < BLOCK_SIZE; i++) {
                buffer[i] = 0;
            }
        }
        return buffer;
    }

    public static void writeBlock(int blockNum, byte[] buffer) {
        try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "rw")) {
            raf.seek(blockNum * BLOCK_SIZE);
            raf.write(buffer);
        } catch (IOException e) {
            System.err.println("警告：无法写入块 " + blockNum + "：" + e.getMessage());
        }
    }

    public static byte readOnlyByte(int blockNum, int listNum, int index) {
        try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "r")) {
            raf.seek(blockNum * BLOCK_SIZE + listNum * 8 + index);
            return raf.readByte();
        } catch (IOException e) {
            System.err.println("警告：无法读取块 " + blockNum + "，索引 " + (listNum * 8 + index) + "：" + e.getMessage());
            return 0;
        }
    }

    public static void writeOnlyByte(int blockNum, int listNum, int index, byte b) {
        try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "rw")) {
            raf.seek(blockNum * BLOCK_SIZE + listNum * 8 + index);
            raf.writeByte(b);
        } catch (IOException e) {
            System.err.println("警告：无法写入块 " + blockNum + "，索引 " + (listNum * 8 + index) + "：" + e.getMessage());
        }
    }

    public static void freeBlock(int blockNum) {
        try (RandomAccessFile raf = new RandomAccessFile(DISK_FILE, "rw")) {
            raf.seek(blockNum / BLOCK_SIZE * BLOCK_SIZE + blockNum % BLOCK_SIZE);
            raf.writeByte(0);
            raf.seek(blockNum * BLOCK_SIZE);
            byte[] emptyBlock = new byte[BLOCK_SIZE];
            raf.write(emptyBlock);
        } catch (IOException e) {
            System.err.println("警告：无法释放块 " + blockNum + "：" + e.getMessage());
        }
    }
}
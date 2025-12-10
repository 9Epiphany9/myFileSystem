package sys_core;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * 虚拟磁盘驱动器 (Virtual Disk Driver)
 * 负责底层字节流的物理读写，模拟真实硬盘的扇区操作。
 */
public class VirtualDisk {

    // --- 物理参数定义 ---
    public static final int TOTAL_BLOCKS = 128;
    public static final int BLOCK_SIZE = 64;

    // 模拟磁盘的存储文件路径
    private static final String STORAGE_FILE = "vdisk_storage.dat";

    /**
     * 初始化/格式化磁盘
     */
    public static void init() {
        // 1. 物理清除：将所有扇区填0
        byte[] emptySector = new byte[BLOCK_SIZE];
        Arrays.fill(emptySector, (byte) 0);

        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            writeBlock(i, emptySector);
        }

        // 2. 系统区初始化 (FAT表占用 Block 0 & 1)
        // 标记 FAT[0], FAT[1], FAT[2] 为已占用 (-1/255 表示占用/结束)
        setByte(0, 0, 0, (byte) 255);
        setByte(0, 0, 1, (byte) 255);
        setByte(0, 0, 2, (byte) 255);

        // 3. 根目录初始化 (Block 2)
        // 将根目录的8个目录项标记为空 ('$')
        for (int i = 0; i < 8; i++) {
            setByte(2, i, 0, (byte) '$');
        }

        System.out.println("系统: 虚拟磁盘格式化完成。");
    }

    /**
     * 核心操作：读取一个盘块
     */
    public static byte[] readBlock(int blockIndex) {
        byte[] data = new byte[BLOCK_SIZE];
        try (RandomAccessFile disk = new RandomAccessFile(STORAGE_FILE, "rw")) {
            long offset = (long) blockIndex * BLOCK_SIZE;
            disk.seek(offset);
            disk.read(data);
        } catch (IOException e) {
            System.err.println("错误: 读取盘块失败 -> " + blockIndex);
            e.printStackTrace();
        }
        return data;
    }

    /**
     * 核心操作：写入一个盘块
     */
    public static void writeBlock(int blockIndex, byte[] data) {
        try (RandomAccessFile disk = new RandomAccessFile(STORAGE_FILE, "rw")) {
            long offset = (long) blockIndex * BLOCK_SIZE;
            disk.seek(offset);
            int lengthToWrite = Math.min(data.length, BLOCK_SIZE);
            disk.write(data, 0, lengthToWrite);
        } catch (IOException e) {
            System.err.println("错误: 写入盘块失败 -> " + blockIndex);
            e.printStackTrace();
        }
    }

    /**
     * 辅助操作：修改特定位置的单个字节
     */
    public static void setByte(int blockIndex, int itemIndex, int byteOffset, byte value) {
        try (RandomAccessFile disk = new RandomAccessFile(STORAGE_FILE, "rw")) {
            long pos = (long) blockIndex * BLOCK_SIZE + (long) itemIndex * 8 + byteOffset;
            disk.seek(pos);
            disk.writeByte(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 辅助操作：读取特定位置的单个字节 (返回 int 避免负数问题)
     */
    public static int getByte(int blockIndex, int itemIndex, int byteOffset) {
        int result = 0;
        try (RandomAccessFile disk = new RandomAccessFile(STORAGE_FILE, "rw")) {
            long pos = (long) blockIndex * BLOCK_SIZE + (long) itemIndex * 8 + byteOffset;
            disk.seek(pos);
            result = disk.readByte() & 0xFF; // 转无符号
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 调试方法：打印磁盘前几块的内容
     */
    public static void debugPrint() {
        System.out.println("--- 磁盘物理状态检查 ---");
        for (int i = 0; i < 8; i++) {
            byte[] b = readBlock(i);
            System.out.printf("Block %d: ", i);
            for (byte val : b) {
                System.out.printf("%02X ", val);
            }
            System.out.println();
        }
    }
}
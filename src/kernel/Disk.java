package kernel;

import java.io.RandomAccessFile;

/**
 * 操作用文件模拟出来的磁盘
 */
/*RandomAccessFile raf = new RandomAccessFile("data.txt", "rw"),new时打开流，但又怕这个类的函数里会有很多
没用的对象留下，所以可以一进程序就打开这个文件，结束时关掉流。后面可以改写成这样*/
public class Disk
{
    // 磁盘初始化：先128个块全部清空，然后给FAT表的前三个字节填上255，然后给盘块2根目录的8个目录项都填上"$",表示这些是空目录项
    public static void start()
    {
        byte[] buffer = new byte[64];
        for (int i = 0; i < 64; i++)
        {
            buffer[i] = 0;
        }
        for (int i = 0; i < 128; i++)
        {
            writeBlock(i, buffer);
        }
        writeOnlyByte(0, 0, 0, (byte) 255);
        writeOnlyByte(0, 0, 1, (byte) 255);
        writeOnlyByte(0, 0, 2, (byte) 255);

        for(int i=0;i<8;i++) {
            Disk.writeOnlyByte(2, i, 0, (byte) '$');
        }
    }

    // 读文件 // 把下标为indexOfSector的盘块的64个字节读到byte[]里返回
    public static byte[] readBlock(int indexOfSector)
    {
        byte[] buffer = new byte[64];
        try (RandomAccessFile raf = new RandomAccessFile("data.txt", "rw"))
        {
            long pointer = indexOfSector * 64;
            for (int i = 0; i < 64; i++)
            {
                raf.seek(pointer + i);
                buffer[i] = raf.readByte();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return buffer;
    }

    // 往文件写 // 把byte[]里的64个字节写入到下标为indexOfSector的盘块里
    public static void writeBlock(int indexOfSector, byte[] buffer)
    {
        try (RandomAccessFile raf = new RandomAccessFile("data.txt", "rw"))//这句new的时候如果没有该文件会自动创建的
        {
            long pointer = indexOfSector * 64;
            for (int i = 0; i < 64; i++)
            {
                raf.seek(pointer + i);
                raf.writeByte(buffer[i]);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // 写单个字节
    public static void writeOnlyByte(int blockNum, int listNum, int bNum, byte word)
    {
        try (RandomAccessFile raf = new RandomAccessFile("data.txt", "rw"))
        {
            long pointer = blockNum * 64 + listNum * 8 + bNum;
            raf.seek(pointer);
            raf.writeByte(word);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // 读单个字节
    public static byte readOnlyByte(int blockNum, int listNum, int bNum)
    {
        byte b = -2;
        try (RandomAccessFile raf = new RandomAccessFile("data.txt", "rw"))
        {
            long pointer = blockNum * 64 + listNum * 8 + bNum;// 按字节来找的。比如（0，0，3）得到FAT表中的字节3（字节0开始）
            raf.seek(pointer);
            b = raf.readByte();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return b;
    }

    // 打印模拟硬盘信息，主要用于调试系统 //按字节，打印前8个块
	public static void diskPrint()
	{
		System.out.println("按字节，打印前8个块：");
		byte[] buffer = new byte[64];
		for (int i = 0; i < 8; i++)
		{
			buffer = readBlock(i);
			System.out.print("第" + i + "个块：");
			for (int j = 0; j < 64; j++)
			{
				System.out.printf("%4d", buffer[j]);
			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------");
	}


    // 回收磁盘块的块号为index:在FAT表中把这一块清0
    public static void freeBlock(int index)
    {
        byte[] buffer = readBlock(index / 64);
        buffer[index % 64] = 0;
        writeBlock(index / 64, buffer);
    }
}

package sys_core;

import java.util.Arrays;

/**
 * 文件控制块 (FCB - File Control Block)
 * 对应磁盘上存储的 8 字节目录项数据。
 */
public class FCB implements Cloneable {

    private byte[] nameBytes;      // 文件名
    private byte[] extBytes;       // 扩展名(后缀)
    private byte attribute;        // 属性
    private byte startBlock;       // 起始块号
    private byte length;           // 占用块数

    // 构造函数1：用于创建新文件对象
    public FCB(byte[] nameBytes, byte[] extBytes, byte attribute, byte startBlock, byte length) {
        this.nameBytes = nameBytes;
        this.extBytes = extBytes;
        this.attribute = attribute;
        this.startBlock = startBlock;
        this.length = length;
    }

    // 构造函数2：从磁盘原始数据中解析 FCB
    public FCB(byte[] buffer, int index) {
        int offset = index * 8;
        this.nameBytes = new byte[] { buffer[offset], buffer[offset + 1], buffer[offset + 2] };
        this.extBytes = new byte[] { buffer[offset + 3], buffer[offset + 4] };
        this.attribute = buffer[offset + 5];
        this.startBlock = buffer[offset + 6];
        this.length = buffer[offset + 7];
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        // 如果第一个字节是 '$'，直接返回，表示空
        if (nameBytes.length > 0 && nameBytes[0] == '$') return "$";

        for (byte b : nameBytes) {
            if (b != 0 && b != 32 && b != '$') sb.append((char) b);
        }

        if (this.attribute < 8) { // 属性8代表目录，目录没有后缀名
            sb.append('.');
            for (byte b : extBytes) {
                if (b != 0 && b != 32) sb.append((char) b);
            }
        }
        return sb.toString().trim();
    }

    @Override
    public FCB clone() {
        try {
            FCB copy = (FCB) super.clone();
            copy.nameBytes = this.nameBytes.clone();
            copy.extBytes = this.extBytes.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("FCB复制失败", e);
        }
    }

    // Getter & Setter
    public byte[] getNameBytes() { return nameBytes; }
    public void setNameBytes(byte[] nameBytes) { this.nameBytes = nameBytes; }
    public byte[] getExtBytes() { return extBytes; }
    public void setExtBytes(byte[] extBytes) { this.extBytes = extBytes; }
    public byte getAttribute() { return attribute; }
    public void setAttribute(byte attribute) { this.attribute = attribute; }
    public byte getStartBlock() { return startBlock; }
    public void setStartBlock(byte startBlock) { this.startBlock = startBlock; }
    public byte getLength() { return length; }
    public void setLength(byte length) { this.length = length; }
}
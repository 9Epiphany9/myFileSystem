package sys_core;

/**
 * 文件句柄 (File Handle)
 * 记录运行时状态。
 */
public class FileHandle {

    public static class Cursor {
        private int blockIndex;
        private int byteOffset;

        public Cursor(int blockIndex, int byteOffset) {
            this.blockIndex = blockIndex;
            this.byteOffset = byteOffset;
        }
        public int getBlockIndex() { return blockIndex; }
        public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }
        public int getByteOffset() { return byteOffset; }
        public void setByteOffset(int byteOffset) { this.byteOffset = byteOffset; }
    }

    private String filePath;
    private byte attribute;
    private int startBlock;
    private int fileSize;
    private int mode;

    private Cursor readCursor;
    private Cursor writeCursor;

    public FileHandle(String filePath, byte attribute, int startBlock, int fileSize, int mode) {
        this.filePath = filePath;
        this.attribute = attribute;
        this.startBlock = startBlock;
        this.fileSize = fileSize;
        this.mode = mode;
        this.readCursor = new Cursor(0, 0);
        this.writeCursor = new Cursor(0, 0);
    }

    public String getModeString() {
        return (mode == 0) ? "读取" : "写入";
    }

    public String getFilePath() { return filePath; }
    public byte getAttribute() { return attribute; }
    public int getStartBlock() { return startBlock; }
    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }
    public int getMode() { return mode; }
    public Cursor getReadCursor() { return readCursor; }
    public void setReadCursor(Cursor readCursor) { this.readCursor = readCursor; }
    public Cursor getWriteCursor() { return writeCursor; }
    public void setWriteCursor(Cursor writeCursor) { this.writeCursor = writeCursor; }
}
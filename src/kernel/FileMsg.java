package kernel;

import java.util.Arrays;

public class FileMsg implements Cloneable {
    private byte[] fileName;
    private byte[] backName;
    private byte attribute;
    private byte startBlock;
    private byte length;

    public FileMsg(byte[] fileName, byte[] backName, byte attribute, byte startBlock, byte length) {
        this.fileName = fileName;
        this.backName = backName;
        this.attribute = attribute;
        this.startBlock = startBlock;
        this.length = length;
    }

    public FileMsg(byte[] buffer, int index) {
        this.fileName = new byte[]{buffer[index * 8 + 0], buffer[index * 8 + 1], buffer[index * 8 + 2]};
        this.backName = new byte[]{buffer[index * 8 + 3], buffer[index * 8 + 4]};
        this.attribute = buffer[index * 8 + 5];
        this.startBlock = buffer[index * 8 + 6];
        this.length = buffer[index * 8 + 7];
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        FileMsg fileMsg = (FileMsg) super.clone();
        byte[] fileName = this.fileName;
        byte[] backName = this.backName;
        byte[] newFileName = (byte[]) fileName.clone();
        byte[] newBackName = (byte[]) backName.clone();
        fileMsg.fileName = newFileName;
        fileMsg.backName = newBackName;
        return fileMsg;
    }

    public String getAllName() {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < 3; i++) {
            if (fileName[i] == (byte) '\0') {
                break;
            }
            str.append((char) fileName[i]);
        }
        if (attribute < 8) {
            str.append('.');
            for (int i = 0; i < 2; i++) {
                if (backName[i] == (byte) '\0') {
                    break;
                }
                str.append((char) backName[i]);
            }
        }
        return new String(str).trim();
    }

    public static String getAllName2(byte[] fileMsg) {
        byte[] fileName = new byte[]{fileMsg[0], fileMsg[1], fileMsg[2]};
        byte[] backName = new byte[]{fileMsg[3], fileMsg[4]};
        byte attribute = fileMsg[5];

        StringBuffer str = new StringBuffer();
        for (int i = 0; i < 3; i++) {
            if (fileName[i] == (byte) '\0') {
                break;
            }
            str.append((char) fileName[i]);
        }
        if (attribute < 8) {
            str.append('.');
            for (int i = 0; i < 2; i++) {
                if (backName[i] == (byte) '\0') {
                    break;
                }
                str.append((char) backName[i]);
            }
        }
        return new String(str);
    }

    public byte[] getFileName() {
        return fileName;
    }

    public void setFileName(byte[] fileName) {
        this.fileName = fileName;
    }

    public byte[] getBackName() {
        return backName;
    }

    public void setBackName(byte[] backName) {
        this.backName = backName;
    }

    public byte getAttribute() {
        return attribute;
    }

    public void setAttribute(byte attribute) {
        this.attribute = attribute;
    }

    public byte getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(byte startBlock) {
        this.startBlock = startBlock;
    }

    public byte getLength() {
        return length;
    }

    public void setLength(byte length) {
        this.length = length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + attribute;
        result = prime * result + Arrays.hashCode(backName);
        result = prime * result + Arrays.hashCode(fileName);
        result = prime * result + length;
        result = prime * result + startBlock;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileMsg other = (FileMsg) obj;
        if (attribute != other.attribute)
            return false;
        if (!Arrays.equals(backName, other.backName))
            return false;
        if (!Arrays.equals(fileName, other.fileName))
            return false;
        if (length != other.length)
            return false;
        if (startBlock != other.startBlock)
            return false;
        return true;
    }
}
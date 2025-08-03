package kernel;

class Pointer {
    private int dnum; // 磁盘盘块号
    private int bnum; // 磁盘盘块内第几个字节

    public Pointer() {
        this.bnum = 0;
        this.dnum = 0;
    }

    public Pointer(int d, int b) {
        this.dnum = d;
        this.bnum = b;
    }

    public int getDnum() {
        return dnum;
    }

    public void setDnum(int dnum) {
        this.dnum = dnum;
    }

    public int getBnum() {
        return bnum;
    }

    public void setBnum(int bnum) {
        this.bnum = bnum;
    }
}

public class Oftle {
    private String name; // 文件绝对路径名
    private byte attribute; // 文件的属性
    private int number; // 文件起始盘块号
    private int length; // 文件长度，文件占用的字节数
    private int flag; // 操作类型，0表示读，1表示写
    private Pointer read; // 读文件的位置
    private Pointer write; // 写文件的位置

    public Oftle() {
        this.read = new Pointer();
        this.write = new Pointer();
    }

    public Oftle(String name, byte attribute, int number, int length, int flag) {
        this.name = name;
        this.attribute = attribute;
        this.number = number;
        this.length = length;
        this.flag = flag;
        this.read = new Pointer();
        this.write = new Pointer();
    }

    public void newPointer() {
        this.read = new Pointer();
        this.write = new Pointer();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getAttribute() {
        return attribute;
    }

    public void setAttribute(byte attribute) {
        this.attribute = attribute;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public Pointer getRead() {
        return read;
    }

    public void setRead(Pointer read) {
        this.read = read;
    }

    public Pointer getWrite() {
        return write;
    }

    public void setWrite(Pointer write) {
        this.write = write;
    }

    // 为 Swing 表格提供显示值
    public String getFlagDisplay() {
        return flag == 0 ? "读" : "写";
    }
}
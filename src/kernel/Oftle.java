package kernel;
class Pointer {
    private int dnum; // 磁盘块号
    private int bnum; // 块内字节偏移

    public Pointer() {
        this.dnum = -1; // 默认值表示未初始化
        this.bnum = 0;
    }

    public Pointer(int dnum, int bnum) {
        this.dnum = dnum;
        this.bnum = bnum;
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
    private String name;
    private byte attribute;
    private int number;
    private int length;
    private int flag;
    private Pointer read;
    private Pointer write;

    public Oftle() {
    }

    public Oftle(String name, byte attribute, int number, int length, int flag) {
        this.name = name;
        this.attribute = attribute;
        this.number = number;
        this.length = length;
        this.flag = flag;
        newPointer();
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

    public String getFlagString() {
        return (flag == 0) ? "读" : "写";
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
}
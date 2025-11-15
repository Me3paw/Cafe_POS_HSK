package entity;

public class ChiTietDonHang {
    private int id;
    private String maDH;
    private String maMon;
    private int soLuong;
    private long donGia;

    public ChiTietDonHang() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaDH() { return maDH; }
    public void setMaDH(String maDH) { this.maDH = maDH; }

    public String getMaMon() { return maMon; }
    public void setMaMon(String maMon) { this.maMon = maMon; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public long getDonGia() { return donGia; }
    public void setDonGia(long donGia) { this.donGia = donGia; }
}

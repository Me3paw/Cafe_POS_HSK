package entity;

import java.math.BigDecimal;

public class ChiTietDonHang {
    private int id;
    private int maDonHang;
    private int maMon;
    private int soLuong;
    private BigDecimal donGia;

    public ChiTietDonHang() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMaDonHang() { return maDonHang; }
    public void setMaDonHang(int maDonHang) { this.maDonHang = maDonHang; }

    public int getMaMon() { return maMon; }
    public void setMaMon(int maMon) { this.maMon = maMon; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}

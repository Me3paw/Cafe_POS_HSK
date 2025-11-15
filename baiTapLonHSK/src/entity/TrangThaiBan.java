package entity;

import java.sql.Timestamp;

public class TrangThaiBan {
    private int maBan;
    private Integer maDonHang;
    private String trangThai;
    private Integer soNguoi;
    private Timestamp capNhatCuoi;

    public TrangThaiBan() {}

    public TrangThaiBan(int maBan, String trangThai) {
        this.maBan = maBan;
        this.trangThai = trangThai;
    }

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }

    public Integer getMaDonHang() { return maDonHang; }
    public void setMaDonHang(Integer maDonHang) { this.maDonHang = maDonHang; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public Integer getSoNguoi() { return soNguoi; }
    public void setSoNguoi(Integer soNguoi) { this.soNguoi = soNguoi; }

    public Timestamp getCapNhatCuoi() { return capNhatCuoi; }
    public void setCapNhatCuoi(Timestamp capNhatCuoi) { this.capNhatCuoi = capNhatCuoi; }
}
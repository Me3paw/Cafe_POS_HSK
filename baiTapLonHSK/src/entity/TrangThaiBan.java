package entity;

import java.sql.Timestamp;

public class TrangThaiBan {
    private String maBan;
    private String trangThai; // values: Free, Occupied, Reserved, Under maintenance, Takeaway
    private Integer soNguoi; // nullable number of people at the table
    private String maDonHang; // nullable order id
    private Timestamp capNhatCuoi; // last update timestamp

    public TrangThaiBan() {}

    public TrangThaiBan(String maBan, String trangThai) {
        this.maBan = maBan;
        this.trangThai = trangThai;
    }

    public String getMaBan() { return maBan; }
    public void setMaBan(String maBan) { this.maBan = maBan; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public Integer getSoNguoi() { return soNguoi; }
    public void setSoNguoi(Integer soNguoi) { this.soNguoi = soNguoi; }

    public String getMaDonHang() { return maDonHang; }
    public void setMaDonHang(String maDonHang) { this.maDonHang = maDonHang; }

    public Timestamp getCapNhatCuoi() { return capNhatCuoi; }
    public void setCapNhatCuoi(Timestamp capNhatCuoi) { this.capNhatCuoi = capNhatCuoi; }
}
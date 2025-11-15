package entity;

import java.sql.Timestamp;

public class ThanhToan {
    private String maTT;
    private String maDH;
    private String hinhThuc;
    private long soTien;
    private Timestamp ngay;

    public ThanhToan() {}

    public String getMaTT() { return maTT; }
    public void setMaTT(String maTT) { this.maTT = maTT; }

    public String getMaDH() { return maDH; }
    public void setMaDH(String maDH) { this.maDH = maDH; }

    public String getHinhThuc() { return hinhThuc; }
    public void setHinhThuc(String hinhThuc) { this.hinhThuc = hinhThuc; }

    public long getSoTien() { return soTien; }
    public void setSoTien(long soTien) { this.soTien = soTien; }

    public Timestamp getNgay() { return ngay; }
    public void setNgay(Timestamp ngay) { this.ngay = ngay; }
}

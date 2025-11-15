package entity;

import java.math.BigDecimal;

public class ChiTietDonHang {
    private int maChiTiet;
    private int maDonHang;
    private int maMon;
    private int soLuong;
    private BigDecimal giaBan;
    private BigDecimal thanhTien;
    private Integer maThue;
    private BigDecimal tienThue;

    public ChiTietDonHang() {}

    public int getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(int maChiTiet) { this.maChiTiet = maChiTiet; }

    public int getMaDonHang() { return maDonHang; }
    public void setMaDonHang(int maDonHang) { this.maDonHang = maDonHang; }

    public int getMaMon() { return maMon; }
    public void setMaMon(int maMon) { this.maMon = maMon; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }

    public Integer getMaThue() { return maThue; }
    public void setMaThue(Integer maThue) { this.maThue = maThue; }

    public BigDecimal getTienThue() { return tienThue; }
    public void setTienThue(BigDecimal tienThue) { this.tienThue = tienThue; }
}

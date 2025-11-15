package entity;

import java.math.BigDecimal;

public class ChiTietDonHang {
    private int maChiTiet;
    private int soLuong;
    private BigDecimal giaBan;
    private BigDecimal thanhTien;
    private Integer maThue;
    private BigDecimal tienThue;

    // Optional object references for convenience
    private DonHang donHang;
    private Mon mon;

    public ChiTietDonHang() {}

    public int getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(int maChiTiet) { this.maChiTiet = maChiTiet; }

    public Integer getMaDonHang() {
        return donHang != null ? donHang.getMaDonHang() : null;
    }
    public void setMaDonHang(int maDonHang) {
        if (this.donHang == null) {
            this.donHang = new DonHang();
        }
        this.donHang.setMaDonHang(maDonHang);
    }

    public Integer getMaMon() {
        return mon != null ? mon.getMaMon() : null;
    }
    public void setMaMon(int maMon) {
        if (this.mon == null) {
            this.mon = new Mon();
        }
        this.mon.setMaMon(maMon);
    }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }

    public Integer getMaThue() { return maThue; }
    public void setMaThue(Integer maThue) {
        this.maThue = maThue;
    }

    public BigDecimal getTienThue() { return tienThue; }
    public void setTienThue(BigDecimal tienThue) { this.tienThue = tienThue; }

    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) {
        this.donHang = donHang;
    }

    public Mon getMon() { return mon; }
    public void setMon(Mon mon) {
        this.mon = mon;
    }

}

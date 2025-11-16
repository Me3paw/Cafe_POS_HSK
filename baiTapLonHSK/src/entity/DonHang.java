package entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class DonHang {
    private int maDonHang;
    private Integer maNguoiDung;
    private Integer maCa;
    private Integer maKhachHang;
    private Integer maGiamGia;
    private BigDecimal tongTien;
    private BigDecimal tienGiam;
    private BigDecimal tienThue;
    private BigDecimal tongCuoi;
    private String trangThai;
    private String loaiDon;
    private Timestamp thoiGianTao;
    private Integer maBan;

    public DonHang() {}

    public int getMaDonHang() { return maDonHang; }
    public void setMaDonHang(int maDonHang) { this.maDonHang = maDonHang; }

    public Integer getMaNguoiDung() { return maNguoiDung; }
    public void setMaNguoiDung(Integer maNguoiDung) { this.maNguoiDung = maNguoiDung; }

    public Integer getMaCa() { return maCa; }
    public void setMaCa(Integer maCa) { this.maCa = maCa; }

    public Integer getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Integer maKhachHang) { this.maKhachHang = maKhachHang; }

    public Integer getMaGiamGia() { return maGiamGia; }
    public void setMaGiamGia(Integer maGiamGia) { this.maGiamGia = maGiamGia; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public BigDecimal getTienGiam() { return tienGiam; }
    public void setTienGiam(BigDecimal tienGiam) { this.tienGiam = tienGiam; }

    public BigDecimal getTienThue() { return tienThue; }
    public void setTienThue(BigDecimal tienThue) { this.tienThue = tienThue; }

    public BigDecimal getTongCuoi() { return tongCuoi; }
    public void setTongCuoi(BigDecimal tongCuoi) { this.tongCuoi = tongCuoi; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getLoaiDon() { return loaiDon; }
    public void setLoaiDon(String loaiDon) { this.loaiDon = loaiDon; }

    public Timestamp getThoiGianTao() { return thoiGianTao; }
    public void setThoiGianTao(Timestamp thoiGianTao) { this.thoiGianTao = thoiGianTao; }

    public Integer getMaBan() { return maBan; }
    public void setMaBan(Integer maBan) { this.maBan = maBan; }
}

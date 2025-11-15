package entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GiamGia {
    private int maGiamGia;
    private String tenChuongTrinh;
    private String loai;
    private BigDecimal giaTri;
    private String hangApDung;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private boolean dangApDung;

    public GiamGia() {}

    public int getMaGiamGia() { return maGiamGia; }
    public void setMaGiamGia(int maGiamGia) { this.maGiamGia = maGiamGia; }

    public String getTenChuongTrinh() { return tenChuongTrinh; }
    public void setTenChuongTrinh(String tenChuongTrinh) { this.tenChuongTrinh = tenChuongTrinh; }

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public BigDecimal getGiaTri() { return giaTri; }
    public void setGiaTri(BigDecimal giaTri) { this.giaTri = giaTri; }

    public String getHangApDung() { return hangApDung; }
    public void setHangApDung(String hangApDung) { this.hangApDung = hangApDung; }

    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDate getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDate ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

    public boolean isDangApDung() { return dangApDung; }
    public void setDangApDung(boolean dangApDung) { this.dangApDung = dangApDung; }
}

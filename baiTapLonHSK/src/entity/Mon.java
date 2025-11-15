package entity;

import java.math.BigDecimal;

public class Mon {
    private int maMon;
    private Integer maDanhMuc;
    private String tenMon;
    private BigDecimal giaBan;
    private boolean conBan;
    private String moTa;

    public Mon() {}

    public int getMaMon() { return maMon; }
    public void setMaMon(int maMon) { this.maMon = maMon; }

    public Integer getMaDanhMuc() { return maDanhMuc; }
    public void setMaDanhMuc(Integer maDanhMuc) { this.maDanhMuc = maDanhMuc; }

    public String getTenMon() { return tenMon; }
    public void setTenMon(String tenMon) { this.tenMon = tenMon; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public boolean isConBan() { return conBan; }
    public void setConBan(boolean conBan) { this.conBan = conBan; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}

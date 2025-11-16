package entity;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    // Helper: compute a simple MD5 hash of key fields (tenMon|giaBan|maDanhMuc|conBan|moTa)
    public String computeHash() {
        try {
            String base = (tenMon == null ? "" : tenMon) + "|" +
                    (giaBan == null ? "" : giaBan.toPlainString()) + "|" +
                    (maDanhMuc == null ? "" : maDanhMuc.toString()) + "|" +
                    (conBan ? "1" : "0") + "|" +
                    (moTa == null ? "" : moTa);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(base.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // should not happen; fallback to simple string
            return String.valueOf((tenMon + "|" + giaBan + "|" + maDanhMuc + "|" + conBan + "|" + moTa).hashCode());
        }
    }
}
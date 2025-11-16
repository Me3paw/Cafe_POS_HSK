package entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class TonKho {
    private int maTon;
    private int maMon;
    private String donVi;
    private BigDecimal soLuong;
    private BigDecimal giaNhap;
    private BigDecimal mucCanhBao;
    private Timestamp capNhatCuoi;

    public TonKho() {}

    public int getMaTon() { return maTon; }
    public void setMaTon(int maTon) { this.maTon = maTon; }

    public int getMaMon() { return maMon; }
    public void setMaMon(int maMon) { this.maMon = maMon; }

    public String getDonVi() { return donVi; }
    public void setDonVi(String donVi) { this.donVi = donVi; }

    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }

    public BigDecimal getGiaNhap() { return giaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }

    public BigDecimal getMucCanhBao() { return mucCanhBao; }
    public void setMucCanhBao(BigDecimal mucCanhBao) { this.mucCanhBao = mucCanhBao; }

    public Timestamp getCapNhatCuoi() { return capNhatCuoi; }
    public void setCapNhatCuoi(Timestamp capNhatCuoi) { this.capNhatCuoi = capNhatCuoi; }
}

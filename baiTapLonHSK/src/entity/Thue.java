package entity;

import java.math.BigDecimal;

public class Thue {
    private int maThue;
    private String tenThue;
    private BigDecimal tyLe;
    private boolean dangApDung;

    public Thue() {}

    public int getMaThue() { return maThue; }
    public void setMaThue(int maThue) { this.maThue = maThue; }

    public String getTenThue() { return tenThue; }
    public void setTenThue(String tenThue) { this.tenThue = tenThue; }

    public BigDecimal getTyLe() { return tyLe; }
    public void setTyLe(BigDecimal tyLe) { this.tyLe = tyLe; }

    public boolean isDangApDung() { return dangApDung; }
    public void setDangApDung(boolean dangApDung) { this.dangApDung = dangApDung; }
}

package dao;

import connectDB.DBConnection;
import entity.Thue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThueDAO {
    public List<Thue> layHet() {
        List<Thue> res = new ArrayList<>();
        String sql = "SELECT maThue, tenThue, tyLe, dangApDung FROM thue";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Thue t = new Thue();
                t.setMaThue(rs.getInt("maThue"));
                t.setTenThue(rs.getString("tenThue"));
                t.setTyLe(rs.getBigDecimal("tyLe"));
                t.setDangApDung(rs.getBoolean("dangApDung"));
                res.add(t);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public Thue layTheoId(int maThue) {
        String sql = "SELECT maThue, tenThue, tyLe, dangApDung FROM thue WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maThue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Thue t = new Thue();
                    t.setMaThue(rs.getInt("maThue"));
                    t.setTenThue(rs.getString("tenThue"));
                    t.setTyLe(rs.getBigDecimal("tyLe"));
                    t.setDangApDung(rs.getBoolean("dangApDung"));
                    return t;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean them(Thue t) {
        String sql = "INSERT INTO thue(tenThue, tyLe, dangApDung) VALUES(?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTenThue());
            ps.setBigDecimal(2, t.getTyLe());
            ps.setBoolean(3, t.isDangApDung());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        t.setMaThue(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean capNhat(Thue t) {
        String sql = "UPDATE thue SET tenThue = ?, tyLe = ?, dangApDung = ? WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTenThue());
            ps.setBigDecimal(2, t.getTyLe());
            ps.setBoolean(3, t.isDangApDung());
            ps.setInt(4, t.getMaThue());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean xoa(int maThue) {
        String sql = "DELETE FROM thue WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maThue);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

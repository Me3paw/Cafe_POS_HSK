package dao;

import connectDB.DBConnection;
import entity.ThanhToan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThanhToanDAO {
    public List<ThanhToan> getAll() {
        List<ThanhToan> res = new ArrayList<>();
        String sql = "SELECT maTT, maDH, hinhThuc, soTien, ngay FROM ThanhToan";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ThanhToan t = new ThanhToan();
                t.setMaTT(rs.getString("maTT"));
                t.setMaDH(rs.getString("maDH"));
                t.setHinhThuc(rs.getString("hinhThuc"));
                t.setSoTien(rs.getLong("soTien"));
                t.setNgay(rs.getTimestamp("ngay"));
                res.add(t);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ThanhToan getById(String maTT) {
        String sql = "SELECT maTT, maDH, hinhThuc, soTien, ngay FROM ThanhToan WHERE maTT = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maTT);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ThanhToan t = new ThanhToan();
                    t.setMaTT(rs.getString("maTT"));
                    t.setMaDH(rs.getString("maDH"));
                    t.setHinhThuc(rs.getString("hinhThuc"));
                    t.setSoTien(rs.getLong("soTien"));
                    t.setNgay(rs.getTimestamp("ngay"));
                    return t;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean insert(ThanhToan t) {
        String sql = "INSERT INTO ThanhToan(maTT, maDH, hinhThuc, soTien, ngay) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getMaTT());
            ps.setString(2, t.getMaDH());
            ps.setString(3, t.getHinhThuc());
            ps.setLong(4, t.getSoTien());
            ps.setTimestamp(5, t.getNgay());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(ThanhToan t) {
        String sql = "UPDATE ThanhToan SET maDH = ?, hinhThuc = ?, soTien = ?, ngay = ? WHERE maTT = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getMaDH());
            ps.setString(2, t.getHinhThuc());
            ps.setLong(3, t.getSoTien());
            ps.setTimestamp(4, t.getNgay());
            ps.setString(5, t.getMaTT());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(String maTT) {
        String sql = "DELETE FROM ThanhToan WHERE maTT = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maTT);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}

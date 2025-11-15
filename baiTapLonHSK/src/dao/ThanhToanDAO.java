package dao;

import connectDB.DBConnection;
import entity.ThanhToan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThanhToanDAO {
    public List<ThanhToan> getAll() {
        List<ThanhToan> res = new ArrayList<>();
        String sql = "SELECT maThanhToan, maDonHang, hinhThuc, soTien, thoiGian FROM thanhToan";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ThanhToan getById(int maThanhToan) {
        String sql = "SELECT maThanhToan, maDonHang, hinhThuc, soTien, thoiGian FROM thanhToan WHERE maThanhToan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maThanhToan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean insert(ThanhToan t) {
        String sql = "INSERT INTO thanhToan(maDonHang, hinhThuc, soTien, thoiGian) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getMaDonHang());
            ps.setString(2, t.getHinhThuc());
            ps.setBigDecimal(3, t.getSoTien());
            ps.setTimestamp(4, t.getThoiGian());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        t.setMaThanhToan(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(ThanhToan t) {
        String sql = "UPDATE thanhToan SET maDonHang = ?, hinhThuc = ?, soTien = ?, thoiGian = ? WHERE maThanhToan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getMaDonHang());
            ps.setString(2, t.getHinhThuc());
            ps.setBigDecimal(3, t.getSoTien());
            ps.setTimestamp(4, t.getThoiGian());
            ps.setInt(5, t.getMaThanhToan());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(int maThanhToan) {
        String sql = "DELETE FROM thanhToan WHERE maThanhToan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maThanhToan);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private ThanhToan mapRow(ResultSet rs) throws SQLException {
        ThanhToan t = new ThanhToan();
        t.setMaThanhToan(rs.getInt("maThanhToan"));
        t.setMaDonHang(rs.getInt("maDonHang"));
        t.setHinhThuc(rs.getString("hinhThuc"));
        t.setSoTien(rs.getBigDecimal("soTien"));
        t.setThoiGian(rs.getTimestamp("thoiGian"));
        return t;
    }
}

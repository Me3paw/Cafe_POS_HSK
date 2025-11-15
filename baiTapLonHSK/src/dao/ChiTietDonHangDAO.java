package dao;

import connectDB.DBConnection;
import entity.ChiTietDonHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDonHangDAO {
    public List<ChiTietDonHang> getAll() {
        List<ChiTietDonHang> res = new ArrayList<>();
        String sql = "SELECT id, maDonHang, maMon, soLuong, donGia FROM chiTietDonHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ChiTietDonHang getById(int id) {
        String sql = "SELECT id, maDonHang, maMon, soLuong, donGia FROM chiTietDonHang WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
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

    public boolean insert(ChiTietDonHang ct) {
        String sql = "INSERT INTO chiTietDonHang(maDonHang, maMon, soLuong, donGia) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ct.getMaDonHang());
            ps.setInt(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setBigDecimal(4, ct.getDonGia());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) ct.setId(keys.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(ChiTietDonHang ct) {
        String sql = "UPDATE chiTietDonHang SET maDonHang = ?, maMon = ?, soLuong = ?, donGia = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ct.getMaDonHang());
            ps.setInt(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setBigDecimal(4, ct.getDonGia());
            ps.setInt(5, ct.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM chiTietDonHang WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private ChiTietDonHang mapRow(ResultSet rs) throws SQLException {
        ChiTietDonHang ct = new ChiTietDonHang();
        ct.setId(rs.getInt("id"));
        ct.setMaDonHang(rs.getInt("maDonHang"));
        ct.setMaMon(rs.getInt("maMon"));
        ct.setSoLuong(rs.getInt("soLuong"));
        ct.setDonGia(rs.getBigDecimal("donGia"));
        return ct;
    }
}

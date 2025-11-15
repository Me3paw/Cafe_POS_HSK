package dao;

import connectDB.DBConnection;
import entity.ChiTietDonHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDonHangDAO {
    public List<ChiTietDonHang> getAll() {
        List<ChiTietDonHang> res = new ArrayList<>();
        String sql = "SELECT id, maDH, maMon, soLuong, donGia FROM ChiTietDonHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ChiTietDonHang ct = new ChiTietDonHang();
                ct.setId(rs.getInt("id"));
                ct.setMaDH(rs.getString("maDH"));
                ct.setMaMon(rs.getString("maMon"));
                ct.setSoLuong(rs.getInt("soLuong"));
                ct.setDonGia(rs.getLong("donGia"));
                res.add(ct);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ChiTietDonHang getById(int id) {
        String sql = "SELECT id, maDH, maMon, soLuong, donGia FROM ChiTietDonHang WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ChiTietDonHang ct = new ChiTietDonHang();
                    ct.setId(rs.getInt("id"));
                    ct.setMaDH(rs.getString("maDH"));
                    ct.setMaMon(rs.getString("maMon"));
                    ct.setSoLuong(rs.getInt("soLuong"));
                    ct.setDonGia(rs.getLong("donGia"));
                    return ct;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean insert(ChiTietDonHang ct) {
        String sql = "INSERT INTO ChiTietDonHang(maDH, maMon, soLuong, donGia) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ct.getMaDH());
            ps.setString(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setLong(4, ct.getDonGia());
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
        String sql = "UPDATE ChiTietDonHang SET maDH = ?, maMon = ?, soLuong = ?, donGia = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ct.getMaDH());
            ps.setString(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setLong(4, ct.getDonGia());
            ps.setInt(5, ct.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM ChiTietDonHang WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}

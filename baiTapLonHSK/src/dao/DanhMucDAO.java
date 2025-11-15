package dao;

import connectDB.DBConnection;
import entity.DanhMuc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DanhMucDAO {
    public List<DanhMuc> getAll() {
        List<DanhMuc> res = new ArrayList<>();
        String sql = "SELECT maDanhMuc, tenDanhMuc, moTa FROM danhMuc";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DanhMuc d = new DanhMuc();
                d.setMaDanhMuc(rs.getInt("maDanhMuc"));
                d.setTenDanhMuc(rs.getString("tenDanhMuc"));
                d.setMoTa(rs.getString("moTa"));
                res.add(d);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public DanhMuc getById(int maDanhMuc) {
        String sql = "SELECT maDanhMuc, tenDanhMuc, moTa FROM danhMuc WHERE maDanhMuc = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DanhMuc d = new DanhMuc();
                    d.setMaDanhMuc(rs.getInt("maDanhMuc"));
                    d.setTenDanhMuc(rs.getString("tenDanhMuc"));
                    d.setMoTa(rs.getString("moTa"));
                    return d;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(DanhMuc d) {
        String sql = "INSERT INTO danhMuc(tenDanhMuc, moTa) VALUES(?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getTenDanhMuc());
            ps.setString(2, d.getMoTa());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        d.setMaDanhMuc(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(DanhMuc d) {
        String sql = "UPDATE danhMuc SET tenDanhMuc = ?, moTa = ? WHERE maDanhMuc = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getTenDanhMuc());
            ps.setString(2, d.getMoTa());
            ps.setInt(3, d.getMaDanhMuc());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(int maDanhMuc) {
        String sql = "DELETE FROM danhMuc WHERE maDanhMuc = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

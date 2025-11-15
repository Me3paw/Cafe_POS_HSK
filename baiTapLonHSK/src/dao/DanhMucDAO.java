package dao;

import connectDB.DBConnection;
import entity.DanhMuc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DanhMucDAO {
    public List<DanhMuc> getAll() {
        List<DanhMuc> res = new ArrayList<>();
        String sql = "SELECT maDM, ten FROM DanhMuc";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DanhMuc d = new DanhMuc();
                d.setMaDM(rs.getString("maDM"));
                d.setTen(rs.getString("ten"));
                res.add(d);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public DanhMuc getById(String maDM) {
        String sql = "SELECT maDM, ten FROM DanhMuc WHERE maDM = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maDM);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DanhMuc d = new DanhMuc();
                    d.setMaDM(rs.getString("maDM"));
                    d.setTen(rs.getString("ten"));
                    return d;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(DanhMuc d) {
        String sql = "INSERT INTO DanhMuc(maDM, ten) VALUES(?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getMaDM());
            ps.setString(2, d.getTen());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(DanhMuc d) {
        String sql = "UPDATE DanhMuc SET ten = ? WHERE maDM = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getTen());
            ps.setString(2, d.getMaDM());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maDM) {
        String sql = "DELETE FROM DanhMuc WHERE maDM = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maDM);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

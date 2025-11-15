package dao;

import connectDB.DBConnection;
import entity.Mon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MonDAO {
    public List<Mon> getAll() {
        List<Mon> res = new ArrayList<>();
        String sql = "SELECT maMon, ten, gia, tonKho FROM Mon";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Mon m = new Mon();
                m.setMaMon(rs.getString("maMon"));
                m.setTen(rs.getString("ten"));
                m.setGia(rs.getLong("gia"));
                m.setTonKho(rs.getInt("tonKho"));
                res.add(m);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public Mon getById(String maMon) {
        String sql = "SELECT maMon, ten, gia, tonKho FROM Mon WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maMon);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Mon m = new Mon();
                    m.setMaMon(rs.getString("maMon"));
                    m.setTen(rs.getString("ten"));
                    m.setGia(rs.getLong("gia"));
                    m.setTonKho(rs.getInt("tonKho"));
                    return m;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(Mon m) {
        String sql = "INSERT INTO Mon(maMon, ten, gia, tonKho) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getMaMon());
            ps.setString(2, m.getTen());
            ps.setLong(3, m.getGia());
            ps.setInt(4, m.getTonKho());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(Mon m) {
        String sql = "UPDATE Mon SET ten = ?, gia = ?, tonKho = ? WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getTen());
            ps.setLong(2, m.getGia());
            ps.setInt(3, m.getTonKho());
            ps.setString(4, m.getMaMon());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maMon) {
        String sql = "DELETE FROM Mon WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maMon);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

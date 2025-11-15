package dao;

import connectDB.DBConnection;
import entity.Thue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThueDAO {
    public List<Thue> getAll() {
        List<Thue> res = new ArrayList<>();
        String sql = "SELECT maThue, ten, phanTram FROM Thue";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Thue t = new Thue();
                t.setMaThue(rs.getString("maThue"));
                t.setTen(rs.getString("ten"));
                t.setPhanTram(rs.getDouble("phanTram"));
                res.add(t);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public Thue getById(String maThue) {
        String sql = "SELECT maThue, ten, phanTram FROM Thue WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maThue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Thue t = new Thue();
                    t.setMaThue(rs.getString("maThue"));
                    t.setTen(rs.getString("ten"));
                    t.setPhanTram(rs.getDouble("phanTram"));
                    return t;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(Thue t) {
        String sql = "INSERT INTO Thue(maThue, ten, phanTram) VALUES(?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getMaThue());
            ps.setString(2, t.getTen());
            ps.setDouble(3, t.getPhanTram());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(Thue t) {
        String sql = "UPDATE Thue SET ten = ?, phanTram = ? WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTen());
            ps.setDouble(2, t.getPhanTram());
            ps.setString(3, t.getMaThue());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maThue) {
        String sql = "DELETE FROM Thue WHERE maThue = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maThue);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

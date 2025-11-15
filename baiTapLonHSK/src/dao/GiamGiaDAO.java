package dao;

import connectDB.DBConnection;
import entity.GiamGia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GiamGiaDAO {
    public List<GiamGia> getAll() {
        List<GiamGia> res = new ArrayList<>();
        String sql = "SELECT maGG, ten, phanTram FROM GiamGia";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiamGia g = new GiamGia();
                g.setMaGG(rs.getString("maGG"));
                g.setTen(rs.getString("ten"));
                g.setPhanTram(rs.getDouble("phanTram"));
                res.add(g);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public GiamGia getById(String maGG) {
        String sql = "SELECT maGG, ten, phanTram FROM GiamGia WHERE maGG = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maGG);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GiamGia g = new GiamGia();
                    g.setMaGG(rs.getString("maGG"));
                    g.setTen(rs.getString("ten"));
                    g.setPhanTram(rs.getDouble("phanTram"));
                    return g;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(GiamGia g) {
        String sql = "INSERT INTO GiamGia(maGG, ten, phanTram) VALUES(?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getMaGG());
            ps.setString(2, g.getTen());
            ps.setDouble(3, g.getPhanTram());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(GiamGia g) {
        String sql = "UPDATE GiamGia SET ten = ?, phanTram = ? WHERE maGG = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getTen());
            ps.setDouble(2, g.getPhanTram());
            ps.setString(3, g.getMaGG());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maGG) {
        String sql = "DELETE FROM GiamGia WHERE maGG = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maGG);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

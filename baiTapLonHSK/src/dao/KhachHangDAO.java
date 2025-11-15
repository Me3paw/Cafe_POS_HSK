package dao;

import connectDB.DBConnection;
import entity.KhachHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {
    public List<KhachHang> getAll() {
        List<KhachHang> res = new ArrayList<>();
        String sql = "SELECT maKH, ten, sdt FROM KhachHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KhachHang k = new KhachHang();
                k.setMaKH(rs.getString("maKH"));
                k.setTen(rs.getString("ten"));
                k.setSdt(rs.getString("sdt"));
                res.add(k);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public KhachHang getById(String maKH) {
        String sql = "SELECT maKH, ten, sdt FROM KhachHang WHERE maKH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    KhachHang k = new KhachHang();
                    k.setMaKH(rs.getString("maKH"));
                    k.setTen(rs.getString("ten"));
                    k.setSdt(rs.getString("sdt"));
                    return k;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(KhachHang k) {
        String sql = "INSERT INTO KhachHang(maKH, ten, sdt) VALUES(?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.getMaKH());
            ps.setString(2, k.getTen());
            ps.setString(3, k.getSdt());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(KhachHang k) {
        String sql = "UPDATE KhachHang SET ten = ?, sdt = ? WHERE maKH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.getTen());
            ps.setString(2, k.getSdt());
            ps.setString(3, k.getMaKH());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maKH) {
        String sql = "DELETE FROM KhachHang WHERE maKH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKH);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

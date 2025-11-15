package dao;

import connectDB.DBConnection;
import entity.KhachHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {
    public List<KhachHang> getAll() {
        List<KhachHang> res = new ArrayList<>();
        String sql = "SELECT maKhachHang, hoTen, soDienThoai, hangThanhVien, ngayTao FROM khachHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KhachHang k = new KhachHang();
                k.setMaKhachHang(rs.getInt("maKhachHang"));
                k.setHoTen(rs.getString("hoTen"));
                k.setSoDienThoai(rs.getString("soDienThoai"));
                k.setHangThanhVien(rs.getString("hangThanhVien"));
                k.setNgayTao(rs.getTimestamp("ngayTao"));
                res.add(k);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public KhachHang getById(int maKhachHang) {
        String sql = "SELECT maKhachHang, hoTen, soDienThoai, hangThanhVien, ngayTao FROM khachHang WHERE maKhachHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhachHang);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    KhachHang k = new KhachHang();
                    k.setMaKhachHang(rs.getInt("maKhachHang"));
                    k.setHoTen(rs.getString("hoTen"));
                    k.setSoDienThoai(rs.getString("soDienThoai"));
                    k.setHangThanhVien(rs.getString("hangThanhVien"));
                    k.setNgayTao(rs.getTimestamp("ngayTao"));
                    return k;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(KhachHang k) {
        String sql = "INSERT INTO khachHang(hoTen, soDienThoai, hangThanhVien) VALUES(?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, k.getHoTen());
            ps.setString(2, k.getSoDienThoai());
            ps.setString(3, k.getHangThanhVien());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        k.setMaKhachHang(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(KhachHang k) {
        String sql = "UPDATE khachHang SET hoTen = ?, soDienThoai = ?, hangThanhVien = ? WHERE maKhachHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.getHoTen());
            ps.setString(2, k.getSoDienThoai());
            ps.setString(3, k.getHangThanhVien());
            ps.setInt(4, k.getMaKhachHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(int maKhachHang) {
        String sql = "DELETE FROM khachHang WHERE maKhachHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maKhachHang);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

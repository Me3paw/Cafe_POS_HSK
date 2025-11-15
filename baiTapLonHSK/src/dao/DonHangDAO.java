package dao;

import connectDB.DBConnection;
import entity.DonHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonHangDAO {
    public List<DonHang> getAll() {
        List<DonHang> res = new ArrayList<>();
        String sql = "SELECT maDH, maBan, ngayTao, tongTien, trangThai FROM DonHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DonHang d = new DonHang();
                d.setMaDH(rs.getString("maDH"));
                d.setMaBan(rs.getString("maBan"));
                d.setNgayTao(rs.getTimestamp("ngayTao"));
                d.setTongTien(rs.getLong("tongTien"));
                d.setTrangThai(rs.getString("trangThai"));
                res.add(d);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public DonHang getById(String maDH) {
        String sql = "SELECT maDH, maBan, ngayTao, tongTien, trangThai FROM DonHang WHERE maDH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maDH);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DonHang d = new DonHang();
                    d.setMaDH(rs.getString("maDH"));
                    d.setMaBan(rs.getString("maBan"));
                    d.setNgayTao(rs.getTimestamp("ngayTao"));
                    d.setTongTien(rs.getLong("tongTien"));
                    d.setTrangThai(rs.getString("trangThai"));
                    return d;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean insert(DonHang d) {
        String sql = "INSERT INTO DonHang(maDH, maBan, ngayTao, tongTien, trangThai) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getMaDH());
            ps.setString(2, d.getMaBan());
            ps.setTimestamp(3, d.getNgayTao());
            ps.setLong(4, d.getTongTien());
            ps.setString(5, d.getTrangThai());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(DonHang d) {
        String sql = "UPDATE DonHang SET maBan = ?, ngayTao = ?, tongTien = ?, trangThai = ? WHERE maDH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getMaBan());
            ps.setTimestamp(2, d.getNgayTao());
            ps.setLong(3, d.getTongTien());
            ps.setString(4, d.getTrangThai());
            ps.setString(5, d.getMaDH());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(String maDH) {
        String sql = "DELETE FROM DonHang WHERE maDH = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maDH);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}

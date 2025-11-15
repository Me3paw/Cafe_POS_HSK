package dao;

import connectDB.DBConnection;
import entity.DonHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonHangDAO {
    public List<DonHang> getAll() {
        List<DonHang> res = new ArrayList<>();
        String sql = "SELECT maDonHang, maNguoiDung, maCa, maKhachHang, maGiamGia, tongTien, tienGiam, tienThue, tongCuoi, trangThai, loaiDon, thoiGianTao, maBan FROM donHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public DonHang getById(int maDonHang) {
        String sql = "SELECT maDonHang, maNguoiDung, maCa, maKhachHang, maGiamGia, tongTien, tienGiam, tienThue, tongCuoi, trangThai, loaiDon, thoiGianTao, maBan FROM donHang WHERE maDonHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDonHang);
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

    public boolean insert(DonHang d) {
        String sql = "INSERT INTO donHang(maNguoiDung, maCa, maKhachHang, maGiamGia, tongTien, tienGiam, tienThue, tongCuoi, trangThai, loaiDon, maBan) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, d.getMaNguoiDung());
            setNullableInt(ps, 2, d.getMaCa());
            setNullableInt(ps, 3, d.getMaKhachHang());
            setNullableInt(ps, 4, d.getMaGiamGia());
            ps.setBigDecimal(5, d.getTongTien());
            ps.setBigDecimal(6, d.getTienGiam());
            ps.setBigDecimal(7, d.getTienThue());
            ps.setBigDecimal(8, d.getTongCuoi());
            ps.setString(9, d.getTrangThai());
            ps.setString(10, d.getLoaiDon());
            ps.setInt(11, d.getMaBan());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        d.setMaDonHang(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(DonHang d) {
        String sql = "UPDATE donHang SET maNguoiDung = ?, maCa = ?, maKhachHang = ?, maGiamGia = ?, tongTien = ?, tienGiam = ?, tienThue = ?, tongCuoi = ?, trangThai = ?, loaiDon = ?, maBan = ? WHERE maDonHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setNullableInt(ps, 1, d.getMaNguoiDung());
            setNullableInt(ps, 2, d.getMaCa());
            setNullableInt(ps, 3, d.getMaKhachHang());
            setNullableInt(ps, 4, d.getMaGiamGia());
            ps.setBigDecimal(5, d.getTongTien());
            ps.setBigDecimal(6, d.getTienGiam());
            ps.setBigDecimal(7, d.getTienThue());
            ps.setBigDecimal(8, d.getTongCuoi());
            ps.setString(9, d.getTrangThai());
            ps.setString(10, d.getLoaiDon());
            ps.setInt(11, d.getMaBan());
            ps.setInt(12, d.getMaDonHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean delete(int maDonHang) {
        String sql = "DELETE FROM donHang WHERE maDonHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDonHang);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private DonHang mapRow(ResultSet rs) throws SQLException {
        DonHang d = new DonHang();
        d.setMaDonHang(rs.getInt("maDonHang"));
        int maNguoiDung = rs.getInt("maNguoiDung");
        d.setMaNguoiDung(rs.wasNull() ? null : maNguoiDung);
        int maCa = rs.getInt("maCa");
        d.setMaCa(rs.wasNull() ? null : maCa);
        int maKhachHang = rs.getInt("maKhachHang");
        d.setMaKhachHang(rs.wasNull() ? null : maKhachHang);
        int maGiamGia = rs.getInt("maGiamGia");
        d.setMaGiamGia(rs.wasNull() ? null : maGiamGia);
        d.setTongTien(rs.getBigDecimal("tongTien"));
        d.setTienGiam(rs.getBigDecimal("tienGiam"));
        d.setTienThue(rs.getBigDecimal("tienThue"));
        d.setTongCuoi(rs.getBigDecimal("tongCuoi"));
        d.setTrangThai(rs.getString("trangThai"));
        d.setLoaiDon(rs.getString("loaiDon"));
        d.setThoiGianTao(rs.getTimestamp("thoiGianTao"));
        d.setMaBan(rs.getInt("maBan"));
        return d;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }
}

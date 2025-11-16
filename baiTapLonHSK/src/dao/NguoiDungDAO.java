package dao;

import connectDB.DBConnection;
import entity.NguoiDung;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NguoiDungDAO {
    public List<NguoiDung> layHet() {
        List<NguoiDung> res = new ArrayList<>();
        String sql = "SELECT maNguoiDung, tenDangNhap, hoTen, vaiTro, matKhau, ngayTao FROM nguoiDung";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NguoiDung n = new NguoiDung();
                n.setMaNguoiDung(rs.getInt("maNguoiDung"));
                n.setTenDangNhap(rs.getString("tenDangNhap"));
                n.setHoTen(rs.getString("hoTen"));
                n.setVaiTro(rs.getString("vaiTro"));
                n.setMatKhau(rs.getString("matKhau"));
                n.setNgayTao(rs.getTimestamp("ngayTao"));
                res.add(n);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public NguoiDung layTheoTenDangNhap(String tenDangNhap) {
        String sql = "SELECT maNguoiDung, tenDangNhap, hoTen, vaiTro, matKhau, ngayTao FROM nguoiDung WHERE tenDangNhap = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NguoiDung n = new NguoiDung();
                    n.setMaNguoiDung(rs.getInt("maNguoiDung"));
                    n.setTenDangNhap(rs.getString("tenDangNhap"));
                    n.setHoTen(rs.getString("hoTen"));
                    n.setVaiTro(rs.getString("vaiTro"));
                    n.setMatKhau(rs.getString("matKhau"));
                    n.setNgayTao(rs.getTimestamp("ngayTao"));
                    return n;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public NguoiDung layTheoId(int maNguoiDung) {
        String sql = "SELECT maNguoiDung, tenDangNhap, hoTen, vaiTro, matKhau, ngayTao FROM nguoiDung WHERE maNguoiDung = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maNguoiDung);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NguoiDung n = new NguoiDung();
                    n.setMaNguoiDung(rs.getInt("maNguoiDung"));
                    n.setTenDangNhap(rs.getString("tenDangNhap"));
                    n.setHoTen(rs.getString("hoTen"));
                    n.setVaiTro(rs.getString("vaiTro"));
                    n.setMatKhau(rs.getString("matKhau"));
                    n.setNgayTao(rs.getTimestamp("ngayTao"));
                    return n;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean them(NguoiDung n) {
        String sql = "INSERT INTO nguoiDung(tenDangNhap, hoTen, vaiTro, matKhau) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getTenDangNhap());
            ps.setString(2, n.getHoTen());
            ps.setString(3, n.getVaiTro());
            ps.setString(4, n.getMatKhau());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        n.setMaNguoiDung(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean capNhat(NguoiDung n) {
        String sql = "UPDATE nguoiDung SET tenDangNhap = ?, hoTen = ?, vaiTro = ?, matKhau = ? WHERE maNguoiDung = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getTenDangNhap());
            ps.setString(2, n.getHoTen());
            ps.setString(3, n.getVaiTro());
            ps.setString(4, n.getMatKhau());
            ps.setInt(5, n.getMaNguoiDung());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean capNhatMatKhau(int maNguoiDung, String matKhauMoi) {
        String sql = "UPDATE nguoiDung SET matKhau = ? WHERE maNguoiDung = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, matKhauMoi);
            ps.setInt(2, maNguoiDung);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean capNhatVaiTro(int maNguoiDung, String vaiTro) {
        String sql = "UPDATE nguoiDung SET vaiTro = ? WHERE maNguoiDung = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, vaiTro);
            ps.setInt(2, maNguoiDung);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean xoa(int maNguoiDung) {
        String sql = "DELETE FROM nguoiDung WHERE maNguoiDung = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maNguoiDung);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

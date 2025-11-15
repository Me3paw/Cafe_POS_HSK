package dao;

import connectDB.DBConnection;
import entity.NguoiDung;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NguoiDungDAO {
    public List<NguoiDung> getAll() {
        List<NguoiDung> res = new ArrayList<>();
        String sql = "SELECT maND, ten, matKhau, quyen FROM NguoiDung";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NguoiDung n = new NguoiDung();
                n.setMaND(rs.getString("maND"));
                n.setTen(rs.getString("ten"));
                n.setMatKhau(rs.getString("matKhau"));
                n.setQuyen(rs.getString("quyen"));
                res.add(n);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public NguoiDung getById(String maND) {
        String sql = "SELECT maND, ten, matKhau, quyen FROM NguoiDung WHERE maND = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maND);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NguoiDung n = new NguoiDung();
                    n.setMaND(rs.getString("maND"));
                    n.setTen(rs.getString("ten"));
                    n.setMatKhau(rs.getString("matKhau"));
                    n.setQuyen(rs.getString("quyen"));
                    return n;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(NguoiDung n) {
        String sql = "INSERT INTO NguoiDung(maND, ten, matKhau, quyen) VALUES(?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getMaND());
            ps.setString(2, n.getTen());
            ps.setString(3, n.getMatKhau());
            ps.setString(4, n.getQuyen());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(NguoiDung n) {
        String sql = "UPDATE NguoiDung SET ten = ?, matKhau = ?, quyen = ? WHERE maND = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getTen());
            ps.setString(2, n.getMatKhau());
            ps.setString(3, n.getQuyen());
            ps.setString(4, n.getMaND());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(String maND) {
        String sql = "DELETE FROM NguoiDung WHERE maND = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maND);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

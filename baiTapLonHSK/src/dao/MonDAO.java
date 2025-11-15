package dao;

import connectDB.DBConnection;
import entity.Mon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MonDAO {
    public List<Mon> getAll() {
        List<Mon> res = new ArrayList<>();
        String sql = "SELECT maMon, maDanhMuc, tenMon, giaBan, conBan, moTa FROM mon";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Mon m = new Mon();
                m.setMaMon(rs.getInt("maMon"));
                int maDM = rs.getInt("maDanhMuc");
                m.setMaDanhMuc(rs.wasNull() ? null : maDM);
                m.setTenMon(rs.getString("tenMon"));
                m.setGiaBan(rs.getBigDecimal("giaBan"));
                m.setConBan(rs.getBoolean("conBan"));
                m.setMoTa(rs.getString("moTa"));
                res.add(m);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public Mon getById(int maMon) {
        String sql = "SELECT maMon, maDanhMuc, tenMon, giaBan, conBan, moTa FROM mon WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Mon m = new Mon();
                    m.setMaMon(rs.getInt("maMon"));
                    int maDM = rs.getInt("maDanhMuc");
                    m.setMaDanhMuc(rs.wasNull() ? null : maDM);
                    m.setTenMon(rs.getString("tenMon"));
                    m.setGiaBan(rs.getBigDecimal("giaBan"));
                    m.setConBan(rs.getBoolean("conBan"));
                    m.setMoTa(rs.getString("moTa"));
                    return m;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean insert(Mon m) {
        String sql = "INSERT INTO mon(maDanhMuc, tenMon, giaBan, conBan, moTa) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (m.getMaDanhMuc() != null) {
                ps.setInt(1, m.getMaDanhMuc());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, m.getTenMon());
            ps.setBigDecimal(3, m.getGiaBan());
            ps.setBoolean(4, m.isConBan());
            ps.setString(5, m.getMoTa());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        m.setMaMon(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(Mon m) {
        String sql = "UPDATE mon SET maDanhMuc = ?, tenMon = ?, giaBan = ?, conBan = ?, moTa = ? WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (m.getMaDanhMuc() != null) {
                ps.setInt(1, m.getMaDanhMuc());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, m.getTenMon());
            ps.setBigDecimal(3, m.getGiaBan());
            ps.setBoolean(4, m.isConBan());
            ps.setString(5, m.getMoTa());
            ps.setInt(6, m.getMaMon());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(int maMon) {
        String sql = "DELETE FROM mon WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
}

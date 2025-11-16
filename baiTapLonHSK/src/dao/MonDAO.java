package dao;

import connectDB.DBConnection;
import entity.Mon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MonDAO {
    public List<Mon> layHet() {
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

    public Mon layTheoId(int maMon) {
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

    public boolean them(Mon m) {
        // Validate: giaBan > giaNhap in TonKho (if exists)
        String validationMsg = validateGiaBan(m.getMaMon(), m.getGiaBan());
        if (validationMsg != null) {
            System.err.println("Validation error: " + validationMsg);
            return false;
        }

        // Use table lock to atomically compute MAX(maMon)+1 and insert with that id.
        String lockSql = "LOCK TABLES mon WRITE";
        String unlockSql = "UNLOCK TABLES";
        String maxSql = "SELECT COALESCE(MAX(maMon), 0) AS mx FROM mon";
        String insertSql = "INSERT INTO mon(maMon, maDanhMuc, tenMon, giaBan, conBan, moTa) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection c = DBConnection.getConnection(); Statement stmt = c.createStatement()) {
            // Lock the table to prevent concurrent inserts
            stmt.execute(lockSql);
            try (PreparedStatement psMax = c.prepareStatement(maxSql); ResultSet rs = psMax.executeQuery()) {
                int nextId = 1;
                if (rs.next()) {
                    nextId = rs.getInt("mx") + 1;
                }
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setInt(1, nextId);
                    if (m.getMaDanhMuc() != null) {
                        ps.setInt(2, m.getMaDanhMuc());
                    } else {
                        ps.setNull(2, Types.INTEGER);
                    }
                    ps.setString(3, m.getTenMon());
                    ps.setBigDecimal(4, m.getGiaBan());
                    ps.setBoolean(5, m.isConBan());
                    ps.setString(6, m.getMoTa());
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        m.setMaMon(nextId);
                        return true;
                    } else {
                        return false;
                    }
                }
            } finally {
                // Always unlock tables even on exception
                try { stmt.execute(unlockSql); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // Helper to get current max maMon inside provided connection (kept for compatibility)
    private int getMaxMaMon(Connection c) throws SQLException {
        String sql = "SELECT COALESCE(MAX(maMon), 0) AS mx FROM mon";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("mx");
        }
        return 0;
    }

    public boolean capNhat(Mon m) {
        // Validate: giaBan > giaNhap in TonKho (if exists)
        String validationMsg = validateGiaBan(m.getMaMon(), m.getGiaBan());
        if (validationMsg != null) {
            System.err.println("Validation error: " + validationMsg);
            return false;
        }

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

    public boolean xoa(int maMon) {
        String sql = "DELETE FROM mon WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    // Check whether an entry with given id exists
    public boolean kiemTraMa(int maMon) {
        String sql = "SELECT 1 FROM mon WHERE maMon = ? LIMIT 1";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    // Validate that giaBan > giaNhap in TonKho (if TonKho exists for this product)
    private String validateGiaBan(int maMon, java.math.BigDecimal giaBan) {
        String sql = "SELECT giaNhap FROM tonKho WHERE maMon = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal giaNhap = rs.getBigDecimal("giaNhap");
                    if (giaBan != null && giaNhap != null && giaBan.compareTo(giaNhap) <= 0) {
                        return "Giá bán phải lớn hơn giá nhập (" + giaNhap + ")";
                    }
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }
}
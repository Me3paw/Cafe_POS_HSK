package dao;

import connectDB.DBConnection;
import entity.Ban;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BanDAO {
    public List<Ban> layHet() {
        List<Ban> res = new ArrayList<>();
        String sql = "SELECT maBan, maDonHang, trangThai, soNguoi, capNhatCuoi FROM ban";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public Ban layTheoId(int maBan) {
        String sql = "SELECT maBan, maDonHang, trangThai, soNguoi, capNhatCuoi FROM ban WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maBan);
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

    public boolean them(Ban t) {
        String sql = "INSERT INTO ban(maban, maDonHang, trangThai, soNguoi, capNhatCuoi) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getMaBan());
            setNullableInt(ps, 2, t.getMaDonHang());
            ps.setString(3, toDbStatus(t.getTrangThai()));
            setNullableInt(ps, 4, t.getSoNguoi());
            if (t.getCapNhatCuoi() != null) ps.setTimestamp(5, t.getCapNhatCuoi());
            else ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean capNhat(Ban t) {
        String sql = "UPDATE ban SET maDonHang = ?, trangThai = ?, soNguoi = ?, capNhatCuoi = ? WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setNullableInt(ps, 1, t.getMaDonHang());
            ps.setString(2, toDbStatus(t.getTrangThai()));
            setNullableInt(ps, 3, t.getSoNguoi());
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setInt(5, t.getMaBan());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Initialize the Ban table from a list of CafeTable definitions.
     * Behavior: if the table already contains rows, do nothing. If empty, insert one row per CafeTable
     * using the integer maBan of the CafeTable. maDonHang = NULL, trangThai = 'TRONG', soNguoi = NULL, capNhatCuoi = NOW().
     */
    public void khoiTao(List<components.GiaoDienKhuVucBan.CafeTable> tables) {
        if (tables == null || tables.isEmpty()) return;
        String countSql = "SELECT COUNT(*) FROM ban";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(countSql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int cnt = rs.getInt(1);
                if (cnt > 0) return; // already initialized
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        // Table is empty: insert rows for integer maBan values (>0)
        String insertSql = "INSERT INTO ban(maBan, maDonHang, trangThai, soNguoi, capNhatCuoi) VALUES(?, NULL, ?, NULL, NOW())";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(insertSql)) {
            for (components.GiaoDienKhuVucBan.CafeTable t : tables) {
                if (t == null) continue;
                if (t.maBan <= 0) continue;
                ps.setInt(1, t.maBan);
                String initialStatus = t.status != null ? t.status : (t.isTakeaway ? "TAKEAWAY" : "Trống");
                ps.setString(2, toDbStatus(initialStatus));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean delete(int maBan) {
        String sql = "DELETE FROM ban WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maBan);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private Ban mapRow(ResultSet rs) throws SQLException {
        Ban t = new Ban();
        t.setMaBan(rs.getInt("maBan"));
        int maDonHang = rs.getInt("maDonHang");
        t.setMaDonHang(rs.wasNull() ? null : maDonHang);
        // Keep status in English (database ENUM format), don't convert to Vietnamese here
        t.setTrangThai(rs.getString("trangThai"));
        int soNguoi = rs.getInt("soNguoi");
        t.setSoNguoi(rs.wasNull() ? null : soNguoi);
        t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
        return t;
    }

    private String toDbStatus(String status) {
        if (status == null) return null;
        String trimmed = status.trim();
        if (trimmed.isEmpty()) return null;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "trống":
            case "trong":
            case "free":
            case "available":
                return "FREE";
            case "đang sử dụng":
            case "dang su dung":
            case "occupied":
                return "OCCUPIED";
            case "đặt trước":
            case "dat truoc":
            case "reserved":
                return "RESERVED";
            case "bảo trì":
            case "bao tri":
            case "maintenance":
                return "MAINTENANCE";
            case "takeaway":
            case "mang di":
                return "TAKEAWAY";
            default:
                String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                if (normalized.length() > 50) normalized = normalized.substring(0, 50);
                return normalized.toUpperCase(Locale.ROOT);
        }
    }

    private String fromDbStatus(String dbStatus) {
        if (dbStatus == null) return null;
        switch (dbStatus.toUpperCase(Locale.ROOT)) {
            case "FREE":
                return "Trống";
            case "OCCUPIED":
                return "Đang sử dụng";
            case "RESERVED":
                return "Đặt trước";
            case "MAINTENANCE":
                return "Bảo trì";
            case "TAKEAWAY":
                return "Mang đi";
            default:
                return dbStatus;
        }
    }
}

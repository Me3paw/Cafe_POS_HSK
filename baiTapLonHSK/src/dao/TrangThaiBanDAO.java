package dao;

import connectDB.DBConnection;
import entity.TrangThaiBan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrangThaiBanDAO {
    public List<TrangThaiBan> getAll() {
        List<TrangThaiBan> res = new ArrayList<>();
        String sql = "SELECT maBan, maDonHang, trangThai, soNguoi, capNhatCuoi FROM trangThaiBan";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public TrangThaiBan getById(int maBan) {
        String sql = "SELECT maBan, maDonHang, trangThai, soNguoi, capNhatCuoi FROM trangThaiBan WHERE maBan = ?";
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

    public boolean insert(TrangThaiBan t) {
        String sql = "INSERT INTO trangThaiBan(maBan, maDonHang, trangThai, soNguoi, capNhatCuoi) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getMaBan());
            setNullableInt(ps, 2, t.getMaDonHang());
            ps.setString(3, t.getTrangThai());
            setNullableInt(ps, 4, t.getSoNguoi());
            if (t.getCapNhatCuoi() != null) ps.setTimestamp(5, t.getCapNhatCuoi());
            else ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(TrangThaiBan t) {
        String sql = "UPDATE trangThaiBan SET maDonHang = ?, trangThai = ?, soNguoi = ?, capNhatCuoi = ? WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setNullableInt(ps, 1, t.getMaDonHang());
            ps.setString(2, t.getTrangThai());
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
     * Initialize the trangThaiBan table from a list of CafeTable definitions.
     * Behavior: if the table already contains rows, do nothing. If empty, insert one row per CafeTable
     * using the integer maBan of the CafeTable. maDonHang = NULL, trangThai = 'TRONG', soNguoi = NULL, capNhatCuoi = NOW().
     */
    public void initFromCafeTables(List<components.TableLayoutPanel.CafeTable> tables) {
        if (tables == null || tables.isEmpty()) return;
        String countSql = "SELECT COUNT(*) FROM trangThaiBan";
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
        String insertSql = "INSERT INTO trangThaiBan(maBan, maDonHang, trangThai, soNguoi, capNhatCuoi) VALUES(?, NULL, 'trong', NULL, NOW())";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(insertSql)) {
            for (components.TableLayoutPanel.CafeTable t : tables) {
                if (t == null) continue;
                // only use positive integer ids; skip takeaway (maBan==0)
                if (t.maBan <= 0) continue;
                ps.setInt(1, t.maBan);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean delete(int maBan) {
        String sql = "DELETE FROM trangThaiBan WHERE maBan = ?";
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

    private TrangThaiBan mapRow(ResultSet rs) throws SQLException {
        TrangThaiBan t = new TrangThaiBan();
        t.setMaBan(rs.getInt("maBan"));
        int maDonHang = rs.getInt("maDonHang");
        t.setMaDonHang(rs.wasNull() ? null : maDonHang);
        t.setTrangThai(rs.getString("trangThai"));
        int soNguoi = rs.getInt("soNguoi");
        t.setSoNguoi(rs.wasNull() ? null : soNguoi);
        t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
        return t;
    }
}
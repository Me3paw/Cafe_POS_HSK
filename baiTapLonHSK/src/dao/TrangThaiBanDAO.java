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
                TrangThaiBan t = new TrangThaiBan();
                t.setMaBan(rs.getString("maBan"));
                t.setMaDonHang(rs.getString("maDonHang"));
                t.setTrangThai(rs.getString("trangThai"));
                t.setSoNguoi(rs.getObject("soNguoi", Integer.class));
                t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
                res.add(t);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public TrangThaiBan getById(String maBan) {
        String sql = "SELECT maBan, maDonHang, trangThai, soNguoi, capNhatCuoi FROM trangThaiBan WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maBan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TrangThaiBan t = new TrangThaiBan();
                    t.setMaBan(rs.getString("maBan"));
                    t.setMaDonHang(rs.getString("maDonHang"));
                    t.setTrangThai(rs.getString("trangThai"));
                    t.setSoNguoi(rs.getObject("soNguoi", Integer.class));
                    t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
                    return t;
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
            ps.setString(1, t.getMaBan());
            // maDonHang may be provided; otherwise keep NULL
            if (t.getMaDonHang() != null) ps.setString(2, t.getMaDonHang());
            else ps.setObject(2, null, Types.VARCHAR);
            ps.setString(3, t.getTrangThai());
            if (t.getSoNguoi() != null) ps.setObject(4, t.getSoNguoi(), Types.INTEGER);
            else ps.setObject(4, null, Types.INTEGER);
            if (t.getCapNhatCuoi() != null) ps.setTimestamp(5, t.getCapNhatCuoi());
            else ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update(TrangThaiBan t) {
        String sql = "UPDATE trangThaiBan SET trangThai = ?, soNguoi = ?, capNhatCuoi = ? WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTrangThai());
            if (t.getSoNguoi() != null) ps.setObject(2, t.getSoNguoi(), Types.INTEGER);
            else ps.setObject(2, null, Types.INTEGER);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, t.getMaBan());
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
        String insertSql = "INSERT INTO trangThaiBan(maBan, maDonHang, trangThai, soNguoi, capNhatCuoi) VALUES(?, NULL, 'TRONG', NULL, NOW())";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(insertSql)) {
            for (components.TableLayoutPanel.CafeTable t : tables) {
                if (t == null) continue;
                // only use positive integer ids; skip takeaway (maBan==0)
                if (t.maBan <= 0) continue;
                ps.setString(1, String.valueOf(t.maBan));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean delete(String maBan) {
        String sql = "DELETE FROM trangThaiBan WHERE maBan = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maBan);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
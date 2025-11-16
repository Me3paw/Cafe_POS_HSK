package dao;

import connectDB.DBConnection;
import entity.GiamGia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GiamGiaDAO {
    public List<GiamGia> layHet() {
        List<GiamGia> res = new ArrayList<>();
        String sql = "SELECT maGiamGia, tenChuongTrinh, loai, giaTri, hangApDung, ngayBatDau, ngayKetThuc, dangApDung FROM giamGia";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(themHang(rs));
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public GiamGia layTheoId(int maGiamGia) {
        String sql = "SELECT maGiamGia, tenChuongTrinh, loai, giaTri, hangApDung, ngayBatDau, ngayKetThuc, dangApDung FROM giamGia WHERE maGiamGia = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maGiamGia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return themHang(rs);
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean them(GiamGia g) {
        String sql = "INSERT INTO giamGia(tenChuongTrinh, loai, giaTri, hangApDung, ngayBatDau, ngayKetThuc, dangApDung) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getTenChuongTrinh());
            ps.setString(2, g.getLoai());
            ps.setBigDecimal(3, g.getGiaTri());
            ps.setString(4, g.getHangApDung());
            if (g.getNgayBatDau() != null) {
                ps.setDate(5, Date.valueOf(g.getNgayBatDau()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            if (g.getNgayKetThuc() != null) {
                ps.setDate(6, Date.valueOf(g.getNgayKetThuc()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setBoolean(7, g.isDangApDung());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        g.setMaGiamGia(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean capNhat(GiamGia g) {
        String sql = "UPDATE giamGia SET tenChuongTrinh = ?, loai = ?, giaTri = ?, hangApDung = ?, ngayBatDau = ?, ngayKetThuc = ?, dangApDung = ? WHERE maGiamGia = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getTenChuongTrinh());
            ps.setString(2, g.getLoai());
            ps.setBigDecimal(3, g.getGiaTri());
            ps.setString(4, g.getHangApDung());
            if (g.getNgayBatDau() != null) {
                ps.setDate(5, Date.valueOf(g.getNgayBatDau()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            if (g.getNgayKetThuc() != null) {
                ps.setDate(6, Date.valueOf(g.getNgayKetThuc()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setBoolean(7, g.isDangApDung());
            ps.setInt(8, g.getMaGiamGia());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean xoa(int maGiamGia) {
        String sql = "DELETE FROM giamGia WHERE maGiamGia = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maGiamGia);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    private GiamGia themHang(ResultSet rs) throws SQLException {
        GiamGia g = new GiamGia();
        g.setMaGiamGia(rs.getInt("maGiamGia"));
        g.setTenChuongTrinh(rs.getString("tenChuongTrinh"));
        g.setLoai(rs.getString("loai"));
        g.setGiaTri(rs.getBigDecimal("giaTri"));
        g.setHangApDung(rs.getString("hangApDung"));
        Date bd = rs.getDate("ngayBatDau");
        g.setNgayBatDau(bd != null ? bd.toLocalDate() : null);
        Date kt = rs.getDate("ngayKetThuc");
        g.setNgayKetThuc(kt != null ? kt.toLocalDate() : null);
        g.setDangApDung(rs.getBoolean("dangApDung"));
        return g;
    }
}

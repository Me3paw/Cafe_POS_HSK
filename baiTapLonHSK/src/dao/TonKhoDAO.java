package dao;

import connectDB.DBConnection;
import entity.TonKho;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TonKhoDAO {
    public List<TonKho> layHet() {
        List<TonKho> res = new ArrayList<>();
        String sql = "SELECT maTon, maMon, donVi, soLuong, giaNhap, mucCanhBao, capNhatCuoi FROM tonKho";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TonKho t = new TonKho();
                t.setMaTon(rs.getInt("maTon"));
                t.setMaMon(rs.getInt("maMon"));
                t.setDonVi(rs.getString("donVi"));
                t.setSoLuong(rs.getBigDecimal("soLuong"));
                t.setGiaNhap(rs.getBigDecimal("giaNhap"));
                t.setMucCanhBao(rs.getBigDecimal("mucCanhBao"));
                t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
                res.add(t);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return res;
    }

    public TonKho layTheoMaMon(int maMon) {
        String sql = "SELECT maTon, maMon, donVi, soLuong, giaNhap, mucCanhBao, capNhatCuoi FROM tonKho WHERE maMon = ? LIMIT 1";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maMon);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TonKho t = new TonKho();
                    t.setMaTon(rs.getInt("maTon"));
                    t.setMaMon(rs.getInt("maMon"));
                    t.setDonVi(rs.getString("donVi"));
                    t.setSoLuong(rs.getBigDecimal("soLuong"));
                    t.setGiaNhap(rs.getBigDecimal("giaNhap"));
                    t.setMucCanhBao(rs.getBigDecimal("mucCanhBao"));
                    t.setCapNhatCuoi(rs.getTimestamp("capNhatCuoi"));
                    return t;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    public boolean capNhat(TonKho t) {
        // Now includes capNhatCuoi
        String sql = "UPDATE tonKho SET donVi = ?, soLuong = ?, giaNhap = ?, mucCanhBao = ?, capNhatCuoi = ? WHERE maTon = ?";
        String updateMonSql = "UPDATE mon SET conBan = ? WHERE maMon = ?";

        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = DBConnection.getConnection();
            c.setAutoCommit(false);

            // Recalculate mức cảnh báo
            BigDecimal mucCanhBao = calculateMucCanhBao(t.getSoLuong());

            ps = c.prepareStatement(sql);
            ps.setString(1, t.getDonVi());
            ps.setBigDecimal(2, t.getSoLuong() != null ? t.getSoLuong() : BigDecimal.ZERO);
            ps.setBigDecimal(3, t.getGiaNhap() != null ? t.getGiaNhap() : BigDecimal.ZERO);
            ps.setBigDecimal(4, mucCanhBao);

            // PATCH: write timestamp to DB
            ps.setTimestamp(5, t.getCapNhatCuoi());

            ps.setInt(6, t.getMaTon());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                // If quantity <= 0, disable product
                if (t.getSoLuong() == null || t.getSoLuong().compareTo(BigDecimal.ZERO) <= 0) {
                    try (PreparedStatement ps2 = c.prepareStatement(updateMonSql)) {
                        ps2.setBoolean(1, false);
                        ps2.setInt(2, t.getMaMon());
                        ps2.executeUpdate();
                    }
                }

                c.commit();
                return true;

            } else {
                c.rollback();
                return false;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            if (c != null) {
                try { c.rollback(); } catch (SQLException ignored) {}
            }
            return false;

        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) {}
        }
    }


    private BigDecimal calculateMucCanhBao(BigDecimal soLuong) {
        // mucCanhBao = 0 if soLuong > 100
        //            = 1 if soLuong > 50
        //            = 2 if soLuong > 10
        //            = 3 if soLuong 1-10
        //            = 4 if soLuong = 0
        if (soLuong == null || soLuong.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal(4);
        } else if (soLuong.compareTo(new BigDecimal(10)) <= 0) {
            return new BigDecimal(3);
        } else if (soLuong.compareTo(new BigDecimal(50)) <= 0) {
            return new BigDecimal(2);
        } else if (soLuong.compareTo(new BigDecimal(100)) <= 0) {
            return new BigDecimal(1);
        } else {
            return new BigDecimal(0);
        }
    }
}
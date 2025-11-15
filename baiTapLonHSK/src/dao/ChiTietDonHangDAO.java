package dao;

import connectDB.DBConnection;
import entity.ChiTietDonHang;
import entity.Mon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDonHangDAO {
    public List<ChiTietDonHang> layHet() {
        List<ChiTietDonHang> res = new ArrayList<>();
        String sql = "SELECT maChiTiet, maDonHang, maMon, soLuong, giaBan, thanhTien, maThue, tienThue FROM chiTietDonHang";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ChiTietDonHang layTheoId(int maChiTiet) {
        String sql = "SELECT maChiTiet, maDonHang, maMon, soLuong, giaBan, thanhTien, maThue, tienThue FROM chiTietDonHang WHERE maChiTiet = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maChiTiet);
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

    public boolean them(ChiTietDonHang ct) {
        String sql = "INSERT INTO chiTietDonHang(maDonHang, maMon, soLuong, giaBan, thanhTien, maThue, tienThue) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ct.getMaDonHang());
            ps.setInt(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setBigDecimal(4, ct.getGiaBan());
            ps.setBigDecimal(5, ct.getThanhTien());
            setNullableInt(ps, 6, ct.getMaThue());
            ps.setBigDecimal(7, ct.getTienThue());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) ct.setMaChiTiet(keys.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean capNhat(ChiTietDonHang ct) {
        String sql = "UPDATE chiTietDonHang SET maDonHang = ?, maMon = ?, soLuong = ?, giaBan = ?, thanhTien = ?, maThue = ?, tienThue = ? WHERE maChiTiet = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ct.getMaDonHang());
            ps.setInt(2, ct.getMaMon());
            ps.setInt(3, ct.getSoLuong());
            ps.setBigDecimal(4, ct.getGiaBan());
            ps.setBigDecimal(5, ct.getThanhTien());
            setNullableInt(ps, 6, ct.getMaThue());
            ps.setBigDecimal(7, ct.getTienThue());
            ps.setInt(8, ct.getMaChiTiet());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean xoa(int maChiTiet) {
        String sql = "DELETE FROM chiTietDonHang WHERE maChiTiet = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maChiTiet);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<ChiTietDonHang> layTheoDonHang(int maDonHang) {
        List<ChiTietDonHang> res = new ArrayList<>();
        String sql = "SELECT ct.maChiTiet, ct.maDonHang, ct.maMon, ct.soLuong, ct.giaBan, ct.thanhTien, ct.maThue, ct.tienThue, " +
                "m.tenMon AS monTen, m.giaBan AS monGia, m.moTa AS monMoTa, m.conBan AS monConBan " +
                "FROM chiTietDonHang ct " +
                "LEFT JOIN mon m ON ct.maMon = m.maMon " +
                "WHERE ct.maDonHang = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maDonHang);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietDonHang ct = mapRow(rs);

                    int maMon = rs.getInt("maMon");
                    if (!rs.wasNull()) {
                        Mon mon = ct.getMon();
                        if (mon == null) {
                            mon = new Mon();
                            ct.setMon(mon);
                        }
                        mon.setMaMon(maMon);
                        mon.setTenMon(rs.getString("monTen"));
                        mon.setGiaBan(rs.getBigDecimal("monGia"));
                        mon.setMoTa(rs.getString("monMoTa"));
                        mon.setConBan(rs.getBoolean("monConBan"));
                    }

                    res.add(ct);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    private ChiTietDonHang mapRow(ResultSet rs) throws SQLException {
        ChiTietDonHang ct = new ChiTietDonHang();
        ct.setMaChiTiet(rs.getInt("maChiTiet"));
        ct.setMaDonHang(rs.getInt("maDonHang"));
        ct.setMaMon(rs.getInt("maMon"));
        ct.setSoLuong(rs.getInt("soLuong"));
        ct.setGiaBan(rs.getBigDecimal("giaBan"));
        ct.setThanhTien(rs.getBigDecimal("thanhTien"));
        int maThue = rs.getInt("maThue");
        ct.setMaThue(rs.wasNull() ? null : maThue);
        ct.setTienThue(rs.getBigDecimal("tienThue"));
        return ct;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }
}

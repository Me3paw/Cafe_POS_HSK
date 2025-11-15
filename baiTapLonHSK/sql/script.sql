CREATE DATABASE IF NOT EXISTS cafe_pos
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;
USE cafe_pos;

-- ==========================
-- BẢNG nguoiDung
-- ==========================
CREATE TABLE nguoiDung (
    maNguoiDung INT AUTO_INCREMENT PRIMARY KEY,
    tenDangNhap VARCHAR(255),
    hoTen VARCHAR(255),
    vaiTro VARCHAR(50),
    matKhau VARCHAR(255),
    ngayTao DATETIME
);

-- ==========================
-- BẢNG caLam
-- ==========================
CREATE TABLE caLam (
    maCa INT AUTO_INCREMENT PRIMARY KEY,
    maNguoiDung INT,
    batDau DATETIME,
    ketThuc DATETIME,
    tongGio DECIMAL(10,2),
    trangThai VARCHAR(50),
    FOREIGN KEY (maNguoiDung) REFERENCES nguoiDung(maNguoiDung)
);

-- ==========================
-- BẢNG khachHang
-- ==========================
CREATE TABLE khachHang (
    maKhachHang INT AUTO_INCREMENT PRIMARY KEY,
    hoTen VARCHAR(255),
    soDienThoai VARCHAR(50),
    hangThanhVien VARCHAR(50),
    ngayTao DATETIME
);

-- ==========================
-- BẢNG danhMuc
-- ==========================
CREATE TABLE danhMuc (
    maDanhMuc INT AUTO_INCREMENT PRIMARY KEY,
    tenDanhMuc VARCHAR(255),
    moTa TEXT
);

-- ==========================
-- BẢNG mon
-- ==========================
CREATE TABLE mon (
    maMon INT AUTO_INCREMENT PRIMARY KEY,
    maDanhMuc INT,
    tenMon VARCHAR(255),
    giaBan DECIMAL(10,2),
    conBan BOOLEAN,
    moTa TEXT,
    FOREIGN KEY (maDanhMuc) REFERENCES danhMuc(maDanhMuc)
);

-- ==========================
-- BẢNG thue
-- ==========================
CREATE TABLE thue (
    maThue INT AUTO_INCREMENT PRIMARY KEY,
    tenThue VARCHAR(255),
    tyLe DECIMAL(10,2),
    dangApDung BOOLEAN
);

-- ==========================
-- BẢNG giamGia
-- ==========================
CREATE TABLE giamGia (
    maGiamGia INT AUTO_INCREMENT PRIMARY KEY,
    tenChuongTrinh VARCHAR(255),
    loai VARCHAR(50),
    giaTri DECIMAL(10,2),
    hangApDung VARCHAR(50),
    ngayBatDau DATE,
    ngayKetThuc DATE,
    dangApDung BOOLEAN
);

-- ==========================
-- BẢNG donHang
-- ==========================
CREATE TABLE donHang (
    maDonHang INT AUTO_INCREMENT PRIMARY KEY,
    maNguoiDung INT,
    maCa INT,
    maKhachHang INT,
    maGiamGia INT,
    tongTien DECIMAL(10,2),
    tienGiam DECIMAL(10,2),
    tienThue DECIMAL(10,2),
    tongCuoi DECIMAL(10,2),
    trangThai VARCHAR(50),
    loaiDon VARCHAR(50),
    thoiGianTao DATETIME,
    maBan INT,
    FOREIGN KEY (maNguoiDung) REFERENCES nguoiDung(maNguoiDung),
    FOREIGN KEY (maCa) REFERENCES caLam(maCa),
    FOREIGN KEY (maKhachHang) REFERENCES khachHang(maKhachHang),
    FOREIGN KEY (maGiamGia) REFERENCES giamGia(maGiamGia)
);

-- ==========================
-- BẢNG chiTietDonHang
-- ==========================
CREATE TABLE chiTietDonHang (
    maChiTiet INT AUTO_INCREMENT PRIMARY KEY,
    maDonHang INT,
    maMon INT,
    soLuong INT,
    giaBan DECIMAL(10,2),
    thanhTien DECIMAL(10,2),
    maThue INT,
    tienThue DECIMAL(10,2),
    FOREIGN KEY (maDonHang) REFERENCES donHang(maDonHang),
    FOREIGN KEY (maMon) REFERENCES mon(maMon),
    FOREIGN KEY (maThue) REFERENCES thue(maThue)
);

-- ==========================
-- BẢNG thanhToan
-- ==========================
CREATE TABLE thanhToan (
    maThanhToan INT AUTO_INCREMENT PRIMARY KEY,
    maDonHang INT,
    hinhThuc VARCHAR(50),
    soTien DECIMAL(10,2),
    thoiGian DATETIME,
    FOREIGN KEY (maDonHang) REFERENCES donHang(maDonHang)
);

-- ==========================
-- BẢNG trangThaiBan
-- ==========================
CREATE TABLE trangThaiBan (
    maBan INT PRIMARY KEY,
    maDonHang INT,
    trangThai VARCHAR(50),
    soNguoi INT,
    capNhatCuoi DATETIME,
    FOREIGN KEY (maDonHang) REFERENCES donHang(maDonHang)
);

-- Tham chiếu ngược từ donHang → trangThaiBan
ALTER TABLE donHang 
    ADD CONSTRAINT fk_dh_maban
    FOREIGN KEY (maBan) REFERENCES trangThaiBan(maBan);

-- ==========================
-- BẢNG tonKho
-- ==========================
CREATE TABLE tonKho (
    maNguyenLieu INT AUTO_INCREMENT PRIMARY KEY,
    tenNguyenLieu VARCHAR(255),
    donVi VARCHAR(50),
    soLuong DECIMAL(10,2),
    giaNhap DECIMAL(10,2),
    mucCanhBao DECIMAL(10,2),
    capNhatCuoi DATETIME
);

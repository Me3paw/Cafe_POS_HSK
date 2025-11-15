CREATE DATABASE IF NOT EXISTS cafe_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cafe_pos;

-- Bảng nhân viên
CREATE TABLE nguoiDung (
    maNguoiDung INT AUTO_INCREMENT PRIMARY KEY,
    tenDangNhap VARCHAR(100) UNIQUE NOT NULL,
    hoTen VARCHAR(100) NOT NULL,
    vaiTro ENUM('thuNgan', 'phaChe', 'quanLy') NOT NULL,
    matKhau VARCHAR(255) NOT NULL,
    ngayTao DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Bảng ca làm việc
CREATE TABLE caLam (
    maCa INT AUTO_INCREMENT PRIMARY KEY,
    maNguoiDung INT NOT NULL,
    batDau DATETIME NOT NULL,
    ketThuc DATETIME,
    tongGio DECIMAL(5,2),
    trangThai ENUM('dangMo', 'daDong') DEFAULT 'dangMo',
    FOREIGN KEY (maNguoiDung) REFERENCES nguoiDung(maNguoiDung)
);

-- Bảng khách hàng
CREATE TABLE khachHang (
    maKhachHang INT AUTO_INCREMENT PRIMARY KEY,
    hoTen VARCHAR(100),
    soDienThoai VARCHAR(15) NOT NULL,
    hangThanhVien ENUM('dong', 'bac', 'vang', 'bachKim') DEFAULT 'dong',
    ngayTao DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Bảng danh mục món
CREATE TABLE danhMuc (
    maDanhMuc INT AUTO_INCREMENT PRIMARY KEY,
    tenDanhMuc VARCHAR(100) NOT NULL,
    moTa TEXT
);

-- Bảng món
CREATE TABLE mon (
    maMon INT AUTO_INCREMENT PRIMARY KEY,
    maDanhMuc INT,
    tenMon VARCHAR(100) NOT NULL,
    giaBan DECIMAL(8,2) NOT NULL,
    conBan BOOLEAN DEFAULT TRUE,
    moTa TEXT,
    FOREIGN KEY (maDanhMuc) REFERENCES danhMuc(maDanhMuc)
);

-- Bảng thuế
CREATE TABLE thue (
    maThue INT AUTO_INCREMENT PRIMARY KEY,
    tenThue VARCHAR(100),
    tyLe DECIMAL(5,2) NOT NULL,
    dangApDung BOOLEAN DEFAULT TRUE
);

-- Bảng giảm giá
CREATE TABLE giamGia (
    maGiamGia INT AUTO_INCREMENT PRIMARY KEY,
    tenChuongTrinh VARCHAR(100),
    loai ENUM('phanTram', 'coDinh') NOT NULL,
    giaTri DECIMAL(8,2) NOT NULL,
    hangApDung ENUM('dong', 'bac', 'vang', 'bachKim'),
    ngayBatDau DATE,
    ngayKetThuc DATE,
    dangApDung BOOLEAN DEFAULT TRUE
);

-- Bảng đơn hàng
CREATE TABLE donHang (
    maDonHang INT AUTO_INCREMENT PRIMARY KEY,
    maNguoiDung INT,
    maCa INT,
    maKhachHang INT,
    maGiamGia INT,
    tongTien DECIMAL(10,2) NOT NULL,
    tienGiam DECIMAL(10,2) DEFAULT 0,
    tienThue DECIMAL(10,2) DEFAULT 0,
    tongCuoi DECIMAL(10,2) NOT NULL,
    trangThai ENUM('dangMo', 'daThanhToan', 'huy') DEFAULT 'dangMo',
    loaiDon ENUM('taiCho', 'mangVe', 'giaoHang') DEFAULT 'taiCho',
    thoiGianTao DATETIME DEFAULT CURRENT_TIMESTAMP,
    maBan INT NOT NULL,
    FOREIGN KEY (maNguoiDung) REFERENCES nguoiDung(maNguoiDung),
    FOREIGN KEY (maCa) REFERENCES caLam(maCa),
    FOREIGN KEY (maKhachHang) REFERENCES khachHang(maKhachHang),
    FOREIGN KEY (maGiamGia) REFERENCES giamGia(maGiamGia)
);

-- Trạng thái bàn
CREATE TABLE trangThaiBan (
    maBan INT PRIMARY KEY,
    maDonHang INT NULL,
    trangThai ENUM('trong', 'dangPhucVu', 'choThanhToan', 'daDong') DEFAULT 'trong',
    soNguoi INT NULL,
    capNhatCuoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (maDonHang) REFERENCES donHang(maDonHang)
);

-- Liên kết donHang ⇄ trangThaiBan
ALTER TABLE donHang
    ADD CONSTRAINT fk_donhang_maban
        FOREIGN KEY (maBan) REFERENCES trangThaiBan(maBan);

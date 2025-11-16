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
        FOREIGN KEY (maBan) REFERENCES ban(maBan);

SHOW CREATE TABLE trangThaiBan;
ALTER TABLE ban ban 
MODIFY trangThai ENUM('FREE','OCCUPIED','RESERVED','MAINTENANCE','TAKEAWAY');
ALTER TABLE tonKho
    CHANGE COLUMN maNguyenLieu maTon INT NOT NULL AUTO_INCREMENT,
    DROP COLUMN tenNguyenLieu,
    ADD COLUMN maMon INT NOT NULL UNIQUE AFTER maTon,
    ADD CONSTRAINT fk_tonkho_mon FOREIGN KEY (maMon) REFERENCES mon(maMon);

INSERT INTO nguoiDung (tenDangNhap, hoTen, vaiTro, matKhau)
VALUES
('admin', 'Quản Lý Chính', 'quanLy', '123456'),
('phaChe1', 'Pha Chế A', 'phaChe', '123456'),
('thuNgan1', 'Thu Ngân A', 'thuNgan', '123456');
INSERT INTO caLam (maCa, maNguoiDung, batDau, ketThuc, tongGio, trangThai)
VALUES
(1, 1, NOW() - INTERVAL 6 HOUR, NOW(), 6.00, 'daDong'),
(2, 2, NOW() - INTERVAL 3 HOUR, NULL, NULL, 'dangMo');
ALTER TABLE caLam AUTO_INCREMENT = 3;
INSERT INTO khachHang (hoTen, soDienThoai, hangThanhVien)
VALUES
('Nguyễn Văn A', '0901111111', 'dong'),
('Trần Thị B', '0902222222', 'bac'),
('Lê Văn C', '0903333333', 'vang'),
('Phạm Thị D', '0904444444', 'dong'),
('Hoàng Văn E', '0905555555', 'bachKim');
INSERT INTO danhMuc (tenDanhMuc, moTa)
VALUES
('Cà phê', 'Các loại cà phê'),
('Trà', 'Trà sữa, trà trái cây'),
('Đá xay', 'Sinh tố, frappe'),
('Nước đóng chai', 'Nước ngọt, nước suối');
INSERT INTO mon (maDanhMuc, tenMon, giaBan, conBan, moTa) VALUES
(1, 'Cà phê đen', 25000, 1, ''),
(1, 'Cà phê sữa', 30000, 1, ''),
(1, 'Bạc xỉu', 35000, 1, ''),
(1, 'Latte', 40000, 1, ''),
(1, 'Espresso', 30000, 1, ''),

(2, 'Trà đào cam sả', 45000, 1, ''),
(2, 'Trà chanh', 30000, 1, ''),
(2, 'Trà sữa truyền thống', 40000, 1, ''),
(2, 'Hồng trà matcha', 45000, 1, ''),
(2, 'Trà vải', 35000, 1, ''),

(3, 'Nước suối', 15000, 1, ''),
(3, 'Coca-Cola', 20000, 1, ''),
(3, 'Pepsi', 20000, 1, ''),
(3, 'Sting', 20000, 1, ''),
(3, '7Up', 20000, 1, ''),

(4, 'Matcha đá xay', 55000, 1, ''),
(4, 'Cookie đá xay', 55000, 1, ''),
(4, 'Sinh tố xoài', 50000, 1, ''),
(4, 'Sinh tố bơ', 55000, 1, ''),
(4, 'Chocolate đá xay', 55000, 1, '');
INSERT INTO thue (tenThue, tyLe, dangApDung)
VALUES ('VAT', 10.00, 1);
INSERT INTO giamGia (tenChuongTrinh, loai, giaTri, hangApDung, ngayBatDau, ngayKetThuc, dangApDung)
VALUES
('Giảm 5% thành viên bạc', 'phanTram', 5, 'bac', CURDATE(), CURDATE() + INTERVAL 30 DAY, 1);
INSERT INTO donHang
(maNguoiDung, maCa, maKhachHang, maGiamGia, tongTien, tienGiam, tienThue, tongCuoi, trangThai, loaiDon, thoiGianTao, maBan)
VALUES
(1, 1, 1, NULL, 30000, 0, 3000, 33000, 'daThanhToan', 'taiCho', NOW(), 2),
(2, 2, 2, 1, 85000, 4250, 8075, 88825, 'daThanhToan', 'taiCho', NOW(), 5),
(3, 1, NULL, NULL, 45000, 0, 4500, 49500, 'daThanhToan', 'mangVe', NOW(), 1),
(1, 1, 3, NULL, 60000, 0, 6000, 66000, 'dangMo', 'taiCho', NOW(), 3),
(2, 2, 4, NULL, 20000, 0, 2000, 22000, 'daThanhToan', 'taiCho', NOW(), 4),
(1, 1, 5, NULL, 75000, 0, 7500, 82500, 'daThanhToan', 'mangVe', NOW(), 6),
(2, 2, NULL, NULL, 55000, 0, 5500, 60500, 'daThanhToan', 'taiCho', NOW(), 7),
(3, 1, NULL, NULL, 50000, 0, 5000, 55000, 'huy', 'giaoHang', NOW(), 8),
(1, 1, 3, NULL, 35000, 0, 3500, 38500, 'daThanhToan', 'taiCho', NOW(), 9),
(2, 2, 2, NULL, 40000, 0, 4000, 44000, 'daThanhToan', 'taiCho', NOW(), 10);

INSERT INTO chiTietDonHang (maDonHang, maMon, soLuong, giaBan, thanhTien, maThue, tienThue)
VALUES
(1, 1, 1, 30000, 30000, 1, 3000),
(2, 6, 1, 45000, 45000, 1, 4500),
(2, 2, 1, 30000, 30000, 1, 3000),
(3, 10, 1, 35000, 35000, 1, 3500),
(4, 4, 1, 40000, 40000, 1, 4000),
(4, 3, 1, 35000, 35000, 1, 3500),
(5, 11, 1, 15000, 15000, 1, 1500),
(6, 17, 1, 55000, 55000, 1, 5500),
(7, 2, 2, 30000, 60000, 1, 6000),
(8, 14, 1, 20000, 20000, 1, 2000),
(9, 8, 1, 40000, 40000, 1, 4000),
(10, 7, 1, 30000, 30000, 1, 3000);
SELECT * FROM donHang;
INSERT INTO thanhToan (maDonHang, hinhThuc, soTien)
VALUES
(1, 'tienMat', 33000),
(2, 'the', 88825),
(3, 'tienMat', 49500),
(5, 'tienMat', 22000),
(6, 'viDienTu', 82500),
(7, 'tienMat', 60500),
(9, 'tienMat', 38500),
(10, 'tienMat', 44000);
INSERT INTO tonKho (maMon, donVi, soLuong, giaNhap, mucCanhBao)
VALUES
(1,'kg',5,100000,1),
(2,'kg',5,100000,1),
(3,'kg',4,120000,1),
(4,'kg',3,150000,1),
(5,'kg',3,150000,1),

(6,'L',10,40000,2),
(7,'L',10,30000,2),
(8,'L',10,35000,2),
(9,'L',8,35000,2),
(10,'L',10,30000,2),

(11,'chai',50,5000,10),
(12,'chai',40,6000,10),
(13,'chai',35,6000,10),
(14,'chai',30,7000,10),
(15,'chai',30,7000,10),

(16,'kg',4,90000,1),
(17,'kg',4,90000,1),
(18,'kg',5,70000,1),
(19,'kg',5,80000,1),
(20,'kg',4,90000,1);


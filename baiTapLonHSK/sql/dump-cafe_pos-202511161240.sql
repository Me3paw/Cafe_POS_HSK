/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-12.0.2-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: cafe_pos
-- ------------------------------------------------------
-- Server version	12.0.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `ban`
--

DROP TABLE IF EXISTS `ban`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ban` (
  `maBan` int(11) NOT NULL,
  `maDonHang` int(11) DEFAULT NULL,
  `trangThai` enum('FREE','OCCUPIED','RESERVED','MAINTENANCE','TAKEAWAY') DEFAULT NULL,
  `soNguoi` int(11) DEFAULT NULL,
  `capNhatCuoi` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`maBan`),
  KEY `fk_ban_donhang` (`maDonHang`),
  CONSTRAINT `fk_ban_donhang` FOREIGN KEY (`maDonHang`) REFERENCES `donHang` (`maDonHang`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ban`
--

LOCK TABLES `ban` WRITE;
/*!40000 ALTER TABLE `ban` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `ban` VALUES
(1,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(2,NULL,'OCCUPIED',NULL,'2025-11-16 04:01:00'),
(3,NULL,'OCCUPIED',NULL,'2025-11-16 04:35:03'),
(4,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(5,NULL,'OCCUPIED',4,'2025-11-16 10:40:24'),
(6,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(7,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(8,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(9,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(10,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(11,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(12,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(13,NULL,'FREE',NULL,'2025-11-16 04:00:48'),
(14,NULL,'FREE',NULL,'2025-11-16 04:00:48');
/*!40000 ALTER TABLE `ban` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `caLam`
--

DROP TABLE IF EXISTS `caLam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `caLam` (
  `maCa` int(11) NOT NULL AUTO_INCREMENT,
  `maNguoiDung` int(11) NOT NULL,
  `batDau` datetime NOT NULL,
  `ketThuc` datetime DEFAULT NULL,
  `tongGio` decimal(5,2) DEFAULT NULL,
  `trangThai` enum('dangMo','daDong') DEFAULT 'dangMo',
  PRIMARY KEY (`maCa`),
  KEY `maNguoiDung` (`maNguoiDung`),
  CONSTRAINT `caLam_ibfk_1` FOREIGN KEY (`maNguoiDung`) REFERENCES `nguoiDung` (`maNguoiDung`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `caLam`
--

LOCK TABLES `caLam` WRITE;
/*!40000 ALTER TABLE `caLam` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `caLam` VALUES
(1,1,'2025-11-16 06:35:35','2025-11-16 12:35:35',6.00,'daDong'),
(2,2,'2025-11-16 09:35:35',NULL,NULL,'dangMo');
/*!40000 ALTER TABLE `caLam` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `chiTietDonHang`
--

DROP TABLE IF EXISTS `chiTietDonHang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chiTietDonHang` (
  `maChiTiet` int(11) NOT NULL AUTO_INCREMENT,
  `maDonHang` int(11) NOT NULL,
  `maMon` int(11) NOT NULL,
  `soLuong` int(11) DEFAULT 1,
  `giaBan` decimal(8,2) NOT NULL,
  `thanhTien` decimal(10,2) NOT NULL,
  `maThue` int(11) DEFAULT NULL,
  `tienThue` decimal(10,2) DEFAULT 0.00,
  PRIMARY KEY (`maChiTiet`),
  KEY `maDonHang` (`maDonHang`),
  KEY `maMon` (`maMon`),
  KEY `maThue` (`maThue`),
  CONSTRAINT `chiTietDonHang_ibfk_1` FOREIGN KEY (`maDonHang`) REFERENCES `donHang` (`maDonHang`),
  CONSTRAINT `chiTietDonHang_ibfk_2` FOREIGN KEY (`maMon`) REFERENCES `mon` (`maMon`),
  CONSTRAINT `chiTietDonHang_ibfk_3` FOREIGN KEY (`maThue`) REFERENCES `thue` (`maThue`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chiTietDonHang`
--

LOCK TABLES `chiTietDonHang` WRITE;
/*!40000 ALTER TABLE `chiTietDonHang` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `chiTietDonHang` VALUES
(1,1,1,1,30000.00,30000.00,1,3000.00),
(2,2,6,1,45000.00,45000.00,1,4500.00),
(3,2,2,1,30000.00,30000.00,1,3000.00),
(4,3,10,1,35000.00,35000.00,1,3500.00),
(5,4,4,1,40000.00,40000.00,1,4000.00),
(6,4,3,1,35000.00,35000.00,1,3500.00),
(7,5,11,1,15000.00,15000.00,1,1500.00),
(8,6,17,1,55000.00,55000.00,1,5500.00),
(9,7,2,2,30000.00,60000.00,1,6000.00),
(10,8,14,1,20000.00,20000.00,1,2000.00),
(11,9,8,1,40000.00,40000.00,1,4000.00),
(12,10,7,1,30000.00,30000.00,1,3000.00);
/*!40000 ALTER TABLE `chiTietDonHang` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `danhMuc`
--

DROP TABLE IF EXISTS `danhMuc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `danhMuc` (
  `maDanhMuc` int(11) NOT NULL AUTO_INCREMENT,
  `tenDanhMuc` varchar(100) NOT NULL,
  `moTa` text DEFAULT NULL,
  PRIMARY KEY (`maDanhMuc`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `danhMuc`
--

LOCK TABLES `danhMuc` WRITE;
/*!40000 ALTER TABLE `danhMuc` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `danhMuc` VALUES
(1,'Cà phê','Các loại cà phê'),
(2,'Trà','Trà sữa, trà trái cây'),
(3,'Đá xay','Sinh tố, frappe'),
(4,'Nước đóng chai','Nước ngọt, nước suối');
/*!40000 ALTER TABLE `danhMuc` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `donHang`
--

DROP TABLE IF EXISTS `donHang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `donHang` (
  `maDonHang` int(11) NOT NULL AUTO_INCREMENT,
  `maNguoiDung` int(11) DEFAULT NULL,
  `maCa` int(11) DEFAULT NULL,
  `maKhachHang` int(11) DEFAULT NULL,
  `maGiamGia` int(11) DEFAULT NULL,
  `tongTien` decimal(10,2) NOT NULL,
  `tienGiam` decimal(10,2) DEFAULT 0.00,
  `tienThue` decimal(10,2) DEFAULT 0.00,
  `tongCuoi` decimal(10,2) NOT NULL,
  `trangThai` enum('dangMo','daThanhToan','huy') DEFAULT 'dangMo',
  `loaiDon` enum('taiCho','mangVe','giaoHang') DEFAULT 'taiCho',
  `thoiGianTao` datetime DEFAULT current_timestamp(),
  `maBan` int(11) NOT NULL,
  PRIMARY KEY (`maDonHang`),
  KEY `maNguoiDung` (`maNguoiDung`),
  KEY `maCa` (`maCa`),
  KEY `maKhachHang` (`maKhachHang`),
  KEY `maGiamGia` (`maGiamGia`),
  KEY `fk_dh_ban` (`maBan`),
  CONSTRAINT `donHang_ibfk_1` FOREIGN KEY (`maNguoiDung`) REFERENCES `nguoiDung` (`maNguoiDung`),
  CONSTRAINT `donHang_ibfk_2` FOREIGN KEY (`maCa`) REFERENCES `caLam` (`maCa`),
  CONSTRAINT `donHang_ibfk_3` FOREIGN KEY (`maKhachHang`) REFERENCES `khachHang` (`maKhachHang`),
  CONSTRAINT `donHang_ibfk_4` FOREIGN KEY (`maGiamGia`) REFERENCES `giamGia` (`maGiamGia`),
  CONSTRAINT `fk_dh_ban` FOREIGN KEY (`maBan`) REFERENCES `ban` (`maBan`),
  CONSTRAINT `fk_donhang_maban` FOREIGN KEY (`maBan`) REFERENCES `ban` (`maBan`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `donHang`
--

LOCK TABLES `donHang` WRITE;
/*!40000 ALTER TABLE `donHang` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `donHang` VALUES
(1,1,1,1,NULL,30000.00,0.00,3000.00,33000.00,'daThanhToan','taiCho','2025-11-16 12:39:05',2),
(2,2,2,2,1,85000.00,4250.00,8075.00,88825.00,'daThanhToan','taiCho','2025-11-16 12:39:05',5),
(3,3,1,NULL,NULL,45000.00,0.00,4500.00,49500.00,'daThanhToan','mangVe','2025-11-16 12:39:05',1),
(4,1,1,3,NULL,60000.00,0.00,6000.00,66000.00,'dangMo','taiCho','2025-11-16 12:39:05',3),
(5,2,2,4,NULL,20000.00,0.00,2000.00,22000.00,'daThanhToan','taiCho','2025-11-16 12:39:05',4),
(6,1,1,5,NULL,75000.00,0.00,7500.00,82500.00,'daThanhToan','mangVe','2025-11-16 12:39:05',6),
(7,2,2,NULL,NULL,55000.00,0.00,5500.00,60500.00,'daThanhToan','taiCho','2025-11-16 12:39:05',7),
(8,3,1,NULL,NULL,50000.00,0.00,5000.00,55000.00,'huy','giaoHang','2025-11-16 12:39:05',8),
(9,1,1,3,NULL,35000.00,0.00,3500.00,38500.00,'daThanhToan','taiCho','2025-11-16 12:39:05',9),
(10,2,2,2,NULL,40000.00,0.00,4000.00,44000.00,'daThanhToan','taiCho','2025-11-16 12:39:05',10);
/*!40000 ALTER TABLE `donHang` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `giamGia`
--

DROP TABLE IF EXISTS `giamGia`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `giamGia` (
  `maGiamGia` int(11) NOT NULL AUTO_INCREMENT,
  `tenChuongTrinh` varchar(100) DEFAULT NULL,
  `loai` enum('phanTram','coDinh') NOT NULL,
  `giaTri` decimal(8,2) NOT NULL,
  `hangApDung` enum('dong','bac','vang','bachKim') DEFAULT NULL,
  `ngayBatDau` date DEFAULT NULL,
  `ngayKetThuc` date DEFAULT NULL,
  `dangApDung` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`maGiamGia`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `giamGia`
--

LOCK TABLES `giamGia` WRITE;
/*!40000 ALTER TABLE `giamGia` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `giamGia` VALUES
(1,'Giảm 5% thành viên bạc','phanTram',5.00,'bac','2025-11-16','2025-12-16',1);
/*!40000 ALTER TABLE `giamGia` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `khachHang`
--

DROP TABLE IF EXISTS `khachHang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `khachHang` (
  `maKhachHang` int(11) NOT NULL AUTO_INCREMENT,
  `hoTen` varchar(100) DEFAULT NULL,
  `soDienThoai` varchar(15) NOT NULL,
  `hangThanhVien` enum('dong','bac','vang','bachKim') DEFAULT 'dong',
  `ngayTao` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`maKhachHang`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `khachHang`
--

LOCK TABLES `khachHang` WRITE;
/*!40000 ALTER TABLE `khachHang` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `khachHang` VALUES
(1,'Nguyễn Văn A','0901111111','dong','2025-11-16 12:33:55'),
(2,'Trần Thị B','0902222222','bac','2025-11-16 12:33:55'),
(3,'Lê Văn C','0903333333','vang','2025-11-16 12:33:55'),
(4,'Phạm Thị D','0904444444','dong','2025-11-16 12:33:55'),
(5,'Hoàng Văn E','0905555555','bachKim','2025-11-16 12:33:55');
/*!40000 ALTER TABLE `khachHang` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `mon`
--

DROP TABLE IF EXISTS `mon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon` (
  `maMon` int(11) NOT NULL AUTO_INCREMENT,
  `maDanhMuc` int(11) DEFAULT NULL,
  `tenMon` varchar(100) NOT NULL,
  `giaBan` decimal(8,2) NOT NULL,
  `conBan` tinyint(1) DEFAULT 1,
  `moTa` text DEFAULT NULL,
  PRIMARY KEY (`maMon`),
  KEY `maDanhMuc` (`maDanhMuc`),
  CONSTRAINT `mon_ibfk_1` FOREIGN KEY (`maDanhMuc`) REFERENCES `danhMuc` (`maDanhMuc`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon`
--

LOCK TABLES `mon` WRITE;
/*!40000 ALTER TABLE `mon` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `mon` VALUES
(1,1,'Cà phê đen',25000.00,1,''),
(2,1,'Cà phê sữa',30000.00,1,''),
(3,1,'Bạc xỉu',35000.00,1,''),
(4,1,'Latte',40000.00,1,''),
(5,1,'Espresso',30000.00,1,''),
(6,2,'Trà đào cam sả',45000.00,1,''),
(7,2,'Trà chanh',30000.00,1,''),
(8,2,'Trà sữa truyền thống',40000.00,1,''),
(9,2,'Hồng trà matcha',45000.00,1,''),
(10,2,'Trà vải',35000.00,1,''),
(11,3,'Nước suối',15000.00,1,''),
(12,3,'Coca-Cola',20000.00,1,''),
(13,3,'Pepsi',20000.00,1,''),
(14,3,'Sting',20000.00,1,''),
(15,3,'7Up',20000.00,1,''),
(16,4,'Matcha đá xay',55000.00,1,''),
(17,4,'Cookie đá xay',55000.00,1,''),
(18,4,'Sinh tố xoài',50000.00,1,''),
(19,4,'Sinh tố bơ',55000.00,1,''),
(20,4,'Chocolate đá xay',55000.00,1,'');
/*!40000 ALTER TABLE `mon` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `nguoiDung`
--

DROP TABLE IF EXISTS `nguoiDung`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `nguoiDung` (
  `maNguoiDung` int(11) NOT NULL AUTO_INCREMENT,
  `tenDangNhap` varchar(100) NOT NULL,
  `hoTen` varchar(100) NOT NULL,
  `vaiTro` enum('thuNgan','phaChe','quanLy') NOT NULL,
  `matKhau` varchar(255) NOT NULL,
  `ngayTao` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`maNguoiDung`),
  UNIQUE KEY `tenDangNhap` (`tenDangNhap`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nguoiDung`
--

LOCK TABLES `nguoiDung` WRITE;
/*!40000 ALTER TABLE `nguoiDung` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `nguoiDung` VALUES
(1,'admin','Quản Lý Chính','quanLy','123456','2025-11-16 12:33:48'),
(2,'phaChe1','Pha Chế A','phaChe','123456','2025-11-16 12:33:48'),
(3,'thuNgan1','Thu Ngân A','thuNgan','123456','2025-11-16 12:33:48');
/*!40000 ALTER TABLE `nguoiDung` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `thanhToan`
--

DROP TABLE IF EXISTS `thanhToan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `thanhToan` (
  `maThanhToan` int(11) NOT NULL AUTO_INCREMENT,
  `maDonHang` int(11) NOT NULL,
  `hinhThuc` enum('tienMat','the','viDienTu','khac') NOT NULL,
  `soTien` decimal(10,2) NOT NULL,
  `thoiGian` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`maThanhToan`),
  KEY `maDonHang` (`maDonHang`),
  CONSTRAINT `thanhToan_ibfk_1` FOREIGN KEY (`maDonHang`) REFERENCES `donHang` (`maDonHang`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thanhToan`
--

LOCK TABLES `thanhToan` WRITE;
/*!40000 ALTER TABLE `thanhToan` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `thanhToan` VALUES
(1,1,'tienMat',33000.00,'2025-11-16 12:39:14'),
(2,2,'the',88825.00,'2025-11-16 12:39:14'),
(3,3,'tienMat',49500.00,'2025-11-16 12:39:14'),
(4,5,'tienMat',22000.00,'2025-11-16 12:39:14'),
(5,6,'viDienTu',82500.00,'2025-11-16 12:39:14'),
(6,7,'tienMat',60500.00,'2025-11-16 12:39:14'),
(7,9,'tienMat',38500.00,'2025-11-16 12:39:14'),
(8,10,'tienMat',44000.00,'2025-11-16 12:39:14');
/*!40000 ALTER TABLE `thanhToan` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `thue`
--

DROP TABLE IF EXISTS `thue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `thue` (
  `maThue` int(11) NOT NULL AUTO_INCREMENT,
  `tenThue` varchar(100) DEFAULT NULL,
  `tyLe` decimal(5,2) NOT NULL,
  `dangApDung` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`maThue`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thue`
--

LOCK TABLES `thue` WRITE;
/*!40000 ALTER TABLE `thue` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `thue` VALUES
(1,'VAT',10.00,1);
/*!40000 ALTER TABLE `thue` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `tonKho`
--

DROP TABLE IF EXISTS `tonKho`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tonKho` (
  `maTon` int(11) NOT NULL AUTO_INCREMENT,
  `maMon` int(11) NOT NULL,
  `donVi` varchar(50) DEFAULT NULL,
  `soLuong` decimal(10,2) DEFAULT 0.00,
  `giaNhap` decimal(10,2) DEFAULT 0.00,
  `mucCanhBao` decimal(10,2) DEFAULT 0.00,
  `capNhatCuoi` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`maTon`),
  UNIQUE KEY `maMon` (`maMon`),
  CONSTRAINT `fk_tonkho_mon` FOREIGN KEY (`maMon`) REFERENCES `mon` (`maMon`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tonKho`
--

LOCK TABLES `tonKho` WRITE;
/*!40000 ALTER TABLE `tonKho` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `tonKho` VALUES
(1,1,'kg',5.00,100000.00,1.00,'2025-11-16 12:39:19'),
(2,2,'kg',5.00,100000.00,1.00,'2025-11-16 12:39:19'),
(3,3,'kg',4.00,120000.00,1.00,'2025-11-16 12:39:19'),
(4,4,'kg',3.00,150000.00,1.00,'2025-11-16 12:39:19'),
(5,5,'kg',3.00,150000.00,1.00,'2025-11-16 12:39:19'),
(6,6,'L',10.00,40000.00,2.00,'2025-11-16 12:39:19'),
(7,7,'L',10.00,30000.00,2.00,'2025-11-16 12:39:19'),
(8,8,'L',10.00,35000.00,2.00,'2025-11-16 12:39:19'),
(9,9,'L',8.00,35000.00,2.00,'2025-11-16 12:39:19'),
(10,10,'L',10.00,30000.00,2.00,'2025-11-16 12:39:19'),
(11,11,'chai',50.00,5000.00,10.00,'2025-11-16 12:39:19'),
(12,12,'chai',40.00,6000.00,10.00,'2025-11-16 12:39:19'),
(13,13,'chai',35.00,6000.00,10.00,'2025-11-16 12:39:19'),
(14,14,'chai',30.00,7000.00,10.00,'2025-11-16 12:39:19'),
(15,15,'chai',30.00,7000.00,10.00,'2025-11-16 12:39:19'),
(16,16,'kg',4.00,90000.00,1.00,'2025-11-16 12:39:19'),
(17,17,'kg',4.00,90000.00,1.00,'2025-11-16 12:39:19'),
(18,18,'kg',5.00,70000.00,1.00,'2025-11-16 12:39:19'),
(19,19,'kg',5.00,80000.00,1.00,'2025-11-16 12:39:19'),
(20,20,'kg',4.00,90000.00,1.00,'2025-11-16 12:39:19');
/*!40000 ALTER TABLE `tonKho` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Dumping routines for database 'cafe_pos'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2025-11-16 12:40:31

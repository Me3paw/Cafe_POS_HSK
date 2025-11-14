🧭 Mục tiêu thiết kế

Cơ sở dữ liệu này được xây dựng theo hướng:

Dễ hiểu bằng tiếng Việt, giữ đúng nghĩa thực tế trong vận hành quán.

Dễ mở rộng: Có thể thêm module (ví dụ: báo cáo ca, nguyên liệu, lịch làm việc).

Đảm bảo tính toàn vẹn dữ liệu qua các khóa ngoại.

🧩 Quy ước đặt tên

Tên bảng = danh từ viết thường, Pascal-case đơn giản, có ý nghĩa rõ ràng:
nguoiDung, donHang, thanhToan, tonKho

Tên cột = mô tả ngắn gọn, bắt đầu bằng “ma” nếu là khóa chính / khóa ngoại.
Ví dụ: maDonHang, maKhachHang, maThue.

Dùng ENUM cho các giá trị có phạm vi giới hạn (vai trò, trạng thái, loại đơn, hạng thành viên).

Dùng DATETIME DEFAULT CURRENT_TIMESTAMP để tự động ghi nhận thời điểm tạo.

⚙️ Quan hệ chính
Bảng	Quan hệ	Ý nghĩa
nguoiDung ↔ caLam	1:N	Một nhân viên có nhiều ca
caLam ↔ donHang	1:N	Một ca có nhiều đơn
khachHang ↔ donHang	1:N	Một khách hàng có nhiều đơn
donHang ↔ chiTietDonHang	1:N	Một đơn có nhiều món
mon ↔ chiTietDonHang	1:N	Một món nằm trong nhiều đơn
donHang ↔ thanhToan	1:N	Một đơn có thể thanh toán nhiều lần
giamGia ↔ donHang	1:N	Mỗi đơn có thể áp dụng một chương trình
tonKho	độc lập	Dành cho quản lý tồn kho
💡 Quy trình nghiệp vụ tiêu chuẩn

Nhân viên đăng nhập → mở ca (caLam).

Tạo đơn (donHang), thêm món (chiTietDonHang).

Hệ thống kiểm tra giamGia dựa theo hangThanhVien của khách (khachHang).

Tính tienThue, tienGiam, tongCuoi.

Thanh toán (thanhToan).

Cuối ngày → đóng ca → báo cáo tổng hợp doanh thu theo caLam.

🧠 Mở rộng trong tương lai

Thêm bảng lichLamViec (quản lý ca theo ngày).

Thêm baoCaoDoanhThu hoặc luongNhanVien để tổng hợp tự động.

Kết nối tonKho với donHang để tự động trừ nguyên liệu khi bán món.

Thêm lichSuGia để lưu lại thay đổi giá bán qua thời gian.

🔒 Ghi chú bảo mật

matKhau phải được mã hóa (bcrypt, Argon2, v.v.) — tuyệt đối không lưu plaintext.

Nên thêm role-based access control (RBAC) nếu hệ thống lớn dần.

Có thể thêm bảng nhatKyHoatDong để log thao tác quan trọng (xóa đơn, chỉnh kho…).

🧾 Quy tắc viết code tương thích

Khi viết code PHP, Node.js hoặc Java backend, luôn dùng tham số hóa (prepared statements) để tránh SQL Injection.

Không truy cập trực tiếp khóa ngoại nếu chưa tồn tại (validate trước khi insert).

Sử dụng transactions (START TRANSACTION → COMMIT / ROLLBACK) khi xử lý thanh toán hoặc thay đổi tồn kho.
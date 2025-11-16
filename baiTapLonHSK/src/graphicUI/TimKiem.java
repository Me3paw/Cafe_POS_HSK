package graphicUI;

import dao.DonHangDAO;
import dao.KhachHangDAO;
import dao.NguoiDungDAO;
import entity.DonHang;
import entity.KhachHang;
import entity.NguoiDung;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SearchPanel provides tabs for invoice, customer, and employee search.
 * Employee search is restricted to admin sessions.
 */
public class TimKiem extends JPanel {
    private final JTabbedPane tabs;

    public TimKiem(Component owner) {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Tìm hóa đơn", new InvoiceSearchPanel());
        tabs.addTab("Tìm khách hàng", new CustomerSearchPanel());
        tabs.addTab("Tìm nhân viên", buildEmployeeTab());
        add(tabs, BorderLayout.CENTER);
    }

    private Component buildEmployeeTab() {
        if (SessionContext.isAdmin()) {
            return new EmployeeSearchPanel();
        }
        JPanel locked = new JPanel(new BorderLayout(10, 10));
        locked.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel lbl = new JLabel("<html>Chức năng tìm nhân viên chỉ dành cho tài khoản quản trị.<br>Đăng nhập với quyền admin để tiếp tục.</html>");
        JButton btn = new JButton("Đăng nhập admin");
        btn.addActionListener(e -> promptForAdmin(locked));
        locked.add(lbl, BorderLayout.CENTER);
        locked.add(btn, BorderLayout.SOUTH);
        return locked;
    }

    private void promptForAdmin(Component lockedComponent) {
        DangNhapDialog dialog = new DangNhapDialog(SwingUtilities.getWindowAncestor(this));
        entity.NguoiDung user = dialog.showDialog();
        if (user == null) {
            return;
        }
        SessionContext.setCurrentUser(user);
        if (!SessionContext.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Tài khoản hiện tại không có quyền admin.", "Từ chối", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int idx = tabs.indexOfComponent(lockedComponent);
        if (idx >= 0) {
            tabs.setComponentAt(idx, new EmployeeSearchPanel());
            tabs.setTitleAt(idx, "Tìm nhân viên");
        }
    }

    static class InvoiceSearchPanel extends JPanel {
        private final DonHangDAO donHangDAO = new DonHangDAO();
        private final KhachHangDAO khachHangDAO = new KhachHangDAO();
        private final DefaultTableModel tableModel;
        private final JTable table;
        private final JTextField keywordField;
        private final JComboBox<InvoiceSearchField> fieldCombo;
        private final List<DonHang> allOrders = new ArrayList<>();
        private final Map<Integer, KhachHang> khachHangCache = new LinkedHashMap<>();
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        public InvoiceSearchPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            tableModel = new DefaultTableModel(new Object[]{"Mã hóa đơn", "Khách hàng", "SĐT", "Tổng cuối", "Trạng thái", "Ngày tạo"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(tableModel);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel controls = new JPanel();
            controls.add(new JLabel("Tìm kiếm theo:"));
            fieldCombo = new JComboBox<>(InvoiceSearchField.values());
            controls.add(fieldCombo);
            keywordField = new JTextField(20);
            controls.add(keywordField);
            JButton searchBtn = new JButton("Tìm");
            searchBtn.addActionListener(e -> performSearch());
            controls.add(searchBtn);
            add(controls, BorderLayout.NORTH);

            loadData();
            refreshTable(allOrders);
        }

        private void loadData() {
            try {
                List<DonHang> donHangs = donHangDAO.layHet();
                if (donHangs != null) {
                    allOrders.clear();
                    allOrders.addAll(donHangs);
                }
                List<KhachHang> customers = khachHangDAO.layHet();
                khachHangCache.clear();
                if (customers != null) {
                    for (KhachHang k : customers) {
                        khachHangCache.put(k.getMaKhachHang(), k);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu hóa đơn", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void performSearch() {
            String keyword = keywordField.getText().trim().toLowerCase();
            InvoiceSearchField field = (InvoiceSearchField) fieldCombo.getSelectedItem();
            if (field == null) {
                refreshTable(allOrders);
                return;
            }
            if (keyword.isEmpty()) {
                refreshTable(allOrders);
                return;
            }
            List<DonHang> filtered = new ArrayList<>();
            for (DonHang order : allOrders) {
                KhachHang kh = order.getMaKhachHang() != null ? khachHangCache.get(order.getMaKhachHang()) : null;
                if (field.matches(order, kh, keyword)) {
                    filtered.add(order);
                }
            }
            refreshTable(filtered);
        }

        private void refreshTable(List<DonHang> source) {
            tableModel.setRowCount(0);
            for (DonHang order : source) {
                KhachHang kh = order.getMaKhachHang() != null ? khachHangCache.get(order.getMaKhachHang()) : null;
                tableModel.addRow(new Object[]{
                        order.getMaDonHang(),
                        kh != null ? kh.getHoTen() : "Khách lẻ",
                        kh != null ? kh.getSoDienThoai() : "",
                        formatCurrency(order.getTongCuoi()),
                        order.getTrangThai(),
                        formatTimestamp(order.getThoiGianTao())
                });
            }
        }

        private String formatCurrency(BigDecimal value) {
            if (value == null) {
                value = BigDecimal.ZERO;
            }
            return currencyFormat.format(value);
        }

        private String formatTimestamp(java.sql.Timestamp ts) {
            if (ts == null) {
                return "";
            }
            return dateFormat.format(ts);
        }
    }

    enum InvoiceSearchField {
        HO_TEN("Họ tên") {
            @Override
            public boolean matches(DonHang order, KhachHang kh, String keyword) {
                if (kh == null || kh.getHoTen() == null) return false;
                return kh.getHoTen().toLowerCase().contains(keyword);
            }
        },
        SO_DIEN_THOAI("Số điện thoại") {
            @Override
            public boolean matches(DonHang order, KhachHang kh, String keyword) {
                if (kh == null || kh.getSoDienThoai() == null) return false;
                return kh.getSoDienThoai().toLowerCase().contains(keyword);
            }
        };

        private final String label;

        InvoiceSearchField(String label) {
            this.label = label;
        }

        public abstract boolean matches(DonHang order, KhachHang kh, String keyword);

        @Override
        public String toString() {
            return label;
        }
    }

    static class CustomerSearchPanel extends JPanel {
        private final KhachHangDAO khachHangDAO = new KhachHangDAO();
        private final DefaultTableModel model;
        private final JTable table;
        private final JTextField keywordField;
        private final JComboBox<CustomerSearchField> fieldCombo;
        private final JTextField nameField = new JTextField(20);
        private final JTextField phoneField = new JTextField(15);
        private final JTextField tierField = new JTextField(10);
        private final JTextField createdField = new JTextField(18);
        private final JButton updateBtn = new JButton("Cập nhật SĐT");
        private final List<KhachHang> allCustomers = new ArrayList<>();
        private KhachHang selectedCustomer;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        public CustomerSearchPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            model = new DefaultTableModel(new Object[]{"Mã KH", "Họ tên", "SĐT", "Hạng", "Ngày tạo"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(new CustomerSelectionListener());
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel controls = new JPanel();
            controls.add(new JLabel("Tìm kiếm theo:"));
            fieldCombo = new JComboBox<>(CustomerSearchField.values());
            controls.add(fieldCombo);
            keywordField = new JTextField(20);
            controls.add(keywordField);
            JButton searchBtn = new JButton("Tìm");
            searchBtn.addActionListener(e -> performSearch());
            controls.add(searchBtn);
            add(controls, BorderLayout.NORTH);

            JPanel detailPanel = buildDetailPanel();
            add(detailPanel, BorderLayout.SOUTH);

            loadData();
            refreshTable(allCustomers);
        }

        private JPanel buildDetailPanel() {
            JPanel detail = new JPanel(new GridBagLayout());
            detail.setBorder(BorderFactory.createTitledBorder("Chi tiết khách hàng"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;
            gbc.gridx = 0;
            gbc.gridy = row;
            detail.add(new JLabel("Họ tên:"), gbc);
            gbc.gridx = 1;
            nameField.setEditable(false);
            detail.add(nameField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            detail.add(new JLabel("Số điện thoại:"), gbc);
            gbc.gridx = 1;
            phoneField.setEditable(SessionContext.isAdmin());
            detail.add(phoneField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            detail.add(new JLabel("Hạng thành viên:"), gbc);
            gbc.gridx = 1;
            tierField.setEditable(false);
            detail.add(tierField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            detail.add(new JLabel("Ngày tạo:"), gbc);
            gbc.gridx = 1;
            createdField.setEditable(false);
            detail.add(createdField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.EAST;
            updateBtn.setEnabled(SessionContext.isAdmin());
            updateBtn.addActionListener(e -> updatePhoneNumber());
            detail.add(updateBtn, gbc);

            return detail;
        }

        private void loadData() {
            try {
                List<KhachHang> customers = khachHangDAO.layHet();
                allCustomers.clear();
                if (customers != null) {
                    for (KhachHang kh : customers) {
                        if (isValidCustomer(kh)) {
                            allCustomers.add(kh);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu khách hàng", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void performSearch() {
            String keyword = keywordField.getText().trim().toLowerCase();
            CustomerSearchField field = (CustomerSearchField) fieldCombo.getSelectedItem();
            if (field == null || keyword.isEmpty()) {
                refreshTable(allCustomers);
                return;
            }
            List<KhachHang> filtered = new ArrayList<>();
            for (KhachHang kh : allCustomers) {
                if (field.matches(kh, keyword)) {
                    filtered.add(kh);
                }
            }
            refreshTable(filtered);
        }

        private boolean isValidCustomer(KhachHang kh) {
            if (kh == null) {
                return false;
            }
            if (kh.getMaKhachHang() <= 0) {
                return false;
            }
            String name = kh.getHoTen();
            return name == null || !name.trim().equalsIgnoreCase("khách lẻ");
        }

        private void refreshTable(List<KhachHang> source) {
            model.setRowCount(0);
            for (KhachHang kh : source) {
                model.addRow(new Object[]{
                        kh.getMaKhachHang(),
                        kh.getHoTen(),
                        kh.getSoDienThoai(),
                        kh.getHangThanhVien(),
                        formatTimestamp(kh.getNgayTao())
                });
            }
            table.clearSelection();
            showCustomerDetail(null);
        }

        private void updatePhoneNumber() {
            if (!SessionContext.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Bạn không có quyền chỉnh sửa số điện thoại", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedCustomer == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String newPhone = phoneField.getText().trim();
            if (newPhone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Số điện thoại không được bỏ trống", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectedCustomer.setSoDienThoai(newPhone);
            boolean success = khachHangDAO.capNhat(selectedCustomer);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã cập nhật số điện thoại", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                performSearch();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showCustomerDetail(KhachHang kh) {
            selectedCustomer = kh;
            if (kh == null) {
                nameField.setText("");
                phoneField.setText("");
                tierField.setText("");
                createdField.setText("");
                return;
            }
            nameField.setText(kh.getHoTen());
            phoneField.setText(kh.getSoDienThoai());
            tierField.setText(kh.getHangThanhVien());
            createdField.setText(formatTimestamp(kh.getNgayTao()));
        }

        private String formatTimestamp(java.sql.Timestamp ts) {
            if (ts == null) return "";
            return dateFormat.format(ts);
        }

        private class CustomerSelectionListener implements ListSelectionListener {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                int row = table.getSelectedRow();
                if (row < 0) {
                    showCustomerDetail(null);
                    return;
                }
                int modelRow = table.convertRowIndexToModel(row);
                int customerId = (int) model.getValueAt(modelRow, 0);
                for (KhachHang kh : allCustomers) {
                    if (kh.getMaKhachHang() == customerId) {
                        showCustomerDetail(kh);
                        return;
                    }
                }
                showCustomerDetail(null);
            }
        }
    }

    enum CustomerSearchField {
        HO_TEN("Họ tên") {
            @Override
            public boolean matches(KhachHang kh, String keyword) {
                return kh.getHoTen() != null && kh.getHoTen().toLowerCase().contains(keyword);
            }
        },
        SO_DIEN_THOAI("Số điện thoại") {
            @Override
            public boolean matches(KhachHang kh, String keyword) {
                return kh.getSoDienThoai() != null && kh.getSoDienThoai().toLowerCase().contains(keyword);
            }
        };

        private final String label;

        CustomerSearchField(String label) {
            this.label = label;
        }

        public abstract boolean matches(KhachHang kh, String keyword);

        @Override
        public String toString() {
            return label;
        }
    }

    static class EmployeeSearchPanel extends JPanel {
        private final NguoiDungDAO nguoiDungDAO = new NguoiDungDAO();
        private final DefaultTableModel model;
        private final JTable table;
        private final JTextField keywordField;
        private final JComboBox<EmployeeSearchField> fieldCombo;
        private final List<NguoiDung> allEmployees = new ArrayList<>();

        public EmployeeSearchPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            model = new DefaultTableModel(new Object[]{"Mã người dùng", "Tên đăng nhập", "Họ tên", "Vai trò"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel controls = new JPanel();
            controls.add(new JLabel("Tìm kiếm theo:"));
            fieldCombo = new JComboBox<>(EmployeeSearchField.values());
            controls.add(fieldCombo);
            keywordField = new JTextField(20);
            controls.add(keywordField);
            JButton searchBtn = new JButton("Tìm");
            searchBtn.addActionListener(e -> performSearch());
            controls.add(searchBtn);
            add(controls, BorderLayout.NORTH);

            loadData();
            refreshTable(allEmployees);
        }

        private void loadData() {
            try {
                List<NguoiDung> employees = nguoiDungDAO.layHet();
                allEmployees.clear();
                if (employees != null) {
                    allEmployees.addAll(employees);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Không thể tải danh sách nhân viên", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void performSearch() {
            String keyword = keywordField.getText().trim().toLowerCase();
            EmployeeSearchField field = (EmployeeSearchField) fieldCombo.getSelectedItem();
            if (field == null || keyword.isEmpty()) {
                refreshTable(allEmployees);
                return;
            }
            List<NguoiDung> filtered = new ArrayList<>();
            for (NguoiDung n : allEmployees) {
                if (field.matches(n, keyword)) {
                    filtered.add(n);
                }
            }
            refreshTable(filtered);
        }

        private void refreshTable(List<NguoiDung> source) {
            model.setRowCount(0);
            for (NguoiDung n : source) {
                model.addRow(new Object[]{
                        n.getMaNguoiDung(),
                        n.getTenDangNhap(),
                        n.getHoTen(),
                        n.getVaiTro()
                });
            }
        }
    }

    enum EmployeeSearchField {
        TEN_DANG_NHAP("Tên đăng nhập") {
            @Override
            public boolean matches(NguoiDung n, String keyword) {
                return n.getTenDangNhap() != null && n.getTenDangNhap().toLowerCase().contains(keyword);
            }
        },
        HO_TEN("Họ tên") {
            @Override
            public boolean matches(NguoiDung n, String keyword) {
                return n.getHoTen() != null && n.getHoTen().toLowerCase().contains(keyword);
            }
        },
        VAI_TRO("Vai trò") {
            @Override
            public boolean matches(NguoiDung n, String keyword) {
                return n.getVaiTro() != null && n.getVaiTro().toLowerCase().contains(keyword);
            }
        };

        private final String label;

        EmployeeSearchField(String label) {
            this.label = label;
        }

        public abstract boolean matches(NguoiDung n, String keyword);

        @Override
        public String toString() {
            return label;
        }
    }
}

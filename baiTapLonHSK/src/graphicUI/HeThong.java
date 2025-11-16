package graphicUI;

import dao.NguoiDungDAO;
import dao.ThueDAO;
import entity.NguoiDung;
import entity.Thue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * SystemPanel now exposes password settings for the current user and, if the
 * logged-in account is an admin, advanced tabs for managing taxes and roles.
 */
public class HeThong extends JPanel {
    private final JTabbedPane tabs = new JTabbedPane();

    public HeThong() {
        setLayout(new BorderLayout());
        tabs.addTab("Mật khẩu", buildPasswordTab());
        tabs.addTab("Thuế", buildAdminTab(
                "Chỉ tài khoản quản trị mới có thể chỉnh thuế.",
                TaxPanel::new));
        tabs.addTab("Phân quyền", buildAdminTab(
                "Chỉ tài khoản quản trị mới được thay đổi vai trò.",
                RolePanel::new));
        add(tabs, BorderLayout.CENTER);
    }

    private Component buildAdminTab(String message, Supplier<Component> supplier) {
        if (SessionContext.isAdmin()) {
            return supplier.get();
        }
        JPanel locked = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("<html>" + message + "<br>Đăng nhập admin để tiếp tục.</html>");
        lbl.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JButton btn = new JButton("Đăng nhập admin");
        btn.addActionListener(e -> {
            DangNhapDialog dialog = new DangNhapDialog(SwingUtilities.getWindowAncestor(this));
            NguoiDung user = dialog.showDialog();
            if (user == null) return;
            SessionContext.setCurrentUser(user);
            if (!SessionContext.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Tài khoản không phải admin.", "Từ chối", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int idx = tabs.indexOfComponent(locked);
            if (idx >= 0) {
                tabs.setComponentAt(idx, supplier.get());
            }
        });
        locked.add(lbl, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(btn);
        locked.add(south, BorderLayout.SOUTH);
        return locked;
    }

    private JPanel buildPasswordTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        JPasswordField currentField = new JPasswordField();
        JPasswordField newField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        form.add(new JLabel("Mật khẩu hiện tại:"));
        form.add(currentField);
        form.add(new JLabel("Mật khẩu mới:"));
        form.add(newField);
        form.add(new JLabel("Xác nhận mật khẩu mới:"));
        form.add(confirmField);

        JButton saveBtn = new JButton("Lưu mật khẩu");
        saveBtn.addActionListener(e -> {
            NguoiDung user = SessionContext.getCurrentUser();
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Chưa đăng nhập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String current = new String(currentField.getPassword());
            String n = new String(newField.getPassword());
            String c = new String(confirmField.getPassword());
            if (user.getMatKhau() != null && !user.getMatKhau().equals(current)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu hiện tại không đúng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (n == null || n.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Mật khẩu mới không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!n.equals(c)) {
                JOptionPane.showMessageDialog(this, "Xác nhận mật khẩu không khớp.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            NguoiDungDAO dao = new NguoiDungDAO();
            boolean ok = dao.capNhatMatKhau(user.getMaNguoiDung(), n);
            if (ok) {
                user.setMatKhau(n);
                SessionContext.setCurrentUser(user);
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                currentField.setText("");
                newField.setText("");
                confirmField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        root.add(form, BorderLayout.CENTER);
        root.add(saveBtn, BorderLayout.SOUTH);
        return root;
    }

    private static class TaxPanel extends JPanel {
        private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Mã", "Tên thuế", "Tỷ lệ (%)", "Áp dụng"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        private final JTable table = new JTable(model);
        private final JTextField nameField = new JTextField(15);
        private final JTextField rateField = new JTextField(8);
        private final JCheckBox activeCheck = new JCheckBox("Đặt làm thuế mặc định");
        private final ThueDAO dao = new ThueDAO();

        public TaxPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4,4,4,4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Tên thuế:"), gbc);
            gbc.gridx = 1; form.add(nameField, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            form.add(new JLabel("Tỷ lệ (%):"), gbc);
            gbc.gridx = 1; form.add(rateField, gbc);
            gbc.gridx = 1; gbc.gridy = 2; form.add(activeCheck, gbc);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addBtn = new JButton("Thêm");
            JButton updateBtn = new JButton("Cập nhật");
            JButton setActiveBtn = new JButton("Đặt làm mặc định");
            JButton deleteBtn = new JButton("Xóa");
            buttons.add(addBtn);
            buttons.add(updateBtn);
            buttons.add(setActiveBtn);
            buttons.add(deleteBtn);

            addBtn.addActionListener(e -> addTax());
            updateBtn.addActionListener(e -> updateSelectedTax());
            setActiveBtn.addActionListener(e -> markSelectedActive());
            deleteBtn.addActionListener(e -> deleteSelectedTax());

            JPanel south = new JPanel(new BorderLayout());
            south.add(form, BorderLayout.CENTER);
            south.add(buttons, BorderLayout.SOUTH);
            add(south, BorderLayout.SOUTH);

            reload();
        }

        private void reload() {
            model.setRowCount(0);
            List<Thue> list = dao.layHet();
            if (list != null) {
                for (Thue t : list) {
                    model.addRow(new Object[]{t.getMaThue(), t.getTenThue(), t.getTyLe(), t.isDangApDung()});
                }
            }
        }

        private void fillFormFromSelection() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            nameField.setText(String.valueOf(model.getValueAt(row, 1)));
            rateField.setText(String.valueOf(model.getValueAt(row, 2)));
            activeCheck.setSelected(Boolean.TRUE.equals(model.getValueAt(row, 3)));
        }

        private Thue getSelectedTax() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Chọn một dòng thuế trước.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            Thue t = new Thue();
            t.setMaThue(Integer.parseInt(model.getValueAt(row, 0).toString()));
            t.setTenThue(model.getValueAt(row, 1).toString());
            t.setTyLe(new BigDecimal(model.getValueAt(row, 2).toString()));
            t.setDangApDung(Boolean.TRUE.equals(model.getValueAt(row, 3)));
            return t;
        }

        private BigDecimal parseRate() {
            try {
                return new BigDecimal(rateField.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Tỷ lệ không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        private void addTax() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập tên thuế.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            BigDecimal rate = parseRate();
            if (rate == null) return;
            Thue t = new Thue();
            t.setTenThue(name);
            t.setTyLe(rate);
            t.setDangApDung(activeCheck.isSelected());
            if (dao.them(t)) {
                if (t.isDangApDung()) {
                    markOnlyActive(t.getMaThue());
                }
                reload();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thêm thuế.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateSelectedTax() {
            Thue selected = getSelectedTax();
            if (selected == null) return;
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập tên thuế.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            BigDecimal rate = parseRate();
            if (rate == null) return;
            selected.setTenThue(name);
            selected.setTyLe(rate);
            selected.setDangApDung(activeCheck.isSelected());
            if (dao.capNhat(selected)) {
                if (selected.isDangApDung()) {
                    markOnlyActive(selected.getMaThue());
                }
                reload();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật thuế.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteSelectedTax() {
            Thue selected = getSelectedTax();
            if (selected == null) return;
            int confirm = JOptionPane.showConfirmDialog(this, "Xóa thuế đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (dao.xoa(selected.getMaThue())) {
                    reload();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể xóa thuế.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void markSelectedActive() {
            Thue selected = getSelectedTax();
            if (selected == null) return;
            markOnlyActive(selected.getMaThue());
            reload();
        }

        private void markOnlyActive(int maThue) {
            List<Thue> list = dao.layHet();
            if (list == null) return;
            for (Thue t : list) {
                t.setDangApDung(t.getMaThue() == maThue);
                dao.capNhat(t);
            }
        }

        private void clearForm() {
            nameField.setText("");
            rateField.setText("");
            activeCheck.setSelected(false);
        }
    }

    private static class RolePanel extends JPanel {
        private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Tên đăng nhập", "Họ tên", "Vai trò"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        private final JTable table = new JTable(model);
        private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"quanly", "thuNgan", "phaChe"});
        private final JLabel selectedUserLabel = new JLabel("Chưa chọn");
        private final NguoiDungDAO dao = new NguoiDungDAO();

        public RolePanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            roleCombo.setEditable(true);

            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> updateSelection());
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel control = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4,4,4,4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            control.add(new JLabel("Người dùng:"), gbc);
            gbc.gridx = 1;
            control.add(selectedUserLabel, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            control.add(new JLabel("Vai trò mới:"), gbc);
            gbc.gridx = 1;
            control.add(roleCombo, gbc);

            JButton saveBtn = new JButton("Cập nhật vai trò");
            saveBtn.addActionListener(e -> saveRole());
            gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
            control.add(saveBtn, gbc);

            JButton reloadBtn = new JButton("Tải lại");
            reloadBtn.addActionListener(e -> reload());
            gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
            control.add(reloadBtn, gbc);

            add(control, BorderLayout.SOUTH);
            reload();
        }

        private void reload() {
            model.setRowCount(0);
            List<NguoiDung> list = dao.layHet();
            if (list != null) {
                for (NguoiDung n : list) {
                    model.addRow(new Object[]{n.getMaNguoiDung(), n.getTenDangNhap(), n.getHoTen(), n.getVaiTro()});
                }
            }
            selectedUserLabel.setText("Chưa chọn");
        }

        private void updateSelection() {
            int row = table.getSelectedRow();
            if (row < 0) {
                selectedUserLabel.setText("Chưa chọn");
                return;
            }
            String username = model.getValueAt(row, 1).toString();
            selectedUserLabel.setText(username);
            Object role = model.getValueAt(row, 3);
            roleCombo.setSelectedItem(role != null ? role.toString() : "");
        }

        private void saveRole() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Chọn người dùng cần cập nhật.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String newRole = roleCombo.getSelectedItem() != null ? roleCombo.getSelectedItem().toString().trim() : "";
            if (newRole.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vai trò không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int userId = Integer.parseInt(model.getValueAt(row, 0).toString());
            if (dao.capNhatVaiTro(userId, newRole)) {
                model.setValueAt(newRole, row, 3);
                JOptionPane.showMessageDialog(this, "Cập nhật vai trò thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật vai trò.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

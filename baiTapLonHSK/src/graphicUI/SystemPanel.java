package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * SystemPanel - manage system settings and user management placeholders.
 */
public class SystemPanel extends JPanel {
    private JPasswordField currentField;
    private JPasswordField newField;
    private JPasswordField confirmField;

    private JTable userTable;
    private DefaultTableModel userModel;

    public SystemPanel() {
        setLayout(new BorderLayout(8,8));
        initComponents();
    }

    private void initComponents() {
        // Top: password change
        JPanel top = new JPanel(new BorderLayout(8,8));
        top.setBorder(BorderFactory.createTitledBorder("Thiết lập chung - Thay đổi mật khẩu hệ thống"));
        JPanel form = new JPanel(new GridLayout(3,2,6,6));
        form.add(new JLabel("Mật khẩu hiện tại:"));
        currentField = new JPasswordField();
        form.add(currentField);
        form.add(new JLabel("Mật khẩu mới:"));
        newField = new JPasswordField();
        form.add(newField);
        form.add(new JLabel("Xác nhận mật khẩu mới:"));
        confirmField = new JPasswordField();
        form.add(confirmField);
        top.add(form, BorderLayout.CENTER);
        JButton btnChange = new JButton("Lưu mật khẩu");
        btnChange.addActionListener(this::onChangePassword);
        top.add(btnChange, BorderLayout.SOUTH);

        // Bottom: user management placeholder
        JPanel bottom = new JPanel(new BorderLayout(8,8));
        bottom.setBorder(BorderFactory.createTitledBorder("Quản lý người dùng và phân quyền (placeholder)"));

        userModel = new DefaultTableModel(new Object[] {"ID", "Username", "Role"}, 0);
        userTable = new JTable(userModel);
        // sample user
        userModel.addRow(new Object[] {1, "admin", "Administrator"});

        bottom.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel userButtons = new JPanel();
        userButtons.add(new JButton("Thêm"));
        userButtons.add(new JButton("Sửa"));
        userButtons.add(new JButton("Xóa"));
        bottom.add(userButtons, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
    }

    private void onChangePassword(ActionEvent e) {
        String current = new String(currentField.getPassword());
        String n = new String(newField.getPassword());
        String c = new String(confirmField.getPassword());
        if (!CaiDat.getSystemPassword().equals(current)) {
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
        CaiDat.setSystemPassword(n);
        JOptionPane.showMessageDialog(this, "Thay đổi mật khẩu thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        currentField.setText("");
        newField.setText("");
        confirmField.setText("");
    }
}

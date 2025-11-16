package graphicUI;

import dao.NguoiDungDAO;
import entity.NguoiDung;

import javax.swing.*;
import java.awt.*;

/**
 * Simple modal login dialog that authenticates users against the nguoiDung table.
 */
public class DangNhapDialog extends JDialog {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private NguoiDung authenticatedUser;

    public DangNhapDialog(Window owner) {
        super(owner, "Đăng nhập hệ thống", ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    private void initComponents() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        form.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        JButton loginBtn = new JButton("Đăng nhập");
        JButton cancelBtn = new JButton("Thoát");
        JPanel buttons = new JPanel();
        buttons.add(loginBtn);
        buttons.add(cancelBtn);

        loginBtn.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        cancelBtn.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập đầy đủ tên đăng nhập và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        NguoiDungDAO dao = new NguoiDungDAO();
        NguoiDung user = dao.layTheoTenDangNhap(username);
        if (user == null || user.getMatKhau() == null || !user.getMatKhau().equals(password)) {
            JOptionPane.showMessageDialog(this, "Sai tên đăng nhập hoặc mật khẩu.", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            return;
        }

        authenticatedUser = user;
        dispose();
    }

    public NguoiDung showDialog() {
        setVisible(true);
        return authenticatedUser;
    }
}

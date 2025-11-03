package graphicUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * PasswordDialog prompts the user for the system password and returns whether authentication succeeded.
 */
public class PasswordDialog extends JDialog {
    private JPasswordField passwordField;
    private boolean authenticated = false;

    private PasswordDialog(Window owner) {
        super(owner, "Xác thực mật khẩu hệ thống", ModalityType.APPLICATION_MODAL);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.add(new JLabel("Nhập mật khẩu hệ thống:"), BorderLayout.NORTH);
        passwordField = new JPasswordField(20);
        p.add(passwordField, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Hủy");
        buttons.add(ok);
        buttons.add(cancel);
        p.add(buttons, BorderLayout.SOUTH);

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String entered = new String(passwordField.getPassword());
                if (AppConfig.getSystemPassword().equals(entered)) {
                    authenticated = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(PasswordDialog.this, "Mật khẩu sai.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                }
            }
        });

        cancel.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        getContentPane().add(p);
        getRootPane().setDefaultButton(ok);
    }

    /**
     * Show the dialog and return true if authentication succeeded.
     */
    public static boolean authenticate(Window owner) {
        PasswordDialog d = new PasswordDialog(owner);
        d.setVisible(true);
        return d.authenticated;
    }
}

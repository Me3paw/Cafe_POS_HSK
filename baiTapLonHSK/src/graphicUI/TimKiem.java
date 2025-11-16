package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * SearchPanel provides tabs for invoice, customer, and employee search.
 * Employee search is restricted to admin sessions.
 */
public class TimKiem extends JPanel {
    private JTabbedPane tabs;
    private Component owner;

    public TimKiem(Component owner) {
        this.owner = owner;
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Tìm hóa đơn", new InvoiceSearchPanel());
        tabs.addTab("Tìm khách hàng", new CustomerSearchPanel());
        tabs.addTab("Tìm nhân viên", buildEmployeeTab()); // admin-only
        add(tabs, BorderLayout.CENTER);
    }

    private Component buildEmployeeTab() {
        if (SessionContext.isAdmin()) {
            return new EmployeeSearchPanel();
        }
        JPanel locked = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("<html>Chức năng tìm nhân viên chỉ dành cho tài khoản quản trị.<br>Đăng nhập với quyền admin để tiếp tục.</html>");
        lbl.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
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
        public InvoiceSearchPanel() {
            setLayout(new BorderLayout(8,8));
            DefaultTableModel m = new DefaultTableModel(new Object[] {"Số hóa đơn","Ngày","Khách","Tổng"},0);
            JTable t = new JTable(m);
            m.addRow(new Object[] {"INV001","2025-11-03","Nguyễn A","150000"});
            add(new JScrollPane(t), BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(new JTextField(20));
            p.add(new JButton("Tìm"));
            add(p, BorderLayout.NORTH);
        }
    }

    static class CustomerSearchPanel extends JPanel {
        public CustomerSearchPanel() {
            setLayout(new BorderLayout(8,8));
            DefaultTableModel m = new DefaultTableModel(new Object[] {"Mã","Tên","SĐT"},0);
            JTable t = new JTable(m);
            m.addRow(new Object[] {"C001","Khách lẻ","0123456789"});
            add(new JScrollPane(t), BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(new JTextField(20));
            p.add(new JButton("Tìm"));
            add(p, BorderLayout.NORTH);
        }
    }

    static class EmployeeSearchPanel extends JPanel {
        public EmployeeSearchPanel() {
            setLayout(new BorderLayout(8,8));
            DefaultTableModel m = new DefaultTableModel(new Object[] {"Mã NV","Tên","Vị trí"},0);
            JTable t = new JTable(m);
            m.addRow(new Object[] {"E001","Nguyễn Văn A","Thu ngân"});
            add(new JScrollPane(t), BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(new JTextField(20));
            p.add(new JButton("Tìm"));
            add(p, BorderLayout.NORTH);
        }
    }
}

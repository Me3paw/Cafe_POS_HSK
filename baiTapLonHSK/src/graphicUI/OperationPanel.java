package graphicUI;

import javax.swing.*;
import java.awt.*;

/**
 * OperationPanel groups payment, printing, refunds, transfer/cancel operations.
 * Refund and some actions require password authentication.
 */
public class OperationPanel extends JPanel {
    private JTabbedPane tabs;
    private Component owner;

    public OperationPanel(Component owner) {
        this.owner = owner;
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Thanh toán", new PaymentPanel());
        tabs.addTab("In hóa đơn", new PrintPanel());
        tabs.addTab("Hoàn tiền", buildRefundTab()); // protected
        tabs.addTab("Chuyển bàn / Hủy đơn", new TransferCancelPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private Component buildRefundTab() {
        JPanel locked = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("<html>Hoàn tiền là chức năng nâng cao và cần xác thực mật khẩu hệ thống.</html>");
        lbl.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JButton btn = new JButton("Xác thực để mở chức năng hoàn tiền");
        btn.addActionListener(e -> {
            boolean ok = PasswordDialog.authenticate(SwingUtilities.getWindowAncestor(this));
            if (ok) {
                int idx = tabs.indexOfComponent(locked);
                if (idx >= 0) {
                    tabs.setComponentAt(idx, new RefundPanel());
                    tabs.setTitleAt(idx, "Hoàn tiền");
                }
            }
        });
        locked.add(lbl, BorderLayout.CENTER);
        locked.add(btn, BorderLayout.SOUTH);
        return locked;
    }

    static class PaymentPanel extends JPanel {
        public PaymentPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("Thanh toán (placeholder)"), BorderLayout.NORTH);
            add(new JPanel(), BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(new JButton("Thanh toán bằng tiền mặt"));
            p.add(new JButton("Thanh toán bằng thẻ"));
            add(p, BorderLayout.SOUTH);
        }
    }

    static class PrintPanel extends JPanel {
        public PrintPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("In hóa đơn (placeholder)"), BorderLayout.NORTH);
            JPanel p = new JPanel();
            p.add(new JButton("Chọn hóa đơn"));
            p.add(new JButton("In"));
            add(p, BorderLayout.CENTER);
        }
    }

    static class RefundPanel extends JPanel {
        public RefundPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("Hoàn tiền (đã xác thực) - placeholder"), BorderLayout.NORTH);
            JPanel p = new JPanel();
            p.add(new JTextField(20));
            p.add(new JButton("Tìm hóa đơn"));
            p.add(new JButton("Hoàn tiền"));
            add(p, BorderLayout.CENTER);
        }
    }

    static class TransferCancelPanel extends JPanel {
        public TransferCancelPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("Chuyển bàn / Hủy đơn (placeholder)"), BorderLayout.NORTH);
            JPanel p = new JPanel();
            p.add(new JButton("Chuyển bàn"));
            p.add(new JButton("Hủy đơn"));
            add(p, BorderLayout.CENTER);
        }
    }
}

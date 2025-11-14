package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

import components.TableLayoutPanel;

/**
 * UpdatePanel contains subpages: Order (default), Table status, Price/Stock updates.
 */
public class UpdatePanel extends JPanel {
    private JTabbedPane tabs;
    private OrderPanel orderPanel;

    public UpdatePanel() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        orderPanel = new OrderPanel();
        tabs.addTab("Đơn hàng mới", orderPanel);
        tabs.addTab("Cập nhật trạng thái bàn", new TableStatusPanel());
        tabs.addTab("Cập nhật giá / tồn kho", new PriceStockPanel());
        add(tabs, BorderLayout.CENTER);
    }

    public void showDefault() {
        tabs.setSelectedIndex(0);
    }

    // Order panel (placeholder)
    static class OrderPanel extends JPanel {
        private DefaultTableModel orderModel;
        private JTable orderTable;

        public OrderPanel() {
            setLayout(new BorderLayout(8,8));
            orderModel = new DefaultTableModel(new Object[] {"Mã ĐH", "Bàn", "Items", "Tổng"}, 0);
            orderTable = new JTable(orderModel);
            orderModel.addRow(new Object[] {"ORD001", "B1", "Cà phê x1", "25000"});
            add(new JScrollPane(orderTable), BorderLayout.CENTER);

            JPanel actions = new JPanel();
            JButton add = new JButton("Tạo đơn mới");
            JButton updateStatus = new JButton("Cập nhật trạng thái");
            actions.add(add); actions.add(updateStatus);
            add(actions, BorderLayout.SOUTH);

            add.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(this, "Tạo đơn: placeholder dialog");
            });
            updateStatus.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(this, "Cập nhật trạng thái bàn: placeholder");
            });
        }
    }

    static class TableStatusPanel extends JPanel {
        public TableStatusPanel() {
            setLayout(new BorderLayout());
            // Only show the TableLayoutPanel as the full content
            add(new TableLayoutPanel(), BorderLayout.CENTER);
        }
    }

    static class PriceStockPanel extends JPanel {
        public PriceStockPanel() {
            setLayout(new BorderLayout(8,8));
            String[] cols = {"Mã", "Tên", "Giá", "Tồn kho"};
            Object[][] rows = { {"P001","Cà phê sữa","25000","20"} };
            JTable t = new JTable(rows, cols);
            add(new JScrollPane(t), BorderLayout.CENTER);
            JPanel p = new JPanel();
            p.add(new JButton("Cập nhật giá"));
            p.add(new JButton("Cập nhật tồn kho"));
            add(p, BorderLayout.SOUTH);
        }
    }
}
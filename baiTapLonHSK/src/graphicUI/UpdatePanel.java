package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import components.TableLayoutPanel;

/**
 * UpdatePanel contains subpages: Order (default), Table status, Price/Stock updates.
 */
public class UpdatePanel extends JPanel {
    private JTabbedPane tabs;
    private OrderPanel orderPanel;
    private final TableLayoutPanel.TableModel tableModel;

    public UpdatePanel(TableLayoutPanel.TableModel tableModel) {
        this.tableModel = tableModel;
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

    // Order panel: includes a TableLayoutPanel in ORDER_MODE and an order list
    static class OrderPanel extends JPanel {
        private DefaultTableModel orderModel;
        private JTable orderTable;
        private AtomicInteger orderCounter = new AtomicInteger(1);

        public OrderPanel() {
            setLayout(new BorderLayout(8,8));

            // Orders list only (no left table map)
            orderModel = new DefaultTableModel(new Object[] {"Mã ĐH", "Bàn", "Items", "Tổng"}, 0);
            orderTable = new JTable(orderModel);
            JScrollPane ordersScroll = new JScrollPane(orderTable);
            add(ordersScroll, BorderLayout.CENTER);

            JPanel actions = new JPanel();
            JButton add = new JButton("Tạo đơn mới");
            JButton updateStatus = new JButton("Cập nhật trạng thái");
            actions.add(add); actions.add(updateStatus);
            add(actions, BorderLayout.SOUTH);

            // When creating a new order, open a modal dialog that lets user pick a table
            add.addActionListener((ActionEvent e) -> {
                // Create a chooser TableLayoutPanel using the shared model from the outer UpdatePanel
                TableLayoutPanel chooser = new TableLayoutPanel(TableLayoutPanel.Mode.ORDER_MODE, ((UpdatePanel)SwingUtilities.getAncestorOfClass(UpdatePanel.class, this)).tableModel);
                // Disable auto-occupy in the chooser; we'll occupy after confirmation
                chooser.setAutoOccupyOnOrder(false);

                // Dialog setup (modal chooser)
                Window parent = SwingUtilities.getWindowAncestor(this);
                JDialog dlg = new JDialog(parent, "Chọn bàn cho đơn mới", Dialog.ModalityType.APPLICATION_MODAL);
                dlg.getContentPane().setLayout(new BorderLayout());
                dlg.getContentPane().add(chooser, BorderLayout.CENTER);

                // Optionally show an instruction label
                JLabel info = new JLabel("Nhấn vào một bàn để chọn, sau đó xác nhận.");
                info.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
                dlg.getContentPane().add(info, BorderLayout.SOUTH);

                dlg.pack();
                dlg.setLocationRelativeTo(this);

                // Listener: when a table is clicked in chooser, confirm and then occupy + add order
                chooser.addTableSelectionListener(table -> {
                    SwingUtilities.invokeLater(() -> {
                        // If table is already occupied, warn and ignore
                        // Check if the table status is either "Occupied" OR "Reserved" (case-insensitive)
                        if ("Occupied".equalsIgnoreCase(table.status) || "Reserved".equalsIgnoreCase(table.status)) {

                            // Determine the specific message based on the status
                            String message;
                            String title = "Bàn không có sẵn"; // "Table not available"

                            if ("Occupied".equalsIgnoreCase(table.status)) {
                                message = "Bàn " + table.name + " đang được sử dụng."; // "Table... is being used."
                            } else {
                                message = "Bàn " + table.name + " đã được đặt trước."; // "Table... is reserved."
                            }

                            JOptionPane.showMessageDialog(dlg,
                                    message,
                                    title,
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        int ans = JOptionPane.showConfirmDialog(dlg,
                                "Xác nhận chọn bàn " + table.name + " cho đơn mới?",
                                "Xác nhận chọn bàn",
                                JOptionPane.YES_NO_OPTION);
                        if (ans == JOptionPane.YES_OPTION) {
                            // Occupy the table and repaint the chooser (no main map on the left)
                            table.status = "Occupied";
                            chooser.repaint();

                            // Add order row and keep its index so we can update total later
                            String id = String.format("ORD%03d", orderCounter.getAndIncrement());
                            orderModel.addRow(new Object[] {id, table.name, "", "0"});
                            int newRow = orderModel.getRowCount() - 1;

                            // Close chooser dialog
                            dlg.dispose();

                            // Ensure the main orders table shows and selects the newly created order
                            SwingUtilities.invokeLater(() -> {
                                orderTable.setRowSelectionInterval(newRow, newRow);
                                orderTable.scrollRectToVisible(orderTable.getCellRect(newRow, 0, true));
                            });

                            // Open a non-modal order editor window to render the order fully
                            openOrderEditorWindow(id, table, newRow);
                        }
                    });
                });

                dlg.setVisible(true);
            });

            updateStatus.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(this, "Cập nhật trạng thái bàn: mở tab 'Cập nhật trạng thái bàn'.");
            });
        }

        // Opens a simple non-modal order editor where user can add items and update total
        private void openOrderEditorWindow(String orderId, TableLayoutPanel.CafeTable table, int orderRowIndex) {
            Window parent = SwingUtilities.getWindowAncestor(this);
            JDialog win = new JDialog(parent, "Đơn hàng - " + orderId, Dialog.ModalityType.MODELESS);
            win.getContentPane().setLayout(new BorderLayout(8,8));

            // Header with order id and table
            JPanel header = new JPanel(new GridLayout(2,1));
            header.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            header.add(new JLabel("Mã ĐH: " + orderId));
            header.add(new JLabel("Bàn: " + table.name));
            win.getContentPane().add(header, BorderLayout.NORTH);

            // Items table
            DefaultTableModel itemsModel = new DefaultTableModel(new Object[] {"Mã","Tên","SL","Giá","Thành tiền"}, 0);
            JTable itemsTable = new JTable(itemsModel);
            win.getContentPane().add(new JScrollPane(itemsTable), BorderLayout.CENTER);

            // Bottom panel with add item, total, save/close
            JPanel bottom = new JPanel(new BorderLayout());
            JPanel buttons = new JPanel();
            JButton addItem = new JButton("Thêm mặt hàng");
            JButton saveClose = new JButton("Lưu và đóng");
            buttons.add(addItem);
            buttons.add(saveClose);
            bottom.add(buttons, BorderLayout.WEST);

            JLabel totalLabel = new JLabel("Tổng: 0");
            totalLabel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            bottom.add(totalLabel, BorderLayout.EAST);
            win.getContentPane().add(bottom, BorderLayout.SOUTH);

            // Helper to recalc total and push to main orderModel
            Runnable recalcTotal = () -> {
                long total = 0;
                for (int r = 0; r < itemsModel.getRowCount(); r++) {
                    Object v = itemsModel.getValueAt(r, 4);
                    if (v instanceof Number) total += ((Number) v).longValue();
                    else {
                        try { total += Long.parseLong(v.toString()); } catch (Exception ex) {}
                    }
                }
                String totalStr = String.valueOf(total);
                totalLabel.setText("Tổng: " + totalStr);
                // update main orderModel total column
                if (orderRowIndex >= 0 && orderRowIndex < orderModel.getRowCount()) {
                    orderModel.setValueAt(totalStr, orderRowIndex, 3);
                }
            };

            addItem.addActionListener(ae -> {
                String code = JOptionPane.showInputDialog(win, "Mã mặt hàng:", "Thêm mặt hàng", JOptionPane.PLAIN_MESSAGE);
                if (code == null) return;
                String name = JOptionPane.showInputDialog(win, "Tên mặt hàng:", "", JOptionPane.PLAIN_MESSAGE);
                if (name == null) return;
                String qtyS = JOptionPane.showInputDialog(win, "Số lượng:", "1");
                if (qtyS == null) return;
                String priceS = JOptionPane.showInputDialog(win, "Đơn giá (số nguyên):", "0");
                if (priceS == null) return;
                try {
                    int qty = Integer.parseInt(qtyS);
                    long price = Long.parseLong(priceS);
                    long line = qty * price;
                    itemsModel.addRow(new Object[] {code, name, qty, price, line});
                    recalcTotal.run();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(win, "Giá trị số không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });

            saveClose.addActionListener(ae -> win.dispose());

            win.pack();
            win.setLocationRelativeTo(this);
            win.setVisible(true);
        }
    }

    class TableStatusPanel extends JPanel {
        public TableStatusPanel() {
            setLayout(new BorderLayout());
            // Only show the TableLayoutPanel as the full content using the shared model
            add(new TableLayoutPanel(TableLayoutPanel.Mode.STATUS_MODE, UpdatePanel.this.tableModel), BorderLayout.CENTER);
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
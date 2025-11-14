package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        private final DefaultTableModel productModel;
        private final DefaultTableModel historyModel;
        private final JTable productTable;
        private final JTable historyTable;
        private final JTextField totalField = new JTextField(10);
        private final JTextField cashField = new JTextField(10);
        private final JLabel changeLabel = new JLabel("Tiền thối lại: 0 đ");
        private final JLabel timeLabel = new JLabel("Chưa thanh toán");
        private int transactionCounter = 1;

        public PaymentPanel() {
            setLayout(new BorderLayout(15, 15));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setBackground(new Color(250, 250, 255));

            // ===== TIÊU ĐỀ =====
            JLabel title = new JLabel("Thanh toán hóa đơn", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(new Color(30, 60, 114));
            add(title, BorderLayout.NORTH);

            // ===== BẢNG SẢN PHẨM =====
            String[] productCols = {"Mã SP", "Tên sản phẩm", "Số lượng", "Giá (đ)", "Thành tiền (đ)"};
            productModel = new DefaultTableModel(productCols, 0);
            productTable = new JTable(productModel);
            productTable.setFillsViewportHeight(true);
            JScrollPane scrollProducts = new JScrollPane(productTable);
            scrollProducts.setBorder(BorderFactory.createTitledBorder("Danh sách sản phẩm"));

            // Panel chứa bảng + nút thêm/xóa
            JPanel productPanel = new JPanel(new BorderLayout(8, 8));
            JPanel productBtns = new JPanel();
            JButton addProduct = new JButton("Thêm sản phẩm");
            JButton removeProduct = new JButton("Xóa dòng");
            productBtns.add(addProduct);
            productBtns.add(removeProduct);
            productPanel.add(scrollProducts, BorderLayout.CENTER);
            productPanel.add(productBtns, BorderLayout.SOUTH);

            // ===== KHU VỰC TÍNH TOÁN =====
            JPanel paymentInfo = new JPanel(new GridLayout(4, 2, 8, 8));
            paymentInfo.setBorder(BorderFactory.createTitledBorder("Thông tin thanh toán"));
            paymentInfo.add(new JLabel("Tổng tiền:"));
            totalField.setEditable(false);
            paymentInfo.add(totalField);
            paymentInfo.add(new JLabel("Khách đưa:"));
            paymentInfo.add(cashField);
            paymentInfo.add(new JLabel("Tiền thối lại:"));
            paymentInfo.add(changeLabel);
            paymentInfo.add(new JLabel("Thời gian thanh toán:"));
            paymentInfo.add(timeLabel);

            // ===== NÚT THANH TOÁN =====
            JPanel payButtons = new JPanel();
            JButton cashBtn = new JButton("Tiền mặt");
            JButton cardBtn = new JButton("Thẻ");
            payButtons.add(cashBtn);
            payButtons.add(cardBtn);

            // ===== LỊCH SỬ GIAO DỊCH =====
            String[] historyCols = {"Mã GD", "Thời gian", "Tổng tiền (đ)"};
            historyModel = new DefaultTableModel(historyCols, 0);
            historyTable = new JTable(historyModel);
            JScrollPane scrollHistory = new JScrollPane(historyTable);
            scrollHistory.setBorder(BorderFactory.createTitledBorder("🧾 Lịch sử giao dịch"));

            // ===== GỘP PHẦN TRUNG TÂM =====
            JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
            centerPanel.add(productPanel, BorderLayout.CENTER);
            centerPanel.add(paymentInfo, BorderLayout.SOUTH);
            add(centerPanel, BorderLayout.CENTER);

            add(payButtons, BorderLayout.SOUTH);
            add(scrollHistory, BorderLayout.EAST);

            // ===== SỰ KIỆN =====
            addProduct.addActionListener(e -> addProductRow());
            removeProduct.addActionListener(e -> removeSelectedProduct());
            cashBtn.addActionListener(e -> handlePayment("Tiền mặt"));
            cardBtn.addActionListener(e -> handlePayment("Thẻ"));
        }

        private void addProductRow() {
            JTextField id = new JTextField();
            JTextField name = new JTextField();
            JTextField qty = new JTextField("1");
            JTextField price = new JTextField();

            JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
            panel.add(new JLabel("Mã SP:"));
            panel.add(id);
            panel.add(new JLabel("Tên SP:"));
            panel.add(name);
            panel.add(new JLabel("Số lượng:"));
            panel.add(qty);
            panel.add(new JLabel("Giá:"));
            panel.add(price);

            int result = JOptionPane.showConfirmDialog(this, panel, "Thêm sản phẩm", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int quantity = Integer.parseInt(qty.getText());
                    double unitPrice = Double.parseDouble(price.getText());
                    double total = quantity * unitPrice;
                    productModel.addRow(new Object[]{id.getText(), name.getText(), quantity, unitPrice, total});
                    updateTotal();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Giá và số lượng phải là số hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void removeSelectedProduct() {
            int row = productTable.getSelectedRow();
            if (row >= 0) {
                productModel.removeRow(row);
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng cần xóa!");
            }
        }

        private void updateTotal() {
            double sum = 0;
            for (int i = 0; i < productModel.getRowCount(); i++) {
                sum += Double.parseDouble(productModel.getValueAt(i, 4).toString());
            }
            totalField.setText(String.format("%,.0f", sum));
        }

        private void handlePayment(String method) {
            if (productModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Chưa có sản phẩm trong hóa đơn!");
                return;
            }

            try {
                double total = Double.parseDouble(totalField.getText().replace(",", ""));
                double cash = Double.parseDouble(cashField.getText());
                if (cash < total) {
                    JOptionPane.showMessageDialog(this, "Khách đưa chưa đủ tiền!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double change = cash - total;
                changeLabel.setText(String.format("%,.0f đ", change));

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
                timeLabel.setText(now.format(fmt));

                // Ghi lịch sử
                String id = "GD" + String.format("%03d", transactionCounter++);
                historyModel.addRow(new Object[]{id, now.format(fmt), String.format("%,.0f", total)});

                JOptionPane.showMessageDialog(this,
                        "Thanh toán thành công bằng " + method + "\n" +
                                "Tổng tiền: " + total + " đ\n" +
                                "Tiền thối lại: " + change + " đ\n" +
                                "Thời gian: " + now.format(fmt),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);

                // Reset bảng sản phẩm
                productModel.setRowCount(0);
                updateTotal();
                cashField.setText("");
                changeLabel.setText("Tiền thối lại: 0 đ");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    static class PrintPanel extends JPanel {
        private final JTextArea invoiceArea;
        private final JTable invoiceTable;
        private final DefaultTableModel invoiceModel;
        private final JLabel timeLabel = new JLabel("Chưa chọn hóa đơn");

        public PrintPanel() {
            setLayout(new BorderLayout(15, 15));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setBackground(new Color(250, 250, 255));

            // ==== TIÊU ĐỀ ====
            JLabel title = new JLabel("In hóa đơn", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(new Color(40, 70, 140));
            add(title, BorderLayout.NORTH);

            // ==== BẢNG DANH SÁCH HÓA ĐƠN ====
            String[] cols = {"Mã HĐ", "Bàn", "Tổng tiền", "Trạng thái"};
            invoiceModel = new DefaultTableModel(cols, 0);
            invoiceTable = new JTable(invoiceModel);
            invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JScrollPane tableScroll = new JScrollPane(invoiceTable);
            tableScroll.setBorder(BorderFactory.createTitledBorder("Danh sách hóa đơn"));
            add(tableScroll, BorderLayout.WEST);

            // ==== VÙNG XEM TRƯỚC HÓA ĐƠN ====
            invoiceArea = new JTextArea(18, 30);
            invoiceArea.setFont(new Font("Consolas", Font.PLAIN, 13));
            invoiceArea.setEditable(false);
            invoiceArea.setBorder(BorderFactory.createTitledBorder("Xem trước hóa đơn"));
            add(new JScrollPane(invoiceArea), BorderLayout.CENTER);

            // ==== PANEL DƯỚI (THỜI GIAN + NÚT) ====
            JPanel bottom = new JPanel(new BorderLayout(10, 10));
            bottom.setOpaque(false);

            // Nhãn thời gian
            timeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            timeLabel.setForeground(Color.DARK_GRAY);
            bottom.add(timeLabel, BorderLayout.WEST);

            // Nút hành động
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            btnPanel.setOpaque(false);
            JButton loadBtn = new JButton("Chọn hóa đơn");
            JButton printBtn = new JButton("In hóa đơn");
            styleButton(loadBtn);
            styleButton(printBtn);
            btnPanel.add(loadBtn);
            btnPanel.add(printBtn);
            bottom.add(btnPanel, BorderLayout.EAST);

            add(bottom, BorderLayout.SOUTH);

            // ==== DỮ LIỆU MẪU ====
            loadSampleInvoices();

            // ==== SỰ KIỆN ====
            loadBtn.addActionListener(e -> handleSelect());
            printBtn.addActionListener(e -> handlePrint());
        }

        private void styleButton(JButton btn) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(220, 235, 255));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 200, 240)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        }

        private void loadSampleInvoices() {
            invoiceModel.addRow(new Object[]{"ORD001", "Bàn 1", "125.000đ", "Đã thanh toán"});
            invoiceModel.addRow(new Object[]{"ORD002", "Bàn 2", "75.000đ", "Đã thanh toán"});
            invoiceModel.addRow(new Object[]{"ORD003", "Bàn 3", "215.000đ", "Chưa in"});
        }

        private void handleSelect() {
            int row = invoiceTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String code = invoiceModel.getValueAt(row, 0).toString();
            String table = invoiceModel.getValueAt(row, 1).toString();
            String total = invoiceModel.getValueAt(row, 2).toString();
            String status = invoiceModel.getValueAt(row, 3).toString();

            invoiceArea.setText(
                    "************ CAFE POS ************\n" +
                            "Mã Hóa Đơn: " + code + "\n" +
                            "Bàn: " + table + "\n" +
                            "----------------------------------\n" +
                            "Cà phê sữa x1 ............. 25.000đ\n" +
                            "Trà đào x1 ................. 30.000đ\n" +
                            "----------------------------------\n" +
                            "TỔNG CỘNG: " + total + "\n" +
                            "Trạng thái: " + status + "\n" +
                            "----------------------------------\n" +
                            "Cảm ơn quý khách!\n"
            );

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
            timeLabel.setText("Đang xem hóa đơn " + code + " | " + now.format(fmt));
        }

        private void handlePrint() {
            if (invoiceArea.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa có hóa đơn nào để in!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Giả lập hành động in — lưu file txt
                File outFile = new File("hoadon_" + System.currentTimeMillis() + ".txt");
                try (PrintWriter writer = new PrintWriter(outFile)) {
                    writer.print(invoiceArea.getText());
                }
                JOptionPane.showMessageDialog(this, "Đã gửi lệnh in (giả lập)\nTệp: " + outFile.getName(), "Thành công", JOptionPane.INFORMATION_MESSAGE);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
                timeLabel.setText("Đã in lúc " + now.format(fmt));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi in hóa đơn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class RefundPanel extends JPanel {
        private final JTextField searchField = new JTextField(15);
        private final JTable invoiceTable;
        private final JTable detailTable;
        private final DefaultTableModel invoiceModel;
        private final DefaultTableModel detailModel;
        private final JTextArea noteArea = new JTextArea(3, 20);

        public RefundPanel() {
            setLayout(new BorderLayout(15, 15));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setBackground(new Color(250, 250, 255));

            JLabel title = new JLabel("Hoàn tiền hóa đơn", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(new Color(70, 90, 150));
            add(title, BorderLayout.NORTH);

            // === Khu vực tìm kiếm hóa đơn ===
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            searchPanel.setOpaque(false);
            searchPanel.add(new JLabel("Tìm hóa đơn:"));
            searchPanel.add(searchField);
            JButton searchBtn = new JButton("Tìm");
            styleButton(searchBtn);
            searchPanel.add(searchBtn);
            add(searchPanel, BorderLayout.NORTH);

            // === Bảng danh sách hóa đơn ===
            String[] invoiceCols = {"Mã hóa đơn", "Bàn", "Tổng tiền", "Ngày thanh toán"};
            invoiceModel = new DefaultTableModel(invoiceCols, 0);
            invoiceTable = new JTable(invoiceModel);
            invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane invoiceScroll = new JScrollPane(invoiceTable);
            invoiceScroll.setBorder(BorderFactory.createTitledBorder("Danh sách hóa đơn"));

            // === Bảng chi tiết sản phẩm ===
            String[] detailCols = {"Tên món", "Số lượng", "Đơn giá", "Thành tiền"};
            detailModel = new DefaultTableModel(detailCols, 0);
            detailTable = new JTable(detailModel);
            JScrollPane detailScroll = new JScrollPane(detailTable);
            detailScroll.setBorder(BorderFactory.createTitledBorder("Chi tiết hóa đơn"));

            // Panel trung tâm chia 2 phần: hóa đơn và chi tiết
            JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, invoiceScroll, detailScroll);
            centerSplit.setResizeWeight(0.5);
            add(centerSplit, BorderLayout.CENTER);

            // === Ghi chú và nút hoàn tiền ===
            JPanel bottom = new JPanel(new BorderLayout(10, 10));
            bottom.setOpaque(false);
            noteArea.setBorder(BorderFactory.createTitledBorder("Ghi chú / Lý do hoàn tiền"));
            bottom.add(new JScrollPane(noteArea), BorderLayout.CENTER);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton refundBtn = new JButton("Hoàn tiền");
            styleButton(refundBtn);
            btnPanel.add(refundBtn);
            bottom.add(btnPanel, BorderLayout.SOUTH);

            add(bottom, BorderLayout.SOUTH);

            // === Dữ liệu mẫu để demo ===
            loadSampleInvoices();

            // === Sự kiện ===
            searchBtn.addActionListener(e -> searchInvoice());
            invoiceTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showInvoiceDetails();
            });
            refundBtn.addActionListener(e -> processRefund());
        }

        private void styleButton(JButton btn) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(220, 235, 255));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 200, 240)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        }

        private void loadSampleInvoices() {
            invoiceModel.addRow(new Object[]{"HD001", "Bàn 3", "120.000đ", "10/11/2025"});
            invoiceModel.addRow(new Object[]{"HD002", "Bàn 7", "85.000đ", "11/11/2025"});
            invoiceModel.addRow(new Object[]{"HD003", "Mang đi", "45.000đ", "12/11/2025"});
        }

        private void searchInvoice() {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập mã hoặc từ khóa để tìm hóa đơn.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (int i = 0; i < invoiceModel.getRowCount(); i++) {
                String id = invoiceModel.getValueAt(i, 0).toString().toLowerCase();
                if (id.contains(query)) {
                    invoiceTable.setRowSelectionInterval(i, i);
                    invoiceTable.scrollRectToVisible(invoiceTable.getCellRect(i, 0, true));
                    showInvoiceDetails();
                    return;
                }
            }

            JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn phù hợp.", "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showInvoiceDetails() {
            int row = invoiceTable.getSelectedRow();
            if (row < 0) return;
            detailModel.setRowCount(0);

            String id = invoiceModel.getValueAt(row, 0).toString();
            // Dữ liệu mẫu
            switch (id) {
                case "HD001" -> {
                    detailModel.addRow(new Object[]{"Cà phê sữa", 2, "25.000đ", "50.000đ"});
                    detailModel.addRow(new Object[]{"Bánh ngọt", 1, "70.000đ", "70.000đ"});
                }
                case "HD002" -> {
                    detailModel.addRow(new Object[]{"Trà đào", 1, "45.000đ", "45.000đ"});
                    detailModel.addRow(new Object[]{"Bánh mì bơ tỏi", 2, "20.000đ", "40.000đ"});
                }
                case "HD003" -> {
                    detailModel.addRow(new Object[]{"Cà phê đen", 1, "25.000đ", "25.000đ"});
                    detailModel.addRow(new Object[]{"Nước suối", 1, "20.000đ", "20.000đ"});
                }
            }
        }

        private void processRefund() {
            int selectedRow = invoiceTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn cần hoàn tiền!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String note = noteArea.getText().trim();
            if (note.isEmpty()) {
                int opt = JOptionPane.showConfirmDialog(this, "Bạn chưa nhập lý do hoàn tiền. Tiếp tục?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            String id = invoiceModel.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hoàn tiền cho hóa đơn " + id + "?",
                    "Xác nhận hoàn tiền", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this,
                        "Hoàn tiền thành công cho hóa đơn " + id + "!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                noteArea.setText("");
                detailModel.setRowCount(0);
            }
        }
    }

    static class TransferCancelPanel extends JPanel {
        private final JTable orderTable;
        private final DefaultTableModel orderModel;
        private final JTextField newTableField = new JTextField(10);
        private final JTextArea noteArea = new JTextArea(3, 20);
        private final JLabel timeLabel = new JLabel("");

        public TransferCancelPanel() {
            setLayout(new BorderLayout(15, 15));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            setBackground(new Color(250, 250, 255));

            // ==== TIÊU ĐỀ ====
            JLabel title = new JLabel("Chuyển bàn / Hủy đơn", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(new Color(60, 80, 150));
            add(title, BorderLayout.NORTH);

            // ==== BẢNG DANH SÁCH ĐƠN HÀNG ====
            String[] cols = {"Mã đơn", "Bàn hiện tại", "Tổng tiền", "Trạng thái"};
            orderModel = new DefaultTableModel(cols, 0);
            orderTable = new JTable(orderModel);
            orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane tableScroll = new JScrollPane(orderTable);
            tableScroll.setBorder(BorderFactory.createTitledBorder("Danh sách đơn hàng"));
            add(tableScroll, BorderLayout.CENTER);

            // ==== PANEL DƯỚI (GỒM FORM + NÚT + THỜI GIAN) ====
            JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
            bottomPanel.setOpaque(false);

            // ---- FORM NHẬP ----
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel newTableLbl = new JLabel("Chuyển sang bàn:");
            JLabel noteLbl = new JLabel("Ghi chú / Lý do hủy:");
            styleLabel(newTableLbl);
            styleLabel(noteLbl);

            gbc.gridx = 0; gbc.gridy = 0;
            formPanel.add(newTableLbl, gbc);
            gbc.gridx = 1;
            formPanel.add(newTableField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            gbc.gridwidth = 2;
            noteArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            formPanel.add(new JScrollPane(noteArea), gbc);

            bottomPanel.add(formPanel, BorderLayout.CENTER);

            // ---- NÚT CHỨC NĂNG ----
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            btnPanel.setOpaque(false);
            JButton transferBtn = new JButton("Chuyển bàn");
            JButton cancelBtn = new JButton("Hủy đơn");
            styleButton(transferBtn);
            styleButton(cancelBtn);
            btnPanel.add(transferBtn);
            btnPanel.add(cancelBtn);
            bottomPanel.add(btnPanel, BorderLayout.SOUTH);

            // ---- NHÃN THỜI GIAN ----
            timeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            timeLabel.setForeground(Color.DARK_GRAY);
            timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            bottomPanel.add(timeLabel, BorderLayout.NORTH);

            add(bottomPanel, BorderLayout.SOUTH);

            // ==== DỮ LIỆU MẪU ====
            loadSampleOrders();

            // ==== SỰ KIỆN ====
            transferBtn.addActionListener(e -> handleTransfer());
            cancelBtn.addActionListener(e -> handleCancel());
        }

        private void styleLabel(JLabel lbl) {
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }

        private void styleButton(JButton btn) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(220, 235, 255));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 200, 240)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        }

        private void loadSampleOrders() {
            orderModel.addRow(new Object[]{"ORD001", "Bàn 1", "125.000đ", "Đang phục vụ"});
            orderModel.addRow(new Object[]{"ORD002", "Bàn 3", "210.000đ", "Đã thanh toán"});
            orderModel.addRow(new Object[]{"ORD003", "Bàn 6", "80.000đ", "Đang phục vụ"});
        }

        private void handleTransfer() {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn đơn hàng để chuyển bàn!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String newTable = newTableField.getText().trim();
            if (newTable.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập bàn mới để chuyển!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String orderId = orderModel.getValueAt(row, 0).toString();
            String oldTable = orderModel.getValueAt(row, 1).toString();

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận chuyển đơn " + orderId + " từ " + oldTable + " sang " + newTable + "?",
                    "Xác nhận chuyển bàn", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                orderModel.setValueAt(newTable, row, 1);
                updateTimeLabel("Chuyển bàn " + orderId + " thành công!");
            }
        }

        private void handleCancel() {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn đơn hàng để hủy!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String note = noteArea.getText().trim();
            if (note.isEmpty()) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Bạn chưa nhập lý do hủy. Tiếp tục?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            String orderId = orderModel.getValueAt(row, 0).toString();

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hủy đơn " + orderId + "?",
                    "Xác nhận hủy đơn", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                orderModel.setValueAt("Đã hủy", row, 3);
                updateTimeLabel("Đơn " + orderId + " đã bị hủy.");
                JOptionPane.showMessageDialog(this, "Hủy đơn " + orderId + " thành công!");
            }
        }

        private void updateTimeLabel(String action) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
            timeLabel.setText(action + " (" + now.format(fmt) + ")");
            newTableField.setText("");
            noteArea.setText("");
        }
    }

}

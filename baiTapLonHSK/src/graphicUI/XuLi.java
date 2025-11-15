package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import components.GiaoDienKhuVucBan;
import components.GiaoDienKhuVucBan.CafeTable;
import dao.ChiTietDonHangDAO;
import dao.DonHangDAO;
import dao.KhachHangDAO;
import entity.ChiTietDonHang;
import entity.DonHang;
import entity.KhachHang;

import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OperationPanel groups payment, printing, refunds, transfer/cancel operations.
 * Refund and some actions require password authentication.
 */
public class XuLi extends JPanel {
    private JTabbedPane tabs;
    private Component owner;
    private PaymentPanel paymentPanel; // hold a reference so other inner panels can access table layout

    public XuLi(Component owner, GiaoDienKhuVucBan.TableModel tableModel) {
        this.owner = owner;
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        paymentPanel = new PaymentPanel(tableModel);
        tabs.addTab("Thanh toán", paymentPanel);
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
        // Left side: list of orders and table layout
        private final DefaultTableModel ordersModel;
        private final JTable ordersTable;
        private final GiaoDienKhuVucBan tableLayout;

        // Right side: payment section
        private final DefaultTableModel productModel;
        private final JTable productTable;
        private final JTextField customerIdField = new JTextField(10);
        private final JTextField customerNameField = new JTextField(12);
        private final JTextField customerPhoneField = new JTextField(10);
        private final JTextField totalField = new JTextField(10);
        private final JTextField cashField = new JTextField(10);
        private final JTextField discountField = new JTextField(8);
        private final JTextField taxField = new JTextField(8);
        private final JLabel changeLabel = new JLabel("Tiền thối lại: 0 đ");
        private final JRadioButton cashRadio = new JRadioButton("Tiền mặt");
        private final JRadioButton bankRadio = new JRadioButton("Chuyển khoản");

        private final DonHangDAO donHangDAO = new DonHangDAO();
        private final ChiTietDonHangDAO chiTietDonHangDAO = new ChiTietDonHangDAO();
        private final KhachHangDAO khachHangDAO = new KhachHangDAO();
        private final List<DonHang> currentOrders = new ArrayList<>();
        private volatile Integer pendingCustomerId = null;
        private final DecimalFormat currencyFormat;

        public PaymentPanel(GiaoDienKhuVucBan.TableModel sharedModel) {
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(new Color(250, 250, 255));
            customerIdField.setEditable(false);
            discountField.setEditable(false);
            taxField.setEditable(false);
            totalField.setEditable(false);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            currencyFormat = new DecimalFormat("#,##0.##", symbols);

            // ===== LEFT: orders list (top) and table layout (bottom) =====
            ordersModel = new DefaultTableModel(new String[]{"Mã HĐ", "Bàn", "Trạng thái"}, 0);
            ordersTable = new JTable(ordersModel);
            JScrollPane ordersScroll = new JScrollPane(ordersTable);
            ordersScroll.setBorder(BorderFactory.createTitledBorder("Danh sách hóa đơn (Chọn để load)"));
            ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JButton refreshOrdersBtn = new JButton("Tải lại");
            refreshOrdersBtn.addActionListener(e -> reloadOrdersFromDatabase());

            // Table layout panel in checkout mode (click should NOT change occupancy)
            // Use the shared model so this panel and others reflect the same table objects
            tableLayout = new GiaoDienKhuVucBan(GiaoDienKhuVucBan.Mode.THANHTOAN_MODE, sharedModel);
             // Give the table layout a larger preferred size so it's fully visible
             tableLayout.setPreferredSize(new Dimension(520, 320));
             tableLayout.setMinimumSize(new Dimension(480, 280));

            // When selecting an order from the list, load its products from DB
            ordersTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int r = ordersTable.getSelectedRow();
                    if (r >= 0) {
                        int modelRow = ordersTable.convertRowIndexToModel(r);
                        if (modelRow >= 0 && modelRow < currentOrders.size()) {
                            DonHang order = currentOrders.get(modelRow);
                            displayOrder(order);
                        }
                    }
                }
            });

            // Limit the orders list height so the table layout keeps enough vertical space
            ordersScroll.setPreferredSize(new Dimension(560, 160));
            ordersScroll.setMinimumSize(new Dimension(480, 120));

            JPanel leftPanel = new JPanel(new BorderLayout(8,8));
            JPanel ordersBlock = new JPanel(new BorderLayout(4,4));
            ordersBlock.add(ordersScroll, BorderLayout.CENTER);
            ordersBlock.add(refreshOrdersBtn, BorderLayout.SOUTH);
            leftPanel.add(ordersBlock, BorderLayout.NORTH);
            // ensure the table layout has room to paint fully
            leftPanel.add(tableLayout, BorderLayout.CENTER);
            // Give the left panel more horizontal space by default
            leftPanel.setPreferredSize(new Dimension(620, 0));
            leftPanel.setMinimumSize(new Dimension(520, 300));

            // ===== RIGHT: thanh toán section =====
            productModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "Số lượng", "Giá (đ)", "Thành tiền (đ)"}, 0);
            productTable = new JTable(productModel);
            JScrollPane productScroll = new JScrollPane(productTable);
            productScroll.setBorder(BorderFactory.createTitledBorder("Danh sách sản phẩm"));
            productTable.setFillsViewportHeight(true);

            // Payment info panel
            JPanel paymentInfo = new JPanel(new GridBagLayout());
            paymentInfo.setBorder(BorderFactory.createTitledBorder("Thông tin thanh toán"));
            paymentInfo.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;
            gbc.gridx = 0; gbc.gridy = row; paymentInfo.add(new JLabel("Mã KH:"), gbc);
            gbc.gridx = 1; paymentInfo.add(customerIdField, gbc);
            gbc.gridx = 2; paymentInfo.add(new JLabel("Mã giảm giá:"), gbc);
            gbc.gridx = 3; paymentInfo.add(discountField, gbc);

            row++;
            gbc.gridx = 0; gbc.gridy = row; paymentInfo.add(new JLabel("Tên KH:"), gbc);
            gbc.gridx = 1; paymentInfo.add(customerNameField, gbc);
            gbc.gridx = 2; paymentInfo.add(new JLabel("Thuế (%):"), gbc);
            gbc.gridx = 3; paymentInfo.add(taxField, gbc);

            row++;
            gbc.gridx = 0; gbc.gridy = row; paymentInfo.add(new JLabel("SĐT:"), gbc);
            gbc.gridx = 1; paymentInfo.add(customerPhoneField, gbc);
            gbc.gridx = 2; paymentInfo.add(new JLabel("Tổng tiền:"), gbc);
            gbc.gridx = 3; paymentInfo.add(totalField, gbc);

            row++;
            gbc.gridx = 0; gbc.gridy = row; paymentInfo.add(new JLabel("Khách đưa:"), gbc);
            gbc.gridx = 1; paymentInfo.add(cashField, gbc);
            gbc.gridx = 2; gbc.gridwidth = 2; paymentInfo.add(changeLabel, gbc);
            gbc.gridwidth = 1;

            // Payment method radios + button
            ButtonGroup methodGroup = new ButtonGroup();
            methodGroup.add(cashRadio);
            methodGroup.add(bankRadio);
            cashRadio.setSelected(true);

            JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            methodPanel.setOpaque(false);
            methodPanel.add(cashRadio);
            methodPanel.add(bankRadio);

            JButton payBtn = new JButton("Thanh toán");

            JPanel rightTop = new JPanel(new BorderLayout(8,8));
            rightTop.add(productScroll, BorderLayout.CENTER);
            rightTop.add(paymentInfo, BorderLayout.SOUTH);

            JPanel rightBottom = new JPanel(new BorderLayout(8,8));
            rightBottom.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
            rightBottom.setOpaque(false);
            rightBottom.add(methodPanel, BorderLayout.WEST);
            rightBottom.add(payBtn, BorderLayout.EAST);

            JPanel rightPanel = new JPanel(new BorderLayout(8,8));
            rightPanel.add(rightTop, BorderLayout.CENTER);
            rightPanel.add(rightBottom, BorderLayout.SOUTH);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            // give the left side more space initially and allow continuous layout while resizing
            split.setContinuousLayout(true);
            split.setResizeWeight(0.60); // left gets ~60% of the width
            // set an initial proportional divider location; framework will use this after UI is realized
            split.setDividerLocation(0.60);
            add(split, BorderLayout.CENTER);

            // ===== Events =====
            tableLayout.addTableSelectionListener(this::loadOrderFromTable);

            payBtn.addActionListener(e -> processPayment());

            reloadOrdersFromDatabase();
        }

        // expose tableLayout so other panels can use the shared list
        public GiaoDienKhuVucBan getTableLayout() { return tableLayout; }

        private void reloadOrdersFromDatabase() {
            ordersModel.setRowCount(0);
            currentOrders.clear();
            new SwingWorker<List<DonHang>, Void>() {
                @Override
                protected List<DonHang> doInBackground() {
                    return donHangDAO.layHet();
                }

                @Override
                protected void done() {
                    try {
                        List<DonHang> list = get();
                        if (list != null) {
                            for (DonHang d : list) {
                                currentOrders.add(d);
                                ordersModel.addRow(new Object[]{d.getMaDonHang(), formatTableName(d), d.getTrangThai()});
                            }
                        }
                        if (!currentOrders.isEmpty()) {
                            ordersTable.setRowSelectionInterval(0, 0);
                            displayOrder(currentOrders.get(0));
                        } else {
                            clearOrderDisplay();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Không tải được danh sách hóa đơn từ CSDL.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private String formatTableName(DonHang order) {
            if (order == null) return "";
            if (order.getMaBan() != null) {
                return "Bàn " + order.getMaBan();
            }
            if (order.getLoaiDon() != null) {
                return order.getLoaiDon();
            }
            return "-";
        }

        private void displayOrder(DonHang order) {
            if (order == null) {
                clearOrderDisplay();
                return;
            }
            customerIdField.setText(order.getMaKhachHang() != null ? order.getMaKhachHang().toString() : "");
            discountField.setText(formatCurrency(order.getTienGiam()));
            taxField.setText(formatCurrency(order.getTienThue()));
            totalField.setText(formatCurrency(order.getTongCuoi()));
            fillCustomerInfo(order.getMaKhachHang());
            loadOrderDetailsFromDatabase(order.getMaDonHang());
        }

        private void clearOrderDisplay() {
            productModel.setRowCount(0);
            pendingCustomerId = null;
            customerIdField.setText("");
            customerNameField.setText("");
            customerPhoneField.setText("");
            discountField.setText("");
            taxField.setText("");
            totalField.setText("");
        }

        private void fillCustomerInfo(Integer maKhachHang) {
            if (maKhachHang == null) {
                pendingCustomerId = null;
                customerNameField.setText("Khách lẻ");
                customerPhoneField.setText("");
                return;
            }

            pendingCustomerId = maKhachHang;
            final Integer expected = maKhachHang;
            new SwingWorker<KhachHang, Void>() {
                @Override
                protected KhachHang doInBackground() {
                    return khachHangDAO.layTheoId(maKhachHang);
                }

                @Override
                protected void done() {
                    try {
                        if (pendingCustomerId != expected) {
                            return;
                        }
                        KhachHang kh = get();
                        if (kh != null) {
                            customerNameField.setText(kh.getHoTen());
                            customerPhoneField.setText(kh.getSoDienThoai());
                        } else {
                            customerNameField.setText("Khách lẻ");
                            customerPhoneField.setText("");
                        }
                    } catch (Exception ex) {
                        customerNameField.setText("Khách lẻ");
                        customerPhoneField.setText("");
                    }
                }
            }.execute();
        }

        private void loadOrderDetailsFromDatabase(int maDonHang) {
            productModel.setRowCount(0);
            new SwingWorker<List<ChiTietDonHang>, Void>() {
                @Override
                protected List<ChiTietDonHang> doInBackground() {
                    return chiTietDonHangDAO.layTheoDonHang(maDonHang);
                }

                @Override
                protected void done() {
                    try {
                        List<ChiTietDonHang> details = get();
                        if (details != null) {
                            for (ChiTietDonHang ct : details) {
                                String tenMon = ct.getMon() != null && ct.getMon().getTenMon() != null
                                        ? ct.getMon().getTenMon()
                                        : ("Món " + ct.getMaMon());
                                productModel.addRow(new Object[]{ct.getMaMon(), tenMon, ct.getSoLuong(), ct.getGiaBan(), ct.getThanhTien()});
                            }
                        }
                        updateTotalFromProducts();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Không tải được chi tiết hóa đơn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void loadOrderFromTable(CafeTable t) {
            if (t == null) return;
            if (t.maBan <= 0) {
                JOptionPane.showMessageDialog(this, "Bàn này không liên kết với hóa đơn tại chỗ.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            new SwingWorker<DonHang, Void>() {
                @Override
                protected DonHang doInBackground() {
                    List<DonHang> list = donHangDAO.layTheoBan(t.maBan);
                    if (list == null || list.isEmpty()) return null;
                    for (DonHang dh : list) {
                        if (!isPaid(dh)) return dh;
                    }
                    return list.get(0);
                }

                @Override
                protected void done() {
                    try {
                        DonHang dh = get();
                        if (dh != null) {
                            selectOrderInTable(dh.getMaDonHang());
                            displayOrder(dh);
                        } else {
                            JOptionPane.showMessageDialog(PaymentPanel.this, "Chưa có hóa đơn nào gắn với bàn " + t.name + ".", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Lỗi khi tải hóa đơn của bàn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void selectOrderInTable(int maDonHang) {
            for (int i = 0; i < currentOrders.size(); i++) {
                DonHang d = currentOrders.get(i);
                if (d != null && d.getMaDonHang() == maDonHang) {
                    final int row = i;
                    SwingUtilities.invokeLater(() -> {
                        ordersTable.setRowSelectionInterval(row, row);
                        ordersTable.scrollRectToVisible(ordersTable.getCellRect(row, 0, true));
                    });
                    return;
                }
            }
            reloadOrdersFromDatabase();
        }

        private boolean isPaid(DonHang order) {
            if (order == null || order.getTrangThai() == null) return false;
            String status = order.getTrangThai().replaceAll("\\s+", "");
            String normalized = Normalizer.normalize(status, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toUpperCase(Locale.ROOT);
            return normalized.contains("PAID") || normalized.contains("THANHTOAN") || normalized.contains("HOANTAT");
        }

        private void updateTotalFromProducts() {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < productModel.getRowCount(); i++) {
                Object v = productModel.getValueAt(i, 4);
                if (v instanceof BigDecimal) {
                    sum = sum.add((BigDecimal) v);
                } else if (v instanceof Number) {
                    sum = sum.add(BigDecimal.valueOf(((Number) v).doubleValue()));
                } else if (v != null) {
                    try {
                        String normalized = v.toString().replaceAll("[^0-9.-]", "");
                        if (!normalized.isEmpty()) {
                            sum = sum.add(new BigDecimal(normalized));
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (sum.compareTo(BigDecimal.ZERO) > 0) {
                totalField.setText(formatCurrency(sum));
            }
        }

        private String formatCurrency(BigDecimal value) {
            if (value == null) return "";
            return currencyFormat.format(value) + " đ";
        }

        private void processPayment() {
            if (productModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Danh sách sản phẩm rỗng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận thanh toán?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            // after confirm, show print option dialog
            int printOpt = JOptionPane.showOptionDialog(this,
                    "Thanh toán thành công. Bạn có muốn in hóa đơn?",
                    "In hóa đơn",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"In hóa đơn", "Đóng"},
                    "In hóa đơn");

            if (printOpt == JOptionPane.YES_OPTION) {
                // simulate print: show a dialog with invoice content (placeholder)
                StringBuilder invoice = new StringBuilder();
                invoice.append("***** HÓA ĐƠN *****\n");
                invoice.append("Khách: ").append(customerNameField.getText()).append("\n");
                invoice.append("SĐT: ").append(customerPhoneField.getText()).append("\n");
                invoice.append("--------------------------------\n");
                for (int i = 0; i < productModel.getRowCount(); i++) {
                    invoice.append(productModel.getValueAt(i,1)).append(" x").append(productModel.getValueAt(i,2)).append("   ").append(productModel.getValueAt(i,4)).append("\n");
                }
                invoice.append("--------------------------------\n");
                invoice.append("Tổng: ").append(totalField.getText()).append(" đ\n");
                JOptionPane.showMessageDialog(this, invoice.toString(), "Hóa đơn (giả lập)", JOptionPane.INFORMATION_MESSAGE);
            }

            // clear after payment
            clearOrderDisplay();
            cashField.setText("");
            changeLabel.setText("Tiền thối lại: 0 đ");
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

    class TransferCancelPanel extends JPanel {
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

            String orderId = orderModel.getValueAt(row, 0).toString();
            String oldTable = orderModel.getValueAt(row, 1).toString();

            // Use the real shared tables from the payment panel layout and start a transfer
            GiaoDienKhuVucBan layout = XuLi.this.paymentPanel.getTableLayout();
            boolean started = layout.beginTransferFrom(oldTable);
            if (!started) return;

            // Add a temporary listener that will be notified when the transfer completes inside the layout
            final int selRow = row;
            final String selOrderId = orderId;
            GiaoDienKhuVucBan.TableSelectionListener temp = new GiaoDienKhuVucBan.TableSelectionListener() {
                @Override
                public void tableSelected(GiaoDienKhuVucBan.CafeTable t) {
                    // Update order model and UI and remove this listener
                    SwingUtilities.invokeLater(() -> {
                        orderModel.setValueAt(t.name, selRow, 1);
                        updateTimeLabel("Chuyển bàn " + selOrderId + " thành công!");
                        layout.removeTableSelectionListener(this);
                    });
                }
            };
            layout.addTableSelectionListener(temp);
        }

        // helper trying to map strings like "Bàn 1" or "T1" to the CafeTable instances
        private GiaoDienKhuVucBan.CafeTable findMatchingTable(List<GiaoDienKhuVucBan.CafeTable> tables, String label) {
            if (label == null) return null;
            // direct match first
            for (GiaoDienKhuVucBan.CafeTable t : tables) {
                if (t.name != null && t.name.equalsIgnoreCase(label)) return t;
            }
            // try to extract a number from label (e.g. "Bàn 1" -> 1) and match T<number>
            String digits = label.replaceAll("\\D+", "");
            if (!digits.isEmpty()) {
                String tname = "T" + digits;
                for (GiaoDienKhuVucBan.CafeTable t : tables) {
                    if (t.name != null && t.name.startsWith(tname)) return t;
                }
            }
            // fallback: match contains
            for (GiaoDienKhuVucBan.CafeTable t : tables) {
                if (t.name != null && label.contains(t.name)) return t;
            }
            return null;
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

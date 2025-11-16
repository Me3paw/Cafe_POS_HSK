package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import components.GiaoDienKhuVucBan;
import components.GiaoDienKhuVucBan.CafeTable;
import dao.*;
import entity.*;

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
import java.util.function.Supplier;

/**
 * OperationPanel groups payment, printing, refunds, transfer/cancel operations.
 * Refunds and cancellations now rely on the logged-in account's role instead of
 * prompting for the legacy system password dialog.
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
        tabs.addTab("Hoàn tiền", buildRefundTab());
        tabs.addTab("Hủy đơn", buildCancelTab());
        add(tabs, BorderLayout.CENTER);
    }

    private Component buildRefundTab() {
        if (SessionContext.isAdmin()) {
            return new RefundPanel();
        }
        return buildLockedAdminPanel(
                "<html>Hoàn tiền chỉ khả dụng cho tài khoản quản trị.<br>Đăng nhập admin để tiếp tục.</html>",
                RefundPanel::new);
    }

    private Component buildCancelTab() {
        if (SessionContext.isAdmin()) {
            return new CancelPanel();
        }
        return buildLockedAdminPanel(
                "<html>Hủy đơn là chức năng nâng cao dành cho admin.</html>",
                CancelPanel::new);
    }

    private Component buildLockedAdminPanel(String message, Supplier<Component> supplier) {
        JPanel locked = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(message);
        lbl.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JButton btn = new JButton("Đăng nhập admin");
        btn.addActionListener(e -> {
            DangNhapDialog dialog = new DangNhapDialog(SwingUtilities.getWindowAncestor(this));
            entity.NguoiDung user = dialog.showDialog();
            if (user == null) {
                return;
            }
            SessionContext.setCurrentUser(user);
            if (!SessionContext.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Tài khoản không có quyền admin.", "Từ chối", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int idx = tabs.indexOfComponent(locked);
            if (idx >= 0) {
                tabs.setComponentAt(idx, supplier.get());
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
        private boolean filterTakeawayOnly = false;

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
        private final BanDAO banDAO = new BanDAO();
        private final GiamGiaDAO giamGiaDAO = new GiamGiaDAO();
        private final List<DonHang> currentOrders = new ArrayList<>();
        private volatile Integer pendingCustomerId = null;
        private final DecimalFormat currencyFormat;
        private DonHang currentLoadedOrder = null;
        // Prevent recursive or programmatic selection events
        private boolean isProgrammaticSelection = false;
        private SwingWorker<List<ChiTietDonHang>, Void> currentDetailsWorker;
        private Integer currentDetailOrderId = null;


        public PaymentPanel(GiaoDienKhuVucBan.TableModel sharedModel) {
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(new Color(250, 250, 255));
            customerIdField.setEditable(false);
            discountField.setEditable(true);
            taxField.setEditable(false);
            totalField.setEditable(false);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            currencyFormat = new DecimalFormat("#,##0.##", symbols);

            // ===== LEFT: orders list (top) and table layout (bottom) =====
            ordersModel = new DefaultTableModel(new String[]{"Mã HĐ", "Bàn"}, 0);
            ordersTable = new JTable(ordersModel);
            JScrollPane ordersScroll = new JScrollPane(ordersTable);
            ordersScroll.setBorder(BorderFactory.createTitledBorder("Danh sách hóa đơn (Chọn để load)"));
            ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JButton refreshOrdersBtn = new JButton("Tải lại");
            refreshOrdersBtn.addActionListener(e -> {
                filterTakeawayOnly = false;
                reloadOrdersFromDatabase();
            });

            // Table layout panel in checkout mode (click should NOT change occupancy)
            // Use the shared model so this panel and others reflect the same table objects
            tableLayout = new GiaoDienKhuVucBan(GiaoDienKhuVucBan.Mode.THANHTOAN_MODE, sharedModel);
             // Give the table layout a larger preferred size so it's fully visible
             tableLayout.setPreferredSize(new Dimension(520, 320));
             tableLayout.setMinimumSize(new Dimension(480, 280));

            // When selecting an order from the list, load its products from DB
             ordersTable.getSelectionModel().addListSelectionListener(e -> {
            	    if (e.getValueIsAdjusting() || isProgrammaticSelection) return;

            	    int viewRow = ordersTable.getSelectedRow();
            	    if (viewRow < 0) return;

            	    int modelRow = ordersTable.convertRowIndexToModel(viewRow);
            	    if (modelRow < 0 || modelRow >= currentOrders.size()) return;

            	    DonHang order = currentOrders.get(modelRow);
            	    if (order != null) {
            	        displayOrder(order);
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
            gbc.gridx = 1; 
            JPanel phonePanel = new JPanel(new BorderLayout(4, 4));
            phonePanel.setOpaque(false);
            JButton lookupPhoneBtn = new JButton("Tìm");
            lookupPhoneBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lookupPhoneBtn.setFocusPainted(false);
            phonePanel.add(customerPhoneField, BorderLayout.CENTER);
            phonePanel.add(lookupPhoneBtn, BorderLayout.EAST);
            paymentInfo.add(phonePanel, gbc);
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
            
            lookupPhoneBtn.addActionListener(e -> lookupCustomerByPhone());
            
            cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            });

            reloadOrdersFromDatabase();
        }

        // expose tableLayout so other panels can use the shared list
        public GiaoDienKhuVucBan getTableLayout() { return tableLayout; }
        private void loadByOrderId(int orderId) {
            for (int i = 0; i < currentOrders.size(); i++) {
                DonHang d = currentOrders.get(i);
                if (d != null && d.getMaDonHang() == orderId) {

                    // Prevent recursive events
                    isProgrammaticSelection = true;
                    ordersTable.setRowSelectionInterval(i, i);
                    isProgrammaticSelection = false;

                    displayOrder(d);
                    return;
                }
            }
        }

        private void refreshTableLayoutFromDatabase() {
            new SwingWorker<List<Ban>, Void>() {
                @Override
                protected List<Ban> doInBackground() {
                    BanDAO banDAO = new BanDAO();
                    return banDAO.layHet();
                }

                @Override
                protected void done() {
                    try {
                        List<Ban> allBans = get();
                        if (allBans != null) {
                            tableLayout.getTableModel().mergeStatuses(allBans);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();
        }

        private void reloadOrdersFromDatabase() {
            ordersModel.setRowCount(0);
            currentOrders.clear();
            clearOrderDisplay();

            final boolean takeawayOnly = filterTakeawayOnly;
            new SwingWorker<List<DonHang>, Void>() {
                @Override
                protected List<DonHang> doInBackground() {
                    List<DonHang> all = donHangDAO.layHet();
                    if (all == null) return List.of();

                    return all.stream()
                              .filter(d -> d.getTrangThai() != null && d.getTrangThai().equalsIgnoreCase("dangMo"))
                              .filter(d -> !takeawayOnly || isTakeawayOrder(d))
                              .toList();
                }

                @Override
                protected void done() {
                    try {
                        List<DonHang> list = get();
                        for (DonHang d : list) {
                            currentOrders.add(d);
                            ordersModel.addRow(new Object[] {
                                d.getMaDonHang(), formatTableName(d)
                            });
                        }

                        if (!currentOrders.isEmpty()) {
                            loadByOrderId(currentOrders.get(0).getMaDonHang());
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this,
                                "Không tải được danh sách hóa đơn từ CSDL.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }


        private String formatTableName(DonHang order) {
            if (order == null) return "";
            if (order.getMaBan() != null) {
                if (isTakeawayOrder(order)) {
                    return "Mang đi";
                }
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
            currentLoadedOrder = order;
            customerIdField.setText(order.getMaKhachHang() != null ? order.getMaKhachHang().toString() : "");
            discountField.setText(formatCurrency(order.getTienGiam()));
            if (order.getTienThue() != null && order.getTienThue().compareTo(BigDecimal.ZERO) > 0) {
                taxField.setText(order.getTienThue().toString());
            } else {
                taxField.setText("0");
            }
            totalField.setText(formatCurrency(order.getTongCuoi()));
            fillCustomerInfo(order.getMaKhachHang());
            // Clear products table first before loading new details to prevent duplicates
            cancelCurrentDetailsLoader();
            productModel.setRowCount(0);
            loadOrderDetailsFromDatabase(order.getMaDonHang(), order.getTongCuoi());
        }

        private void clearOrderDisplay() {
            cancelCurrentDetailsLoader();
            currentDetailOrderId = null;
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

        private void lookupCustomerByPhone() {
            String phone = customerPhoneField.getText().trim();
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số điện thoại để tìm khách hàng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new SwingWorker<KhachHang, Void>() {
                @Override
                protected KhachHang doInBackground() {
                    return khachHangDAO.layTheoSoDienThoai(phone);
                }

                @Override
                protected void done() {
                    try {
                        KhachHang kh = get();
                        if (kh != null) {
                            customerIdField.setText(kh.getMaKhachHang() + "");
                            customerNameField.setText(kh.getHoTen());
                            customerPhoneField.setText(kh.getSoDienThoai());
                            pendingCustomerId = kh.getMaKhachHang();
                            JOptionPane.showMessageDialog(PaymentPanel.this, "Tìm thấy khách hàng: " + kh.getHoTen(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(PaymentPanel.this, "Không tìm thấy khách hàng với số điện thoại này.", "Không tìm thấy", JOptionPane.INFORMATION_MESSAGE);
                            customerIdField.setText("");
                            customerNameField.setText("Khách lẻ");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Lỗi khi tìm khách hàng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void updateChange() {
            String cashStr = cashField.getText().trim();
            String totalStr = totalField.getText();
            
            if (cashStr.isEmpty() || totalStr.isEmpty()) {
                changeLabel.setText("Tiền thối lại: 0 đ");
                return;
            }

            try {
                BigDecimal cash = new BigDecimal(cashStr.replaceAll("[^0-9.-]", ""));
                BigDecimal total = new BigDecimal(totalStr.replaceAll("[^0-9.-]", ""));
                BigDecimal change = cash.subtract(total);
                
                if (change.compareTo(BigDecimal.ZERO) < 0) {
                    changeLabel.setText("Tiền thối lại: KHÔNG ĐỦ");
                    changeLabel.setForeground(Color.RED);
                } else {
                    changeLabel.setText("Tiền thối lại: " + formatCurrency(change));
                    changeLabel.setForeground(Color.BLACK);
                }
            } catch (Exception ex) {
                changeLabel.setText("Tiền thối lại: 0 đ");
            }
        }

        private void loadOrderDetailsFromDatabase(int maDonHang, BigDecimal tongCuoiExpected) {
            cancelCurrentDetailsLoader();
            currentDetailOrderId = maDonHang;
            productModel.setRowCount(0);
            final int expectedOrderId = maDonHang;
            currentDetailsWorker = new SwingWorker<List<ChiTietDonHang>, Void>() {
                @Override
                protected List<ChiTietDonHang> doInBackground() {
                    return chiTietDonHangDAO.layTheoDonHang(maDonHang);
                }

                @Override
                protected void done() {
                    try {
                        if (isCancelled() || !isCurrentDetailRequest(expectedOrderId)) {
                            return;
                        }
                        List<ChiTietDonHang> details = get();
                        if (details != null) {
                            for (ChiTietDonHang ct : details) {
                                String tenMon = ct.getMon() != null && ct.getMon().getTenMon() != null
                                        ? ct.getMon().getTenMon()
                                        : ("Món " + ct.getMaMon());
                                productModel.addRow(new Object[]{ct.getMaMon(), tenMon, ct.getSoLuong(), ct.getGiaBan(), ct.getThanhTien()});
                            }
                        }
                        // Preserve the tongCuoi value (total with tax), don't recalculate from products
                        if (tongCuoiExpected != null) {
                            totalField.setText(formatCurrency(tongCuoiExpected));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Không tải được chi tiết hóa đơn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            currentDetailsWorker.execute();
        }

        private boolean isCurrentDetailRequest(int expectedOrderId) {
            if (currentDetailOrderId == null || currentLoadedOrder == null) {
                return false;
            }
            return currentDetailOrderId == expectedOrderId
                    && currentLoadedOrder.getMaDonHang() == expectedOrderId;
        }

        private void cancelCurrentDetailsLoader() {
            if (currentDetailsWorker != null && !currentDetailsWorker.isDone()) {
                currentDetailsWorker.cancel(true);
            }
        }

        private void loadOrderFromTable(CafeTable t) {
            if (t == null) {
                JOptionPane.showMessageDialog(this, "Không xác định được bàn.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (t.isTakeaway) {
                showTakeawayOrdersOnly();
                return;
            }

            if (t.maBan <= 0) {
                JOptionPane.showMessageDialog(this, "Bàn này không liên kết với hóa đơn tại chỗ.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (filterTakeawayOnly) {
                filterTakeawayOnly = false;
                reloadOrdersFromDatabase();
            }

            new SwingWorker<DonHang, Void>() {
                @Override
                protected DonHang doInBackground() {
                    List<DonHang> list = donHangDAO.layTheoBan(t.maBan);
                    if (list == null || list.isEmpty()) return null;

                    // Load first unpaid order, otherwise the first
                    return list.stream()
                               .filter(d -> !isPaid(d))
                               .findFirst()
                               .orElse(list.get(0));
                }

                @Override
                protected void done() {
                    try {
                        DonHang dh = get();
                        if (dh != null) {
                            displayOrder(dh);
                            selectOrderInTable(dh.getMaDonHang());
                        } else {
                            JOptionPane.showMessageDialog(PaymentPanel.this,
                                "Chưa có hóa đơn nào gắn với bàn " + t.name + ".",
                                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this,
                                "Lỗi khi tải hóa đơn của bàn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void showTakeawayOrdersOnly() {
            boolean alreadyFiltering = filterTakeawayOnly;
            filterTakeawayOnly = true;
            reloadOrdersFromDatabase();
            if (!alreadyFiltering) {
                JOptionPane.showMessageDialog(this,
                        "Đang lọc danh sách hóa đơn theo các đơn mang đi đang mở.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private boolean isTakeawayOrder(DonHang order) {
            return order != null
                    && order.getMaBan() != null
                    && order.getMaBan() == GiaoDienKhuVucBan.TAKEAWAY_TABLE_ID;
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

            if (currentLoadedOrder == null) {
                JOptionPane.showMessageDialog(this, "Không có hóa đơn nào để thanh toán!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Validate cash before processing payment
            String cashStr = cashField.getText().trim();
            String totalStr = totalField.getText();
            
            if (cashStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền khách đưa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                BigDecimal cash = new BigDecimal(cashStr.replaceAll("[^0-9.-]", ""));
                BigDecimal total = new BigDecimal(totalStr.replaceAll("[^0-9.-]", ""));
                
                if (cash.compareTo(total) < 0) {
                    JOptionPane.showMessageDialog(this, "Số tiền khách đưa không đủ! Cần thêm " + formatCurrency(total.subtract(cash)), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận thanh toán?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        // Update DonHang trangThai to "daThanhToan"
                        currentLoadedOrder.setTrangThai("daThanhToan");
                        if (!donHangDAO.capNhat(currentLoadedOrder)) {
                            return false;
                        }

                        // Update Ban trangThai to "trong" and clear maDonHang
                        Ban ban = banDAO.layTheoId(currentLoadedOrder.getMaBan());
                        if (ban != null) {
                            ban.setTrangThai("trong");
                            ban.setMaDonHang(null);
                            if (!banDAO.capNhat(ban)) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        Boolean success = get();
                        if (success) {
                            // Show print option dialog
                            int printOpt = JOptionPane.showOptionDialog(PaymentPanel.this,
                                    "Thanh toán thành công. Bạn có muốn in hóa đơn?",
                                    "In hóa đơn",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    new String[]{"In hóa đơn", "Đóng"},
                                    "In hóa đơn");

                            if (printOpt == JOptionPane.YES_OPTION) {
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
                                JOptionPane.showMessageDialog(PaymentPanel.this, invoice.toString(), "Hóa đơn", JOptionPane.INFORMATION_MESSAGE);
                            }

                            // Reload orders and refresh table layout from database
                            reloadOrdersFromDatabase();
                            refreshTableLayoutFromDatabase();
                            clearOrderDisplay();
                            cashField.setText("");
                            changeLabel.setText("Tiền thối lại: 0 đ");
                            changeLabel.setForeground(Color.BLACK);
                        } else {
                            JOptionPane.showMessageDialog(PaymentPanel.this, "Lỗi khi cập nhật trạng thái thanh toán.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(PaymentPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
    static class RefundPanel extends JPanel {
        private final JTextField searchField = new JTextField(15);
        private final JTable invoiceTable;
        private final JTable detailTable;
        private final DefaultTableModel invoiceModel;
        private final DefaultTableModel detailModel;
        private final JTextArea noteArea = new JTextArea(3, 20);
        private final DonHangDAO donHangDAO = new DonHangDAO();
        private final BanDAO banDAO = new BanDAO();
        private DonHang currentSelectedOrder = null;

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
            searchPanel.add(new JLabel("Tìm hóa đơn (Mã HĐ):"));
            searchPanel.add(searchField);
            JButton searchBtn = new JButton("Tìm");
            styleButton(searchBtn);
            searchPanel.add(searchBtn);
            add(searchPanel, BorderLayout.NORTH);

            // === Bảng danh sách hóa đơn ===
            String[] invoiceCols = {"Mã hóa đơn", "Bàn", "Tổng tiền", "Trạng thái"};
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

        private void searchInvoice() {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập mã hóa đơn để tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int maDonHang = Integer.parseInt(query);
                DonHang order = donHangDAO.layTheoId(maDonHang);
                
                if (order != null) {
                    invoiceModel.setRowCount(0);
                    String banInfo = order.getMaBan() != null ? "Bàn " + order.getMaBan() : "Mang đi";
                    invoiceModel.addRow(new Object[]{
                        order.getMaDonHang(),
                        banInfo,
                        order.getTongCuoi(),
                        order.getTrangThai()
                    });
                    invoiceTable.setRowSelectionInterval(0, 0);
                    showInvoiceDetails();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn với mã: " + maDonHang, "Không tìm thấy", JOptionPane.INFORMATION_MESSAGE);
                    invoiceModel.setRowCount(0);
                    detailModel.setRowCount(0);
                    currentSelectedOrder = null;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Mã hóa đơn phải là số nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showInvoiceDetails() {
            int row = invoiceTable.getSelectedRow();
            if (row < 0) return;
            detailModel.setRowCount(0);

            try {
                String idStr = invoiceModel.getValueAt(row, 0).toString();
                int maDonHang = Integer.parseInt(idStr);
                currentSelectedOrder = donHangDAO.layTheoId(maDonHang);
                
                if (currentSelectedOrder != null) {
                    ChiTietDonHangDAO chiTietDAO = new ChiTietDonHangDAO();
                    List<ChiTietDonHang> details = chiTietDAO.layTheoDonHang(currentSelectedOrder.getMaDonHang());
                    if (details != null) {
                        for (ChiTietDonHang ct : details) {
                            String tenMon = ct.getMon() != null && ct.getMon().getTenMon() != null
                                    ? ct.getMon().getTenMon()
                                    : ("Món " + ct.getMaMon());
                            detailModel.addRow(new Object[]{tenMon, ct.getSoLuong(), ct.getGiaBan(), ct.getThanhTien()});
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void processRefund() {
            if (currentSelectedOrder == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn cần hoàn tiền!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String note = noteArea.getText().trim();
            if (note.isEmpty()) {
                int opt = JOptionPane.showConfirmDialog(this, "Bạn chưa nhập lý do hoàn tiền. Tiếp tục?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hoàn tiền cho hóa đơn " + currentSelectedOrder.getMaDonHang() + "?",
                    "Xác nhận hoàn tiền", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        try {
                            // Update DonHang trangThai to "huy"
                            currentSelectedOrder.setTrangThai("huy");
                            if (!donHangDAO.capNhat(currentSelectedOrder)) {
                                return false;
                            }

                            // Update Ban trangThai to "trong" and clear maDonHang
                            if (currentSelectedOrder.getMaBan() != null && currentSelectedOrder.getMaBan() > 0) {
                                Ban ban = banDAO.layTheoId(currentSelectedOrder.getMaBan());
                                if (ban != null) {
                                    ban.setTrangThai("trong");
                                    ban.setMaDonHang(null);
                                    if (!banDAO.capNhat(ban)) {
                                        return false;
                                    }
                                }
                            }

                            return true;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return false;
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            Boolean success = get();
                            if (success) {
                                JOptionPane.showMessageDialog(RefundPanel.this,
                                        "Hoàn tiền thành công cho hóa đơn " + currentSelectedOrder.getMaDonHang() + "!",
                                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                noteArea.setText("");
                                detailModel.setRowCount(0);
                                invoiceModel.setRowCount(0);
                                searchField.setText("");
                                currentSelectedOrder = null;
                            } else {
                                JOptionPane.showMessageDialog(RefundPanel.this,
                                        "Lỗi khi cập nhật trạng thái hoàn tiền.",
                                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(RefundPanel.this,
                                    "Lỗi: " + ex.getMessage(),
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            }
        }
    }

    static class CancelPanel extends JPanel {
        private final JTextField orderIdField = new JTextField(10);
        private final JTextArea reasonArea = new JTextArea(3, 20);
        private final DonHangDAO donHangDAO = new DonHangDAO();
        private final BanDAO banDAO = new BanDAO();
        private DonHang currentSelectedOrder = null;

        public CancelPanel() {
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JLabel title = new JLabel("Hủy đơn hàng", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Mã hóa đơn:"), gbc);
            gbc.gridx = 1;
            JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
            JButton searchBtn = new JButton("Tìm");
            inputPanel.add(orderIdField, BorderLayout.CENTER);
            inputPanel.add(searchBtn, BorderLayout.EAST);
            form.add(inputPanel, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            gbc.gridwidth = 2;
            reasonArea.setBorder(BorderFactory.createTitledBorder("Lý do hủy"));
            form.add(new JScrollPane(reasonArea), gbc);

            JButton cancelBtn = new JButton("Hủy đơn");
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.EAST;
            form.add(cancelBtn, gbc);

            add(form, BorderLayout.CENTER);

            // === Sự kiện ===
            searchBtn.addActionListener(e -> searchOrder());
            cancelBtn.addActionListener(e -> cancelOrder());
        }

        private void searchOrder() {
            String text = orderIdField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập mã hóa đơn để tìm kiếm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int maDonHang = Integer.parseInt(text);
                currentSelectedOrder = donHangDAO.layTheoId(maDonHang);
                
                if (currentSelectedOrder != null) {
                    reasonArea.setText("");
                    JOptionPane.showMessageDialog(this,
                            "Tìm thấy hóa đơn " + maDonHang + "\nTrạng thái: " + currentSelectedOrder.getTrangThai(),
                            "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn với mã: " + maDonHang, "Không tìm thấy", JOptionPane.INFORMATION_MESSAGE);
                    currentSelectedOrder = null;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Mã hóa đơn phải là số nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void cancelOrder() {
            if (currentSelectedOrder == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng tìm hóa đơn trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if ("daHuy".equalsIgnoreCase(currentSelectedOrder.getTrangThai())) {
                JOptionPane.showMessageDialog(this, "Hóa đơn đã được hủy trước đó.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String reason = reasonArea.getText().trim();
            if (reason.isEmpty()) {
                int opt = JOptionPane.showConfirmDialog(this, "Bạn chưa nhập lý do hủy. Tiếp tục?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận hủy hóa đơn " + currentSelectedOrder.getMaDonHang() + "?",
                    "Xác nhận hủy", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        try {
                            // Update DonHang trangThai to "huy"
                            currentSelectedOrder.setTrangThai("huy");
                            if (!donHangDAO.capNhat(currentSelectedOrder)) {
                                return false;
                            }

                            // Update Ban trangThai to "trong" and clear maDonHang
                            if (currentSelectedOrder.getMaBan() != null && currentSelectedOrder.getMaBan() > 0) {
                                Ban ban = banDAO.layTheoId(currentSelectedOrder.getMaBan());
                                if (ban != null) {
                                    ban.setTrangThai("trong");
                                    ban.setMaDonHang(null);
                                    if (!banDAO.capNhat(ban)) {
                                        return false;
                                    }
                                }
                            }

                            return true;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return false;
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            Boolean success = get();
                            if (success) {
                                JOptionPane.showMessageDialog(CancelPanel.this,
                                        "Đã hủy hóa đơn " + currentSelectedOrder.getMaDonHang() + ".",
                                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                orderIdField.setText("");
                                reasonArea.setText("");
                                currentSelectedOrder = null;
                            } else {
                                JOptionPane.showMessageDialog(CancelPanel.this,
                                        "Không thể hủy hóa đơn.",
                                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(CancelPanel.this,
                                    "Lỗi: " + ex.getMessage(),
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            }
        }
    }

}

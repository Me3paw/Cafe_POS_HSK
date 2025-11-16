package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import components.GiaoDienKhuVucBan;
import components.OrderPanelBridge;
import dao.DanhMucDAO;
import dao.MonDAO;
import dao.TonKhoDAO;
import dao.DonHangDAO;
import dao.ChiTietDonHangDAO;
import dao.BanDAO;
import dao.ThueDAO;
import entity.DanhMuc;
import entity.Mon;
import entity.TonKho;
import entity.DonHang;
import entity.ChiTietDonHang;
import entity.Ban;
import entity.Thue;

/**
 * UpdatePanel contains subpages: Order (default), Table status.
 */
public class CapNhat extends JPanel {
    private JTabbedPane tabs;
    private OrderPanel orderPanel;
    private final GiaoDienKhuVucBan.TableModel tableModel;

    public CapNhat(GiaoDienKhuVucBan.TableModel tableModel) {
        this.tableModel = tableModel;
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        orderPanel = new OrderPanel();
        tabs.addTab("Đơn hàng mới", orderPanel);
        tabs.addTab("Cập nhật trạng thái bàn", new TableStatusPanel());
        add(tabs, BorderLayout.CENTER);
    }

    public void showDefault() {
        tabs.setSelectedIndex(0);
    }

    // Order panel with món list on left and order items on right
    class OrderPanel extends JPanel implements OrderPanelBridge {
        private DefaultTableModel monModel;
        private JTable monTable;
        private DefaultTableModel orderItemsModel;
        private JTable orderItemsTable;
        private JTextField qtyField;
        private Map<Integer, Mon> monCache;
        private Map<Integer, TonKho> tonKhoCache;
        private Map<Integer, String> danhMucCache;
        private JButton createOrderBtn;
        private boolean appendMode = false;
        private Integer appendDonHangId = null;
        private Runnable appendSuccessCallback;

        public OrderPanel() {
            setLayout(new BorderLayout(8,8));
            setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

            // Load data from DB
            loadData();

            // Left panel: món list
            JPanel leftPanel = new JPanel(new BorderLayout());
            monModel = new DefaultTableModel(new Object[]{"Mã món", "Tên", "Danh mục", "Giá bán", "Còn bán", "Mô tả"}, 0);
            monTable = new JTable(monModel);
            monTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            populateMonTable();
            leftPanel.add(new JScrollPane(monTable), BorderLayout.CENTER);

            // Right panel: order items and controls
            JPanel rightPanel = new JPanel(new BorderLayout(8,8));

            // Order items table
            orderItemsModel = new DefaultTableModel(new Object[]{"Tên", "Giá Bán", "Số Lượng", "Thành Tiền"}, 0);
            orderItemsTable = new JTable(orderItemsModel);
            rightPanel.add(new JScrollPane(orderItemsTable), BorderLayout.CENTER);

            // Controls: quantity input and buttons
            JPanel controlPanel = new JPanel(new BorderLayout(8,8));
            JPanel inputPanel = new JPanel();
            inputPanel.add(new JLabel("Số Lượng:"));
            qtyField = new JTextField("1", 5);
            inputPanel.add(qtyField);
            controlPanel.add(inputPanel, BorderLayout.WEST);

            JPanel buttonsPanel = new JPanel();
            JButton addBtn = new JButton("Thêm");
            JButton deleteBtn = new JButton("Xóa");
            JButton clearBtn = new JButton("Hủy");
            createOrderBtn = new JButton("Tạo đơn");
            buttonsPanel.add(addBtn);
            buttonsPanel.add(deleteBtn);
            buttonsPanel.add(clearBtn);
            buttonsPanel.add(createOrderBtn);
            controlPanel.add(buttonsPanel, BorderLayout.EAST);

            rightPanel.add(controlPanel, BorderLayout.SOUTH);

            // Split view: left and right
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            split.setDividerLocation(400);
            add(split, BorderLayout.CENTER);

            // Button listeners
            addBtn.addActionListener(e -> addItemToOrder());
            deleteBtn.addActionListener(e -> deleteItemFromOrder());
            clearBtn.addActionListener(e -> clearOrderItems());
            createOrderBtn.addActionListener(e -> {
                if (appendMode) {
                    appendItemsToExistingOrder();
                } else {
                    createOrder();
                }
            });
        }

        private void loadData() {
            monCache = new HashMap<>();
            tonKhoCache = new HashMap<>();
            danhMucCache = new HashMap<>();

            try {
                MonDAO monDAO = new MonDAO();
                List<Mon> mons = monDAO.layHet();
                for (Mon m : mons) {
                    monCache.put(m.getMaMon(), m);
                }

                TonKhoDAO tonKhoDAO = new TonKhoDAO();
                List<TonKho> tonKhos = tonKhoDAO.layHet();
                for (TonKho tk : tonKhos) {
                    tonKhoCache.put(tk.getMaMon(), tk);
                }

                DanhMucDAO danhMucDAO = new DanhMucDAO();
                List<DanhMuc> danhMucs = danhMucDAO.layHet();
                for (DanhMuc dm : danhMucs) {
                    danhMucCache.put(dm.getMaDanhMuc(), dm.getTenDanhMuc());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void populateMonTable() {
            monModel.setRowCount(0);
            for (Mon m : monCache.values()) {
                String danhMucName = danhMucCache.getOrDefault(m.getMaDanhMuc(), "");
                TonKho tonKho = tonKhoCache.get(m.getMaMon());
                BigDecimal soLuong = tonKho != null ? tonKho.getSoLuong() : BigDecimal.ZERO;

                monModel.addRow(new Object[]{
                        m.getMaMon(),
                        m.getTenMon(),
                        danhMucName,
                        m.getGiaBan(),
                        m.isConBan() ? "Có" : "Không",
                        m.getMoTa()
                });
            }
        }

        private void addItemToOrder() {
            int selectedRow = monTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một món.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int maMon = (int) monModel.getValueAt(selectedRow, 0);
            Mon mon = monCache.get(maMon);

            if (mon == null) {
                JOptionPane.showMessageDialog(this, "Không thể tìm thấy món.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if món is available (conBan = true and tonKho > 0)
            if (!mon.isConBan()) {
                JOptionPane.showMessageDialog(this, "Hết hàng: " + mon.getTenMon(), "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            TonKho tonKho = tonKhoCache.get(maMon);
            if (tonKho == null || tonKho.getSoLuong().compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Hết hàng: " + mon.getTenMon(), "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Validate quantity input
            String qtyStr = qtyField.getText().trim();
            if (qtyStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Số Lượng không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int qty = Integer.parseInt(qtyStr);
                if (qty < 1) {
                    JOptionPane.showMessageDialog(this, "Số Lượng phải >= 1.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int existingRow = findOrderRowByMon(maMon);
                int existingQty = existingRow != -1 ? toInt(orderItemsModel.getValueAt(existingRow, 2)) : 0;
                int updatedQty = existingQty + qty;

                if (tonKho.getSoLuong().compareTo(BigDecimal.valueOf(updatedQty)) < 0) {
                    JOptionPane.showMessageDialog(this, "Vượt quá số lượng hàng còn.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                BigDecimal lineTotal = mon.getGiaBan().multiply(BigDecimal.valueOf(updatedQty));

                if (existingRow != -1) {
                    orderItemsModel.setValueAt(updatedQty, existingRow, 2);
                    orderItemsModel.setValueAt(lineTotal, existingRow, 3);
                } else {
                    orderItemsModel.addRow(new Object[]{
                            mon.getTenMon(),
                            mon.getGiaBan(),
                            qty,
                            mon.getGiaBan().multiply(BigDecimal.valueOf(qty))
                    });
                }

                qtyField.setText("1");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số Lượng phải là số nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private int findOrderRowByMon(int maMon) {
            Mon mon = monCache.get(maMon);
            if (mon == null) {
                return -1;
            }
            String tenMon = mon.getTenMon();
            for (int i = 0; i < orderItemsModel.getRowCount(); i++) {
                if (tenMon.equals(orderItemsModel.getValueAt(i, 0))) {
                    return i;
                }
            }
            return -1;
        }

        private int toInt(Object value) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        }

        private void deleteItemFromOrder() {
            int selectedRow = orderItemsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một mục để xóa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            orderItemsModel.removeRow(selectedRow);
        }

        private void clearOrderItems() {
            orderItemsModel.setRowCount(0);
            qtyField.setText("1");
        }

        private void createOrder() {
            List<OrderItem> items = collectOrderItems();
            if (items == null || items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Đơn hàng không được trống. Hãy thêm ít nhất một mục.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Refresh table statuses from database before opening dialog
            refreshTableStatusesFromDB();

            // Open table selection dialog
            GiaoDienKhuVucBan chooser = new GiaoDienKhuVucBan(GiaoDienKhuVucBan.Mode.DATBAN_MODE, CapNhat.this.tableModel);
            chooser.setAutoOccupyOnOrder(false);

            Window parent = SwingUtilities.getWindowAncestor(this);
            JDialog dlg = new JDialog(parent, "Chọn bàn cho đơn mới", Dialog.ModalityType.APPLICATION_MODAL);
            dlg.getContentPane().setLayout(new BorderLayout());
            dlg.getContentPane().add(chooser, BorderLayout.CENTER);

            JLabel info = new JLabel("Nhấn vào một bàn để chọn, sau đó xác nhận.");
            info.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            dlg.getContentPane().add(info, BorderLayout.SOUTH);

            dlg.pack();
            dlg.setLocationRelativeTo(this);

            chooser.addTableSelectionListener(table -> {
                SwingUtilities.invokeLater(() -> {
                    if (table.isTakeaway) {
                        dlg.dispose();
                        handleTakeawayOrder(table, items);
                        return;
                    }
                    // FETCH FRESH STATUS FROM DB before creating order
                    try {
                        BanDAO dao = new BanDAO();
                        Ban fresh = dao.layTheoId(table.maBan);
                        String status = fresh != null ? fresh.getTrangThai() : table.status;
                        String normalized = status != null ? status.trim().toUpperCase(Locale.ROOT) : "FREE";

                        if (!"FREE".equals(normalized) && !"RESERVED".equals(normalized)) {
                            String message = "Bàn " + table.name + " đang ở trạng thái "
                                    + translateStatusForDisplay(normalized)
                                    + ".\nChỉ có thể tạo đơn cho bàn Trống hoặc Đặt trước.";
                            JOptionPane.showMessageDialog(dlg, message, "Bàn không có sẵn", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dlg, "Lỗi kiểm tra trạng thái bàn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int ans = JOptionPane.showConfirmDialog(dlg,
                            "Xác nhận chọn bàn " + table.name + "?",
                            "Xác nhận chọn bàn",
                            JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.YES_OPTION) {
                        dlg.dispose();

                        // Prompt for number of people using the same logic as GiaoDienKhuVucBan
                        Integer soNguoi = promptForSoNguoi();
                        
                        // Update table status through model with English ENUM value (stored in DB)
                        // but TableModel will handle display as Vietnamese
                        CapNhat.this.tableModel.setStatus(table, "OCCUPIED");
                        if (soNguoi != null) {
                            table.setSoNguoi(soNguoi);
                            CapNhat.this.tableModel.updateSoNguoi(table, soNguoi);
                        }

                        // Create DonHang and ChiTietDonHang in DB
                        createAndSaveDonHang(table, soNguoi, items);
                    }
                });
            });

            dlg.setVisible(true);
        }

        private void handleTakeawayOrder(GiaoDienKhuVucBan.CafeTable takeaway, List<OrderItem> items) {
            int ans = JOptionPane.showConfirmDialog(OrderPanel.this,
                    "Tạo đơn mang đi mới?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                if (takeaway != null) {
                    createAndSaveDonHang(takeaway, null, items);
                } else {
                    JOptionPane.showMessageDialog(OrderPanel.this,
                            "Không tìm thấy bàn mang đi trong sơ đồ.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void refreshTableStatusesFromDB() {
            try {
                BanDAO banDAO = new BanDAO();
                List<Ban> bans = banDAO.layHet();
                for (Ban ban : bans) {
                    // Find the corresponding CafeTable in tableModel and update its status
                    // tableModel.tables contains all CafeTable objects
                    for (GiaoDienKhuVucBan.CafeTable table : tableModel.getTables()) {
                        if (table.maBan == ban.getMaBan()) {
                            table.status = ban.getTrangThai(); // trangThai is already in English ENUM from DAO
                            table.maDonHang = String.valueOf(ban.getMaDonHang());
                            table.capNhatCuoi = ban.getCapNhatCuoi();
                            if (ban.getSoNguoi() != null) {
                                table.setSoNguoi(ban.getSoNguoi());
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private Integer promptForSoNguoi() {
            // Exact dialog requested: loop until valid or cancelled
            while (true) {
                String input = JOptionPane.showInputDialog(OrderPanel.this, "Số người? (bỏ trống nếu không rõ)");
                if (input == null) {
                    return null; // user cancelled
                }
                String trimmed = input.trim();
                if (trimmed.isEmpty()) {
                    return null; // user left blank (valid)
                }
                try {
                    int v = Integer.parseInt(trimmed);
                    if (v >= 1) return v;
                    else JOptionPane.showMessageDialog(OrderPanel.this, "Số người phải lớn hơn hoặc bằng 1.", "Lỗi nhập", JOptionPane.ERROR_MESSAGE);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(OrderPanel.this, "Vui lòng nhập số nguyên hợp lệ hoặc để trống.", "Lỗi nhập", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void createAndSaveDonHang(GiaoDienKhuVucBan.CafeTable table, Integer soNguoi, List<OrderItem> items) {
            try {
                Thue activeTax = findActiveTax();

                // Create DonHang entity
                DonHang donHang = new DonHang();
                if (SessionContext.getCurrentUser() != null) {
                    donHang.setMaNguoiDung(SessionContext.getCurrentUser().getMaNguoiDung());
                }
                donHang.setMaBan((int)table.maBan);
                donHang.setTrangThai("dangMo");
                donHang.setLoaiDon(table.isTakeaway ? "mangVe" : "taiCho");

                // Calculate total from order items
                BigDecimal total = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    total = total.add(item.getThanhTien());
                }
                
                // Calculate tax if active tax exists
                BigDecimal tienThue = BigDecimal.ZERO;
                if (activeTax != null) {
                    tienThue = total.multiply(activeTax.getTyLe()).divide(BigDecimal.valueOf(100));
                }
                
                // Calculate final total with tax
                BigDecimal tongCuoi = total.add(tienThue);
                
                // Set both tongTien and tongCuoi (tongTien is required by DB, tongCuoi is the final after discount/tax)
                donHang.setTongTien(total);
                donHang.setTienThue(tienThue);
                donHang.setTongCuoi(tongCuoi);

                // Save DonHang
                DonHangDAO donHangDAO = new DonHangDAO();
                boolean ok = donHangDAO.them(donHang);
                if (!ok) {
                    JOptionPane.showMessageDialog(OrderPanel.this, "Lỗi tạo đơn hàng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int maDonHang = donHang.getMaDonHang();

                // Create ChiTietDonHang entries for each item and update TonKho
                ChiTietDonHangDAO chiTietDAO = new ChiTietDonHangDAO();
                TonKhoDAO tonKhoDAO = new TonKhoDAO();
                
                for (OrderItem item : items) {
                    ChiTietDonHang chiTiet = new ChiTietDonHang();
                    chiTiet.setMaDonHang(maDonHang);
                    chiTiet.setMaMon(item.maMon);
                    chiTiet.setSoLuong(item.soLuong);
                    chiTiet.setGiaBan(item.giaBan);
                    chiTiet.setThanhTien(item.getThanhTien());

                    // Set tax for this line item if active tax exists
                    if (activeTax != null) {
                        chiTiet.setMaThue(activeTax.getMaThue());
                        BigDecimal lineThue = chiTiet.getThanhTien().multiply(activeTax.getTyLe()).divide(BigDecimal.valueOf(100));
                        chiTiet.setTienThue(lineThue);
                    }

                    chiTietDAO.them(chiTiet);

                    // Update TonKho: reduce soLuong by soLuong of this order
                    TonKho tonKho = tonKhoDAO.layTheoMaMon(item.maMon);
                    if (tonKho != null) {
                        BigDecimal newSoLuong = tonKho.getSoLuong().subtract(BigDecimal.valueOf(item.soLuong));
                        tonKho.setSoLuong(newSoLuong);
                        tonKhoDAO.capNhat(tonKho);
                    }
                }

                JOptionPane.showMessageDialog(OrderPanel.this,
                        "Đơn hàng #" + maDonHang + " đã được tạo cho bàn " + table.name + ".\n" +
                        "Tổng mục: " + items.size(),
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                clearOrderItems();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(OrderPanel.this, "Lỗi lưu đơn hàng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String translateStatusForDisplay(String dbStatus) {
            if (dbStatus == null) return "Không xác định";
            switch (dbStatus.toUpperCase(Locale.ROOT)) {
                case "FREE":
                    return "Trống";
                case "OCCUPIED":
                    return "Đang sử dụng";
                case "RESERVED":
                    return "Đặt trước";
                case "MAINTENANCE":
                    return "Bảo trì";
                case "TAKEAWAY":
                    return "Mang đi";
                default:
                    return dbStatus;
            }
        }

        @Override
        public void configureForExistingOrder(int maDonHang, Runnable onSuccess) {
            this.appendMode = true;
            this.appendDonHangId = maDonHang;
            this.appendSuccessCallback = onSuccess;
            if (createOrderBtn != null) {
                createOrderBtn.setText("Thêm vào đơn");
            }
        }

        private void appendItemsToExistingOrder() {
            if (appendDonHangId == null) {
                JOptionPane.showMessageDialog(this, "Không xác định được đơn hàng cần thêm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<OrderItem> items = collectOrderItems();
            if (items == null || items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một món để thêm.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            try {
                Thue activeTax = findActiveTax();
                DonHangDAO donHangDAO = new DonHangDAO();
                DonHang donHang = donHangDAO.layTheoId(appendDonHangId);
                if (donHang == null) {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy đơn hàng cần cập nhật.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ChiTietDonHangDAO chiTietDAO = new ChiTietDonHangDAO();
                TonKhoDAO tonKhoDAO = new TonKhoDAO();

                BigDecimal totalIncrement = BigDecimal.ZERO;
                BigDecimal taxIncrement = BigDecimal.ZERO;

                for (OrderItem item : items) {
                    ChiTietDonHang chiTiet = new ChiTietDonHang();
                    chiTiet.setMaDonHang(appendDonHangId);
                    chiTiet.setMaMon(item.maMon);
                    chiTiet.setSoLuong(item.soLuong);
                    chiTiet.setGiaBan(item.giaBan);
                    BigDecimal lineTotal = item.getThanhTien();
                    chiTiet.setThanhTien(lineTotal);

                    if (activeTax != null) {
                        chiTiet.setMaThue(activeTax.getMaThue());
                        BigDecimal lineTax = lineTotal.multiply(activeTax.getTyLe()).divide(BigDecimal.valueOf(100));
                        chiTiet.setTienThue(lineTax);
                        taxIncrement = taxIncrement.add(lineTax);
                    }

                    chiTietDAO.them(chiTiet);

                    TonKho tonKho = tonKhoDAO.layTheoMaMon(item.maMon);
                    if (tonKho != null) {
                        BigDecimal newSoLuong = tonKho.getSoLuong().subtract(BigDecimal.valueOf(item.soLuong));
                        tonKho.setSoLuong(newSoLuong);
                        tonKhoDAO.capNhat(tonKho);
                    }

                    totalIncrement = totalIncrement.add(lineTotal);
                }

                BigDecimal currentTotal = donHang.getTongTien() != null ? donHang.getTongTien() : BigDecimal.ZERO;
                BigDecimal currentTax = donHang.getTienThue() != null ? donHang.getTienThue() : BigDecimal.ZERO;
                BigDecimal currentDiscount = donHang.getTienGiam() != null ? donHang.getTienGiam() : BigDecimal.ZERO;

                donHang.setTongTien(currentTotal.add(totalIncrement));
                donHang.setTienThue(currentTax.add(taxIncrement));
                BigDecimal tongCuoi = donHang.getTongTien().subtract(currentDiscount).add(donHang.getTienThue());
                donHang.setTongCuoi(tongCuoi);
                donHangDAO.capNhat(donHang);

                JOptionPane.showMessageDialog(this,
                        "Đã thêm " + items.size() + " món vào đơn #" + appendDonHangId + ".",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                clearOrderItems();
                if (appendSuccessCallback != null) {
                    appendSuccessCallback.run();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi thêm món vào đơn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private Thue findActiveTax() {
            try {
                ThueDAO thueDAO = new ThueDAO();
                List<Thue> allThues = thueDAO.layHet();
                for (Thue t : allThues) {
                    if (t.isDangApDung()) {
                        return t;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        private List<OrderItem> collectOrderItems() {
            List<OrderItem> items = new ArrayList<>();
            for (int r = 0; r < orderItemsModel.getRowCount(); r++) {
                String tenMon = String.valueOf(orderItemsModel.getValueAt(r, 0));
                Integer maMon = findMaMonByName(tenMon);
                if (maMon == null) {
                    JOptionPane.showMessageDialog(this, "Không xác định được mã món cho " + tenMon + ".", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                BigDecimal giaBan = toBigDecimal(orderItemsModel.getValueAt(r, 1));
                int soLuong = ((Number) orderItemsModel.getValueAt(r, 2)).intValue();
                items.add(new OrderItem(maMon, tenMon, giaBan, soLuong));
            }
            return items;
        }

        private Integer findMaMonByName(String tenMon) {
            for (Map.Entry<Integer, Mon> entry : monCache.entrySet()) {
                if (entry.getValue().getTenMon().equals(tenMon)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private BigDecimal toBigDecimal(Object value) {
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return new BigDecimal(value.toString());
            return new BigDecimal(value.toString());
        }

        private class OrderItem {
            final int maMon;
            final String tenMon;
            final BigDecimal giaBan;
            final int soLuong;

            OrderItem(int maMon, String tenMon, BigDecimal giaBan, int soLuong) {
                this.maMon = maMon;
                this.tenMon = tenMon;
                this.giaBan = giaBan;
                this.soLuong = soLuong;
            }

            BigDecimal getThanhTien() {
                return giaBan.multiply(BigDecimal.valueOf(soLuong));
            }
        }
    }

    class TableStatusPanel extends JPanel {
        public TableStatusPanel() {
            setLayout(new BorderLayout());
            // Only show the TableLayoutPanel as the full content using the shared model
            GiaoDienKhuVucBan layout = new GiaoDienKhuVucBan(GiaoDienKhuVucBan.Mode.TRANGTHAI_MODE, CapNhat.this.tableModel);
            layout.setTakeawayOrderPanelSupplier(() -> CapNhat.this.new OrderPanel());
            add(layout, BorderLayout.CENTER);
        }
    }
}
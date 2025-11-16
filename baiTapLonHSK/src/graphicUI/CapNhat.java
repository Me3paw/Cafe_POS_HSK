package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import components.GiaoDienKhuVucBan;
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
    class OrderPanel extends JPanel {
        private DefaultTableModel monModel;
        private JTable monTable;
        private DefaultTableModel orderItemsModel;
        private JTable orderItemsTable;
        private JTextField qtyField;
        private Map<Integer, Mon> monCache;
        private Map<Integer, TonKho> tonKhoCache;
        private Map<Integer, String> danhMucCache;

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
            JButton createOrderBtn = new JButton("Tạo đơn");
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
            createOrderBtn.addActionListener(e -> createOrder());
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

                // Calculate line total
                long lineTotal = qty * mon.getGiaBan().longValue();

                // Add to order items table
                orderItemsModel.addRow(new Object[]{
                        mon.getTenMon(),
                        mon.getGiaBan(),
                        qty,
                        lineTotal
                });

                qtyField.setText("1");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số Lượng phải là số nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
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
            if (orderItemsModel.getRowCount() == 0) {
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
                    // FETCH FRESH STATUS FROM DB before creating order
                    try {
                        BanDAO dao = new BanDAO();
                        Ban fresh = dao.layTheoId(table.maBan);
                        String status = fresh != null ? fresh.getTrangThai() : table.status;
                        
                        if ("OCCUPIED".equalsIgnoreCase(status) 
                            || "RESERVED".equalsIgnoreCase(status) 
                            || "MAINTENANCE".equalsIgnoreCase(status)) {
                            String message;
                            if ("OCCUPIED".equalsIgnoreCase(status)) {
                                message = "Bàn " + table.name + " đang được sử dụng.";
                            } else if ("RESERVED".equalsIgnoreCase(status)) {
                                message = "Bàn " + table.name + " đã được đặt trước.";
                            } else {
                                message = "Bàn " + table.name + " đang được bảo trì.";
                            }
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
                        createAndSaveDonHang(table, soNguoi);
                    }
                });
            });

            dlg.setVisible(true);
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

        private void createAndSaveDonHang(GiaoDienKhuVucBan.CafeTable table, Integer soNguoi) {
            try {
                // Get active tax (dangApDung = true)
                ThueDAO thueDAO = new ThueDAO();
                List<Thue> allThues = thueDAO.layHet();
                Thue activeTax = null;
                for (Thue t : allThues) {
                    if (t.isDangApDung()) {
                        activeTax = t;
                        break;
                    }
                }

                // Create DonHang entity
                DonHang donHang = new DonHang();
                donHang.setMaBan((int)table.maBan);
                donHang.setTrangThai("dangMo");

                // Calculate total from order items
                BigDecimal total = BigDecimal.ZERO;
                for (int r = 0; r < orderItemsModel.getRowCount(); r++) {
                    Object v = orderItemsModel.getValueAt(r, 3);
                    if (v instanceof BigDecimal) total = total.add((BigDecimal) v);
                    else if (v instanceof Number) total = total.add(BigDecimal.valueOf(((Number) v).longValue()));
                    else {
                        try { total = total.add(new BigDecimal(v.toString())); } catch (Exception ex) {}
                    }
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
                
                for (int r = 0; r < orderItemsModel.getRowCount(); r++) {
                    String tenMon = (String) orderItemsModel.getValueAt(r, 0);
                    BigDecimal giaBan = (BigDecimal) orderItemsModel.getValueAt(r, 1);
                    int soLuong = (int) orderItemsModel.getValueAt(r, 2);

                    // Find maMon by tenMon
                    int maMon = -1;
                    for (Map.Entry<Integer, Mon> entry : monCache.entrySet()) {
                        if (entry.getValue().getTenMon().equals(tenMon)) {
                            maMon = entry.getKey();
                            break;
                        }
                    }

                    if (maMon > 0) {
                        ChiTietDonHang chiTiet = new ChiTietDonHang();
                        chiTiet.setMaDonHang(maDonHang);
                        chiTiet.setMaMon(maMon);
                        chiTiet.setSoLuong(soLuong);
                        chiTiet.setGiaBan(giaBan);
                        chiTiet.setThanhTien(giaBan.multiply(BigDecimal.valueOf(soLuong)));
                        
                        // Set tax for this line item if active tax exists
                        if (activeTax != null) {
                            chiTiet.setMaThue(activeTax.getMaThue());
                            BigDecimal lineThue = chiTiet.getThanhTien().multiply(activeTax.getTyLe()).divide(BigDecimal.valueOf(100));
                            chiTiet.setTienThue(lineThue);
                        }
                        
                        chiTietDAO.them(chiTiet);
                        
                        // Update TonKho: reduce soLuong by soLuong of this order
                        TonKho tonKho = tonKhoDAO.layTheoMaMon(maMon);
                        if (tonKho != null) {
                            BigDecimal newSoLuong = tonKho.getSoLuong().subtract(BigDecimal.valueOf(soLuong));
                            tonKho.setSoLuong(newSoLuong);
                            tonKhoDAO.capNhat(tonKho);
                        }
                    }
                }

                JOptionPane.showMessageDialog(OrderPanel.this,
                        "Đơn hàng #" + maDonHang + " đã được tạo cho bàn " + table.name + ".\n" +
                        "Tổng mục: " + orderItemsModel.getRowCount(),
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);

                clearOrderItems();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(OrderPanel.this, "Lỗi lưu đơn hàng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class TableStatusPanel extends JPanel {
        public TableStatusPanel() {
            setLayout(new BorderLayout());
            // Only show the TableLayoutPanel as the full content using the shared model
            add(new GiaoDienKhuVucBan(GiaoDienKhuVucBan.Mode.TRANGTHAI_MODE, CapNhat.this.tableModel), BorderLayout.CENTER);
        }
    }
}
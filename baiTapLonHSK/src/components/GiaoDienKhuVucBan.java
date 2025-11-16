package components;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import dao.DonHangDAO;
import dao.BanDAO;
import entity.DonHang;
import entity.Ban;

import java.sql.*;

public class GiaoDienKhuVucBan extends JPanel {
    public enum Mode {
        TRANGTHAI_MODE,
        DATBAN_MODE,
        THANHTOAN_MODE // new mode for selecting a table for checkout without auto-occupying
    }

    public static final int TAKEAWAY_TABLE_ID = 0;

    private Mode mode;

    private boolean autoOccupyOnOrder = true; // default true to preserve existing behavior

    /** Listener interface to notify when a table is selected (ORDER_MODE / CHECKOUT_MODE) */
    public interface TableSelectionListener {
        void tableSelected(CafeTable table);
    }

    private final java.util.List<TableSelectionListener> selectionListeners = new ArrayList<>();

    private Supplier<JPanel> takeawayOrderPanelSupplier;

    public void addTableSelectionListener(TableSelectionListener l) {
        if (l != null) selectionListeners.add(l);
    }

    public void removeTableSelectionListener(TableSelectionListener l) {
        selectionListeners.remove(l);
    }

    public void setTakeawayOrderPanelSupplier(Supplier<JPanel> supplier) {
        this.takeawayOrderPanelSupplier = supplier;
    }

    // Nested CafeTable class represents a single table on the floor
    public static class CafeTable {
        public String name;
        public int x, y, size;
        // integer table id used for DB (positive = valid table id)
        public int maBan;
        // optional order id and last update timestamp from DB
        public String maDonHang;
        public java.sql.Timestamp capNhatCuoi;
        public String status = "FREE"; // FREE / OCCUPIED / RESERVED / MAINTENANCE / TAKEAWAY (database ENUM values)
        public boolean isCircle;
        public boolean isTakeaway = false; // true for the takeaway slot

        // UI-level number of people at the table (nullable)
        private Integer soNguoi = null;

        public CafeTable(int maBan, String name, int x, int y, int size, boolean isCircle) {
            this.maBan = maBan;
            this.name = name;
            this.x = x;
            this.y = y;
            this.size = size;
            this.isCircle = isCircle;
        }

        // Hit test for mouse clicks
        public boolean contains(int px, int py) {
            if (isCircle) {
                int cx = x + size / 2;
                int cy = y + size / 2;
                int r = size / 2;
                int dx = px - cx;
                int dy = py - cy;
                return dx * dx + dy * dy <= r * r;
            } else {
                return (px >= x && px <= x + size) && (py >= y && py <= y + size);
            }
        }

        // getter/setter for soNguoi
        public Integer getSoNguoi() { return soNguoi; }
        public void setSoNguoi(Integer soNguoi) { this.soNguoi = soNguoi; }
    }

    // small helper result to distinguish cancel vs value
    private static class InputResult {
        final boolean cancelled; // true if user cancelled dialog (input == null)
        final Integer value; // parsed value or null if left blank
        InputResult(boolean cancelled, Integer value) { this.cancelled = cancelled; this.value = value; }
    }

    /**
     * TableModel is a small observable model holding a list of CafeTable objects.
     * Panels can share the same TableModel to reflect status changes. Listeners
     * are notified when a table's status changes.
     */
    public static class TableModel {
        public interface TableModelListener {
            void tableStatusChanged(CafeTable table, String oldStatus, String newStatus);
        }

        private final List<CafeTable> tables = new ArrayList<>();
        private final List<TableModelListener> listeners = new ArrayList<>();

        public TableModel() {
        }

        public TableModel(List<CafeTable> initial) {
            if (initial != null) tables.addAll(initial);
        }

        public List<CafeTable> getTables() {
            return tables;
        }

        public void addTable(CafeTable t) {
            tables.add(t);
        }

        public void addListener(TableModelListener l) {
            if (l != null) listeners.add(l);
        }

        public void removeListener(TableModelListener l) {
            listeners.remove(l);
        }

        public void setStatus(CafeTable t, String newStatus) {
            if (t == null || newStatus == null) return;
            String old = t.status;
            if (newStatus.equals(old)) return;
            t.status = newStatus;

            // Persist to DB (best-effort) on a background thread so UI isn't blocked
            new Thread(() -> {
                try {
                    BanDAO dao = new BanDAO();
                    Ban tt = new Ban();
                    // persist using integer table id
                    tt.setMaBan(t.maBan);
                    tt.setTrangThai(newStatus);
                    // include UI-level soNguoi in persistence
                    tt.setSoNguoi(t.getSoNguoi());
                    boolean ok = dao.capNhat(tt);
                    if (!ok) {
                        dao.them(tt);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            // notify listeners on EDT
            SwingUtilities.invokeLater(() -> {
                for (TableModelListener l : new ArrayList<>(listeners)) {
                    try { l.tableStatusChanged(t, old, newStatus); } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        }

        /**
         * Update soNguoi only and persist to DB. Listeners are notified so UI can refresh.
         */
        public void updateSoNguoi(CafeTable t, Integer soNguoi) {
            if (t == null) return;
            String old = t.status;
            t.setSoNguoi(soNguoi);

            new Thread(() -> {
                try {
                    BanDAO dao = new BanDAO();
                    Ban tt = new Ban();
                    tt.setMaBan(t.maBan);
                    tt.setTrangThai(t.status);
                    tt.setSoNguoi(soNguoi);
                    boolean ok = dao.capNhat(tt);
                    if (!ok) dao.them(tt);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            SwingUtilities.invokeLater(() -> {
                for (TableModelListener l : new ArrayList<>(listeners)) {
                    try { l.tableStatusChanged(t, old, t.status); } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        }

        /**
         * Merge database-provided state into the model without persisting back to DB.
         * This sets both status and soNguoi, and notifies listeners.
         */
        public void mergeFromDB(CafeTable t, String status, Integer soNguoi) {
            if (t == null) return;
            String old = t.status;
            t.status = status != null ? status : t.status;
            t.setSoNguoi(soNguoi);
            SwingUtilities.invokeLater(() -> {
                for (TableModelListener l : new ArrayList<>(listeners)) {
                    try { l.tableStatusChanged(t, old, t.status); } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        }

        /**
         * Find a table by its integer id (maBan). Returns null if not found.
         */
        public CafeTable findTableById(int id) {
            for (CafeTable t : tables) {
                if (t != null && t.maBan == id) return t;
            }
            return null;
        }

        /**
         * Merge a list of DB states into the model. For each DB state, find matching
         * CafeTable by integer maBan and replace only status-related fields (trangThai, soNguoi, maDonHang, capNhatCuoi).
         */
        public void mergeStatuses(List<entity.Ban> states) {
            if (states == null) return;
            for (entity.Ban s : states) {
                if (s == null) continue;
                int maBan = s.getMaBan();
                if (maBan <= 0) continue;
                CafeTable ct = findTableById(maBan);
                if (ct != null) {
                    String old = ct.status;
                    ct.status = s.getTrangThai() != null ? s.getTrangThai() : ct.status;
                    ct.setSoNguoi(s.getSoNguoi());
                    Integer maDonHang = s.getMaDonHang();
                    ct.maDonHang = maDonHang != null ? String.valueOf(maDonHang) : null;
                    ct.capNhatCuoi = s.getCapNhatCuoi();
                    final CafeTable notifyTable = ct;
                    SwingUtilities.invokeLater(() -> {
                        for (TableModelListener l : new ArrayList<>(listeners)) {
                            try { l.tableStatusChanged(notifyTable, old, notifyTable.status); } catch (Exception ex) { ex.printStackTrace(); }
                        }
                    });
                }
            }
        }

        // helper to find a table by label (reuse logic from panel)
        public CafeTable findTableByLabel(String label) {
            if (label == null) return null;
            for (CafeTable t : tables) {
                if (t.name != null && t.name.equalsIgnoreCase(label)) return t;
            }
            String digits = label.replaceAll("\\D+", "");
            if (!digits.isEmpty()) {
                String tnamePrefix = "T" + digits;
                for (CafeTable t : tables) {
                    if (t.name != null && t.name.startsWith(tnamePrefix)) return t;
                }
            }
            for (CafeTable t : tables) {
                if (t.name != null && label.contains(t.name)) return t;
            }
            return null;
        }
    }

    // The underlying model (can be shared across panels)
    private final TableModel tableModel;

    // Hovered table for mouse-over feedback
    private CafeTable hoverTable = null;

    // track a pending move (source table) when user selects "Chuyển bàn" in STATUS_MODE
    private CafeTable pendingMoveSource = null;

    // NOTE: Require injection of a shared TableModel. The constructors that created
    // isolated models have been removed to prevent accidental divergent views.
    public GiaoDienKhuVucBan(Mode mode, TableModel model) {
        this.mode = mode;
        this.tableModel = model != null ? model : new TableModel(copyDefaultLayout());

        setBackground(new Color(200, 230, 255));
        // increase preferred size so takeaway fits
        setPreferredSize(new Dimension(520, 380));

        // Load table status from database on initialization
        loadTableStatusFromDB();

        // register to repaint when model changes
        this.tableModel.addListener((table, oldS, newS) -> {
            repaint();
        });

        // NOTE: removed top control panel buttons (Chuyển bàn / Chuyển trạng thái)
        // Transfers should be initiated from the occupied-table modal only.

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }

            private void handleClick(int mx, int my) {
                for (CafeTable t : tableModel.getTables()) {
                    if (t.contains(mx, my)) {
                    	
                        // If there is a pending move source, always treat this click as a destination
                        // selection for the transfer flow, regardless of the current mode. This
                        // prevents the status dialog from showing when the user is in the middle
                        // of a transfer and clicks on an empty table.
                        if (pendingMoveSource != null) {
                            // destination chosen
                            if (pendingMoveSource == t) {
                                JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Chọn bàn đích khác.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            if (t.isTakeaway || "MAINTENANCE".equalsIgnoreCase(t.status)) {
                                JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Không thể chuyển đến bàn này (Takeaway hoặc đang bảo trì).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            int ans = JOptionPane.showConfirmDialog(GiaoDienKhuVucBan.this,
                                    "Xác nhận chuyển đơn từ " + pendingMoveSource.name + " sang " + t.name + "?",
                                    "Xác nhận chuyển bàn",
                                    JOptionPane.YES_NO_OPTION);
                            if (ans == JOptionPane.YES_OPTION) {
                                tableModel.setStatus(t, "OCCUPIED");
                                tableModel.setStatus(pendingMoveSource, "FREE");
                                notifySelectionListeners(t);
                                pendingMoveSource = null;
                                setCursor(Cursor.getDefaultCursor());
                                setMode(Mode.TRANGTHAI_MODE);
                                repaint();
                            } else {
                                pendingMoveSource = null;
                                setCursor(Cursor.getDefaultCursor());
                                setMode(Mode.TRANGTHAI_MODE);
                            }

                            // handled this click, stop processing
                            break;
                        }

                        // No pending move - behave according to current mode
                        if (mode == Mode.TRANGTHAI_MODE) {
                            // STATUS_MODE is used for modal interactions (including initiating transfers).
                            showTableActionDialog(t);
                        } else if (mode == Mode.DATBAN_MODE) {
                            // ORDER_MODE behavior: select table for creating/updating order
                            if (t.isTakeaway) {
                                notifySelectionListeners(t);
                            } else if (autoOccupyOnOrder) {
                                setTableInUse(t);
                            } else {
                                notifySelectionListeners(t);
                            }
                        } else if (mode == Mode.THANHTOAN_MODE) {
                            // For checkout mode we only notify listeners and do NOT change the table status
                            notifySelectionListeners(t);
                        }
                        // Only act on the first table matched
                        break;
                    }
                }
            }

            private void showTableActionDialog(CafeTable table) {
                // If the clicked tile is the takeaway slot, show the takeaway orders list
                if (table.isTakeaway) {
                    showTakeawayOrdersDialog();
                    return;
                }

                String[] quickOptions = new String[]{"Đổi số người", "Thêm món", "Đổi trạng thái", "Hủy"};
                int choice = JOptionPane.showOptionDialog(
                        GiaoDienKhuVucBan.this,
                        "Chọn thao tác cho " + table.name + ":",
                        "Tùy chọn bàn",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        quickOptions,
                        quickOptions[0]
                );

                if (choice == 0) {
                    InputResult res = promptForSoNguoiSimple();
                    if (!res.cancelled) {
                        tableModel.updateSoNguoi(table, res.value);
                    }
                } else if (choice == 1) {
                    handleAddItemsForTable(table);
                } else if (choice == 2) {
                    showStatusManagementDialog(table);
                }
            }

            private void showStatusManagementDialog(CafeTable table) {
                // If occupied table, offer actions: update order, change table, change status, update people
                if ("OCCUPIED".equalsIgnoreCase(table.status)) {
                    String[] occupiedOptions = new String[]{"Cập nhật đơn", "Cập nhật số người", "Chuyển bàn", "Đổi trạng thái", "Hủy"};
                    int opt = JOptionPane.showOptionDialog(
                            GiaoDienKhuVucBan.this,
                            "Bàn " + table.name + " đang được phục vụ. Chọn hành động:",
                            "Bàn đang bận",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            occupiedOptions,
                            occupiedOptions[0]);

                    if (opt == 0) { // cập nhật đơn
                        JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Mở cập nhật đơn cho " + table.name + " (placeholder)", "Cập nhật đơn", JOptionPane.INFORMATION_MESSAGE);
                    } else if (opt == 1) { // cập nhật số người
                        InputResult res = promptForSoNguoiSimple();
                        if (!res.cancelled) {
                            // update via model so persistence + listeners
                            tableModel.updateSoNguoi(table, res.value);
                        }
                    } else if (opt == 2) { // chuyển bàn
                        // set pending move source; status-mode will handle the destination click
                        pendingMoveSource = table;
                        setMode(Mode.TRANGTHAI_MODE);
                        // change cursor to indicate selection mode and prompt
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Chọn bàn đích để chuyển đơn từ " + table.name + ". (Bấm vào bàn đích)", "Chuyển bàn", JOptionPane.INFORMATION_MESSAGE);
                    } else if (opt == 3) { // đổi trạng thái
                        String[] statusOptions = new String[]{"FREE", "OCCUPIED", "RESERVED", "MAINTENANCE"};
                        String statusChoice = (String) JOptionPane.showInputDialog(
                                GiaoDienKhuVucBan.this,
                                "Chọn trạng thái:",
                                "Bàn: " + table.name,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                statusOptions,
                                table.status
                        );

                        if (statusChoice != null) {
                            String dbStatus = vietnameseToDbStatus(statusChoice);
                            if ("OCCUPIED".equalsIgnoreCase(dbStatus)) {
                                // ask for number of people; if cancelled, abort status change
                                InputResult res = promptForSoNguoiSimple();
                                if (res.cancelled) {
                                    // user cancelled, do not change status
                                    return;
                                }
                                table.setSoNguoi(res.value);
                                tableModel.setStatus(table, dbStatus);
                            } else {
                                // other statuses: allow changing status without forcing people input
                                tableModel.setStatus(table, dbStatus);
                            }
                        }
                    }
                    return;
                }

                // Otherwise allow changing status between Free / Occupied / Reserved / Under maintenance
                String[] options = {"FREE", "OCCUPIED", "RESERVED", "MAINTENANCE"};
                String choice = (String) JOptionPane.showInputDialog(
                        GiaoDienKhuVucBan.this,
                        "Chọn trạng thái:",
                        "Bàn: " + table.name,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        table.status
                );

                if (choice != null) {
                    String dbStatus = vietnameseToDbStatus(choice);
                    if ("OCCUPIED".equalsIgnoreCase(dbStatus)) {
                        // when user marks Occupied, prompt for number; cancel means abort
                        InputResult res = promptForSoNguoiSimple();
                        if (res.cancelled) {
                            return; // no change
                        }
                        table.setSoNguoi(res.value);
                        tableModel.setStatus(table, dbStatus);
                    } else {
                        // changing to non-Occupied: keep soNguoi as-is (user can update via new menu action)
                        tableModel.setStatus(table, dbStatus);
                    }
                }
            }

            private void handleAddItemsForTable(CafeTable table) {
                if (table == null) {
                    return;
                }
                if (table.maBan <= 0) {
                    JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this,
                            "Bàn này chưa được gắn với mã bàn trong hệ thống.",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                Integer maDon = GiaoDienKhuVucBan.this.findOpenOrderIdForTable(table);
                if (maDon == null) {
                    JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this,
                            "Không tìm thấy hóa đơn đang mở cho " + table.name + ".",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                GiaoDienKhuVucBan.this.openExistingOrderPanel(maDon, "Thêm món cho " + table.name, null);
            }

            private InputResult promptForSoNguoiSimple() {
                // Exact dialog requested by user. Loop until valid or cancelled.
                while (true) {
                    String input = JOptionPane.showInputDialog(GiaoDienKhuVucBan.this, "Số người? (bỏ trống nếu không rõ)");
                    if (input == null) {
                        return new InputResult(true, null);
                    }
                    String trimmed = input.trim();
                    if (trimmed.isEmpty()) {
                        return new InputResult(false, null);
                    }
                    try {
                        int v = Integer.parseInt(trimmed);
                        if (v >= 1) return new InputResult(false, v);
                        else JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Số người phải lớn hơn hoặc bằng 1.", "Lỗi nhập", JOptionPane.ERROR_MESSAGE);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this, "Vui lòng nhập số nguyên hợp lệ hoặc để trống.", "Lỗi nhập", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void setTableInUse(CafeTable table) {
                // when auto-occupying for an order, prompt for number of people (editable anytime)
                InputResult res = promptForSoNguoiSimple();
                if (res.cancelled) return; // user cancelled -> do not occupy
                table.setSoNguoi(res.value);
                tableModel.setStatus(table, "OCCUPIED");
                System.out.println("Selected table for order: " + table.name);
                notifySelectionListeners(table);
            }
        });

        // track mouse movement so we can highlight potential destination tables during a pending move
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                CafeTable prev = hoverTable;
                hoverTable = findTableAt(e.getX(), e.getY());
                if (prev != hoverTable) repaint();
            }
        });
    }

    // helper to find a table at coordinates
    private CafeTable findTableAt(int x, int y) {
        for (CafeTable t : tableModel.getTables()) {
            if (t.contains(x, y)) return t;
        }
        return null;
    }

    // Helper: begin a transfer from a named table. Returns true if pending started.
    public boolean beginTransferFrom(String tableLabel) {
        CafeTable src = tableModel.findTableByLabel(tableLabel);
        if (src == null) return false;
        if (src.isTakeaway || "MAINTENANCE".equalsIgnoreCase(src.status)) {
            JOptionPane.showMessageDialog(this, "Không thể chuyển từ bàn này.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        pendingMoveSource = src;
        // use STATUS_MODE as the unified interaction mode
        setMode(Mode.TRANGTHAI_MODE);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        JOptionPane.showMessageDialog(this, "Chọn bàn đích để chuyển đơn từ " + src.name + ". (Bấm vào bàn đích)", "Chuyển bàn", JOptionPane.INFORMATION_MESSAGE);
        repaint();
        return true;
    }

    private void showTakeawayOrdersDialog() {
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Mã đơn", "Tổng cuối", "Trạng thái", "Thời gian"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable ordersTable = new JTable(tableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.setFillsViewportHeight(true);

        JLabel statusLabel = new JLabel();

        Runnable refresher = () -> {
            java.util.List<DonHang> openOrders = fetchOpenTakeawayOrders();
            tableModel.setRowCount(0);
            NumberFormat currencyFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            currencyFmt.setMaximumFractionDigits(0);
            SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm");
            for (DonHang d : openOrders) {
                String amount = d.getTongCuoi() != null ? currencyFmt.format(d.getTongCuoi()) + " đ" : "";
                String time = d.getThoiGianTao() != null ? df.format(d.getThoiGianTao()) : "";
                tableModel.addRow(new Object[]{d.getMaDonHang(), amount, d.getTrangThai(), time});
            }
            if (openOrders.isEmpty()) {
                statusLabel.setText("Chưa có đơn mang đi đang mở.");
            } else {
                statusLabel.setText("Có " + openOrders.size() + " đơn mang đi đang mở.");
            }
        };

        refresher.run();

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setPreferredSize(new Dimension(420, 220));

        JButton refreshBtn = new JButton("Tải lại");
        refreshBtn.addActionListener(e -> refresher.run());

        JButton addBtn = new JButton("Thêm món");
        if (takeawayOrderPanelSupplier == null) {
            addBtn.setEnabled(false);
        }
        addBtn.addActionListener(e -> {
            int selectedRow = ordersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this,
                        "Vui lòng chọn một đơn để thêm món.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Object val = tableModel.getValueAt(selectedRow, 0);
            if (val == null) {
                JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this,
                        "Không xác định được mã đơn hàng.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int maDonHang = Integer.parseInt(val.toString());
                openTakeawayOrderPanel(maDonHang, refresher);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(GiaoDienKhuVucBan.this,
                        "Giá trị mã đơn hàng không hợp lệ.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refreshBtn);
        actions.add(addBtn);
        footer.add(actions, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = owner instanceof Dialog
                ? new JDialog((Dialog) owner, "Đơn mang đi", Dialog.ModalityType.APPLICATION_MODAL)
                : new JDialog((Frame) owner, "Đơn mang đi", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private java.util.List<DonHang> fetchOpenTakeawayOrders() {
        try {
            DonHangDAO dao = new DonHangDAO();
            java.util.List<DonHang> byTable = dao.layTheoBan(TAKEAWAY_TABLE_ID);
            if (byTable == null) return List.of();
            java.util.List<DonHang> open = new ArrayList<>();
            for (DonHang d : byTable) {
                if (d != null && "dangMo".equalsIgnoreCase(d.getTrangThai())) {
                    open.add(d);
                }
            }
            return open;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách đơn mang đi từ cơ sở dữ liệu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return List.of();
        }
    }

    private void openTakeawayOrderPanel(int maDonHang, Runnable afterClose) {
        openExistingOrderPanel(maDonHang, "Thêm đơn mang đi", afterClose);
    }

    private void openExistingOrderPanel(int maDonHang, String dialogTitle, Runnable afterClose) {
        if (takeawayOrderPanelSupplier == null) {
            JOptionPane.showMessageDialog(this, "Không thể mở giao diện thêm món vì chưa cấu hình.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPanel orderPanel;
        try {
            orderPanel = takeawayOrderPanelSupplier.get();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tạo giao diện thêm món.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (orderPanel == null) {
            JOptionPane.showMessageDialog(this, "Không thể mở giao diện thêm món.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        String title = dialogTitle != null ? dialogTitle : "Thêm món";
        JDialog addDialog = owner instanceof Dialog
                ? new JDialog((Dialog) owner, title, Dialog.ModalityType.APPLICATION_MODAL)
                : new JDialog((Frame) owner, title, true);
        addDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        if (orderPanel instanceof OrderPanelBridge) {
            ((OrderPanelBridge) orderPanel).configureForExistingOrder(maDonHang, addDialog::dispose);
        } else {
            JOptionPane.showMessageDialog(addDialog,
                    "Giao diện thêm món không hỗ trợ chế độ thêm vào đơn có sẵn.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
        addDialog.getContentPane().add(orderPanel, BorderLayout.CENTER);
        addDialog.pack();
        addDialog.setLocationRelativeTo(owner);
        addDialog.setVisible(true);

        if (afterClose != null) {
            afterClose.run();
        }
    }

    private Integer findOpenOrderIdForTable(CafeTable table) {
        if (table == null || table.maBan <= 0) {
            return null;
        }
        try {
            DonHangDAO dao = new DonHangDAO();
            java.util.List<DonHang> orders = dao.layTheoBan(table.maBan);
            if (orders == null || orders.isEmpty()) {
                return null;
            }
            for (DonHang order : orders) {
                if (order != null && !isPaidOrder(order)) {
                    return order.getMaDonHang();
                }
            }
            for (DonHang order : orders) {
                if (order != null) {
                    return order.getMaDonHang();
                }
            }
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Không thể xác định hóa đơn của " + table.name + ".",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private boolean isPaidOrder(DonHang order) {
        if (order == null || order.getTrangThai() == null) return false;
        String status = order.getTrangThai().replaceAll("\\s+", "");
        String normalized = Normalizer.normalize(status, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized.contains("PAID")
                || normalized.contains("THANHTOAN")
                || normalized.contains("HOANTAT");
    }

    private void notifySelectionListeners(CafeTable table) {
        if (!selectionListeners.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (TableSelectionListener l : new ArrayList<>(selectionListeners)) {
                    try {
                        l.tableSelected(table);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    // Programmatically occupy a table (useful when selection requires confirmation outside the panel)
    public void occupyTable(CafeTable table) {
        if (table != null) {
            tableModel.setStatus(table, "OCCUPIED");
            notifySelectionListeners(table);
        }
    }

    public void setAutoOccupyOnOrder(boolean v) { this.autoOccupyOnOrder = v; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // draw each table with color based on status
            for (CafeTable t : tableModel.getTables()) {
                Color fill;
                // Check status - internally stored as English enums but convert to Vietnamese for display logic
                String displayStatus = dbStatusToVietnamese(t.status);
                
                switch (displayStatus) {
                    case "Đang sử dụng":
                        fill = Color.RED;
                        break;
                    case "Đặt trước":
                        fill = Color.ORANGE;
                        break;
                    case "Takeaway":
                        fill = new Color(80, 140, 200);
                        break;
                    case "Bảo trì":
                        fill = new Color(120, 120, 120);
                        break;
                    default:
                        fill = Color.GREEN.darker();
                        break;
                }

                // draw shadow / border for a nicer look
                g2.setColor(fill);
                if (t.isCircle) {
                    g2.fillOval(t.x, t.y, t.size, t.size);
                    g2.setColor(Color.DARK_GRAY);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(t.x, t.y, t.size, t.size);
                } else {
                    g2.fillRect(t.x, t.y, t.size, t.size);
                    g2.setColor(Color.DARK_GRAY);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(t.x, t.y, t.size, t.size);
                }

                // Draw label centered; include soNguoi if set (format: "T <maBan> | P<soNguoi>")
                String label = buildTableLabel(t);

                g2.setColor(Color.WHITE);
                Font font = getFont().deriveFont(Font.BOLD, 14f);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(label);
                int textH = fm.getAscent();
                int tx = t.x + (t.size - textW) / 2;
                int ty = t.y + (t.size + textH) / 2 - 4; // small upward tweak
                g2.drawString(label, tx, ty);

                // (removed separate corner soNguoi hint — now integrated into label)
            }

            // If a pending move exists, highlight source and hovered destination
            if (pendingMoveSource != null) {
                // highlight source with yellow thick border
                g2.setColor(new Color(220, 200, 40));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRect(pendingMoveSource.x - 4, pendingMoveSource.y - 4, pendingMoveSource.size + 8, pendingMoveSource.size + 8);

                // highlight hovered table if valid
                if (hoverTable != null && hoverTable != pendingMoveSource) {
                    // invalid destinations: takeaway or under maintenance
                    boolean invalid = hoverTable.isTakeaway || "MAINTENANCE".equalsIgnoreCase(hoverTable.status);
                    if (!invalid) {
                        g2.setColor(new Color(40, 180, 60));
                    } else {
                        g2.setColor(new Color(180, 40, 40));
                    }
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(hoverTable.x - 3, hoverTable.y - 3, hoverTable.size + 6, hoverTable.size + 6);
                }

                // draw instruction text top-left
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Chọn bàn đích để chuyển từ " + pendingMoveSource.name, 12, 18);
            }

            // subtle hover hint when not moving
            if (pendingMoveSource == null && hoverTable != null) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(hoverTable.x, hoverTable.y, hoverTable.size, hoverTable.size);
            }


        } finally {
            g2.dispose();
        }
    }

    // Optional: allow changing mode at runtime
    public void setMode(Mode mode) {
        this.mode = mode;
        // reset cursor if leaving order mode where a pending move might have set a custom cursor
        if (mode != Mode.DATBAN_MODE) {
            setCursor(Cursor.getDefaultCursor());
        }
        // Control buttons removed; nothing else to update here.
    }

    public Mode getMode() {
        return mode;
    }

    // Optional: expose tables for tests or external manipulation
    public List<CafeTable> getTables() {
        return tableModel.getTables();
    }

    // Public getter for tableModel to allow refresh operations
    public TableModel getTableModel() {
        return tableModel;
    }

    private String buildTableLabel(CafeTable t) {
        if (t == null) return "";
        if (t.isTakeaway) {
            return t.name != null ? t.name : "Mang đi";
        }
        StringBuilder sb = new StringBuilder();
        if (t.maBan > 0) {
            sb.append("T ").append(t.maBan);
        } else if (t.name != null) {
            sb.append(t.name);
        }
        if (t.getSoNguoi() != null) {
            sb.append(" | P").append(t.getSoNguoi());
        }
        return sb.toString();
    }

    // Build the default layout for this instance (creates a fresh copy)
    private List<CafeTable> buildDefaultTables() {
        // delegate to static factory to keep one canonical layout definition
        return new ArrayList<>(copyDefaultLayout());
    }

    private void loadTableStatusFromDB() {
        new Thread(() -> {
            try {
                BanDAO banDAO = new BanDAO();
                List<Ban> dbStates = banDAO.layHet();
                tableModel.mergeStatuses(dbStates);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // Helper: convert DB enum status (FREE/OCCUPIED/etc) to Vietnamese for UI display
    private String dbStatusToVietnamese(String dbStatus) {
        if (dbStatus == null) return "Trống";
        switch (dbStatus.toUpperCase()) {
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

    // Helper: convert Vietnamese UI status to DB enum
    private String vietnameseToDbStatus(String uiStatus) {
        if (uiStatus == null) return null;
        String lower = uiStatus.toLowerCase();
        if (lower.contains("trống") || lower.contains("trong") || lower.contains("free")) return "FREE";
        if (lower.contains("đang sử dụng") || lower.contains("dang su dung") || lower.contains("occupied")) return "OCCUPIED";
        if (lower.contains("đặt trước") || lower.contains("dat truoc") || lower.contains("reserved")) return "RESERVED";
        if (lower.contains("bảo trì") || lower.contains("bao tri") || lower.contains("maintenance")) return "MAINTENANCE";
        if (lower.contains("takeaway")) return "TAKEAWAY";
        return uiStatus.toUpperCase();
    }

        // Provide a static copy of the default layout so other panels (e.g. checkout view)
        // can use the same table names/positions without sharing the same objects.
        public static List<CafeTable> copyDefaultLayout() {
            List<CafeTable> def = new ArrayList<>();

            int startX = 30;      // minor adjustments for nicer visual centering
            int startY = 30;
            int gapX = 25;
            int gapY = 50;

            // === TOP ROW: 5 rectangular tables ===
            int rectW = 90;
            int rectH = 60;

            for (int i = 0; i < 5; i++) {
                int num = 1 + i;
                int x = startX + i * (rectW + gapX);
                int y = startY;
                def.add(new CafeTable(num, "T" + num + " | P8", x, y, rectW, false));
            }

            // === MIDDLE: 4 circular tables ===
            int circSize = 80;
            int midY = startY + rectH + gapY;

            // fine-tuned offsets to mimic the picture layout (slightly curved)
            int[] middleXOffsets = {
                    startX + 40,
                    startX + 40 + circSize + 20,
                    startX + 40 + circSize * 2 + 40,
                    startX + 40 + circSize * 3 + 60
            };

            for (int i = 0; i < 4; i++) {
                int num = 6 + i;
                int x = middleXOffsets[i];
                int y = midY + (i == 1 ? 10 : i == 2 ? -5 : 0); // small vertical curve
                def.add(new CafeTable(num, "T" + num + " | P8", x, y, circSize, true));
            }

            // === BOTTOM ROW: 4 rectangular tables ===
            int botY = midY + circSize + gapY;

            for (int i = 0; i < 4; i++) {
                int num = 11 + i;
                int x = startX + i * (rectW + gapX);
                def.add(new CafeTable(num, "T" + num + " | P8", x, botY, rectW, false));
            }

            // === TAKEAWAY RECTANGLE on the far right ===
            int twW = 110;
            int twH = rectH;
            int twX = startX + 5 * (rectW + gapX) + 20;
            int twY = startY;

            CafeTable takeaway = new CafeTable(TAKEAWAY_TABLE_ID, "Mang đi", twX, twY, twW, false);
            takeaway.isTakeaway = true;
            takeaway.status = "TAKEAWAY";
            def.add(takeaway);

            return def;
        }

}

package graphicUI;

import dao.ChiTietDonHangDAO;
import dao.DonHangDAO;
import dao.KhachHangDAO;
import dao.TonKhoDAO;
import entity.ChiTietDonHang;
import entity.DonHang;
import entity.KhachHang;
import entity.TonKho;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * ReportPanel contains tabs for revenue and export features.
 */
public class BaoBieu extends JPanel {
    private final JTabbedPane tabs;
    private final ReportData reportData = new ReportData();
    private final ExportPanel exportPanel;

    public BaoBieu() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        SalesReportPanel salesReportPanel = new SalesReportPanel(reportData, this::reportUpdated);
        exportPanel = new ExportPanel(reportData);
        tabs.addTab("Doanh thu", salesReportPanel);
        tabs.addTab("Xuất", exportPanel);
        add(tabs, BorderLayout.CENTER);
    }

    private void reportUpdated() {
        exportPanel.refreshState();
    }

    static class SalesReportPanel extends JPanel {
        private final DonHangDAO donHangDAO = new DonHangDAO();
        private final KhachHangDAO khachHangDAO = new KhachHangDAO();
        private final TonKhoDAO tonKhoDAO = new TonKhoDAO();
        private final ChiTietDonHangDAO chiTietDonHangDAO = new ChiTietDonHangDAO();
        private final DefaultTableModel tableModel;
        private final JTable table;
        private final JComboBox<PeriodOption> periodCombo;
        private final JLabel rangeLabel = new JLabel("Khoảng thời gian: -");
        private final JLabel revenueLabel = new JLabel("Doanh thu: 0");
        private final JLabel profitLabel = new JLabel("Lợi nhuận: 0");
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        private final Map<Integer, KhachHang> khachHangMap = new HashMap<>();
        private final ReportData reportData;
        private final Runnable reportUpdatedCallback;

        public SalesReportPanel(ReportData reportData, Runnable reportUpdatedCallback) {
            this.reportData = reportData;
            this.reportUpdatedCallback = reportUpdatedCallback;
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            tableModel = new DefaultTableModel(new Object[]{
                    "Mã đơn", "Khách hàng", "SĐT", "Tổng tiền", "Giảm", "Thuế", "Tổng cuối", "Giá nhập", "Trạng thái", "Ngày tạo"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(tableModel);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel controls = new JPanel();
            controls.add(new JLabel("Khoảng thời gian:"));
            periodCombo = new JComboBox<>(PeriodOption.values());
            controls.add(periodCombo);
            JButton generateBtn = new JButton("Tạo báo cáo");
            generateBtn.addActionListener(e -> performReport());
            controls.add(generateBtn);
            topPanel.add(controls, BorderLayout.WEST);
            add(topPanel, BorderLayout.NORTH);

            JPanel summaryPanel = new JPanel(new GridLayout(3, 1));
            summaryPanel.add(rangeLabel);
            summaryPanel.add(revenueLabel);
            summaryPanel.add(profitLabel);
            add(summaryPanel, BorderLayout.SOUTH);

            loadKhachHangCache();
        }

        private void loadKhachHangCache() {
            khachHangMap.clear();
            try {
                List<KhachHang> customers = khachHangDAO.layHet();
                if (customers != null) {
                    for (KhachHang kh : customers) {
                        khachHangMap.put(kh.getMaKhachHang(), kh);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void performReport() {
            PeriodOption option = (PeriodOption) periodCombo.getSelectedItem();
            if (option == null) {
                return;
            }
            PeriodRange range = option.createRange(LocalDateTime.now());
            List<DonHang> allOrders = new ArrayList<>();
            try {
                List<DonHang> dbOrders = donHangDAO.layHet();
                if (dbOrders != null) {
                    allOrders.addAll(dbOrders);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu hóa đơn", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

            List<DonHang> filtered = new ArrayList<>();
            for (DonHang order : allOrders) {
                if (order.getThoiGianTao() == null) {
                    continue;
                }
                LocalDateTime created = LocalDateTime.ofInstant(order.getThoiGianTao().toInstant(), ZoneId.systemDefault());
                if (!created.isBefore(range.getStart()) && created.isBefore(range.getEnd())) {
                    filtered.add(order);
                }
            }

            Map<Integer, BigDecimal> orderCostMap = calculateOrderCosts(filtered);
            refreshTable(filtered, orderCostMap);

            BigDecimal revenue = calculateRevenue(filtered);
            BigDecimal tonKhoCost = orderCostMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = revenue.subtract(tonKhoCost);

            rangeLabel.setText("Khoảng thời gian: " + option.describeRange(range));
            revenueLabel.setText("Doanh thu: " + currencyFormat.format(revenue));
            profitLabel.setText("Lợi nhuận: " + currencyFormat.format(profit));

            reportData.update(filtered, range, option.getDisplayName(), revenue, profit);
            if (reportUpdatedCallback != null) {
                reportUpdatedCallback.run();
            }
        }

        private BigDecimal calculateRevenue(List<DonHang> orders) {
            BigDecimal revenue = BigDecimal.ZERO;
            for (DonHang order : orders) {
                if (order.getTongTien() != null) {
                    revenue = revenue.add(order.getTongTien());
                }
            }
            return revenue;
        }

        private Map<Integer, BigDecimal> calculateOrderCosts(List<DonHang> orders) {
            Map<Integer, BigDecimal> costByOrder = new HashMap<>();
            if (orders.isEmpty()) {
                return costByOrder;
            }

            Map<Integer, BigDecimal> giaNhapByMon = loadGiaNhapByMon();
            for (DonHang order : orders) {
                BigDecimal totalCost = BigDecimal.ZERO;
                try {
                    List<ChiTietDonHang> details = chiTietDonHangDAO.layTheoDonHang(order.getMaDonHang());
                    for (ChiTietDonHang detail : details) {
                        Integer maMon = detail.getMaMon();
                        if (maMon == null) {
                            continue;
                        }
                        BigDecimal giaNhap = giaNhapByMon.get(maMon);
                        if (giaNhap == null) {
                            continue;
                        }
                        BigDecimal soLuong = BigDecimal.valueOf(detail.getSoLuong());
                        totalCost = totalCost.add(giaNhap.multiply(soLuong));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                costByOrder.put(order.getMaDonHang(), totalCost);
            }
            return costByOrder;
        }

        private Map<Integer, BigDecimal> loadGiaNhapByMon() {
            Map<Integer, BigDecimal> giaNhapByMon = new HashMap<>();
            try {
                List<TonKho> tonKhos = tonKhoDAO.layHet();
                if (tonKhos != null) {
                    for (TonKho tk : tonKhos) {
                        if (tk.getGiaNhap() != null) {
                            giaNhapByMon.put(tk.getMaMon(), tk.getGiaNhap());
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return giaNhapByMon;
        }

        private void refreshTable(List<DonHang> orders, Map<Integer, BigDecimal> orderCostMap) {
            tableModel.setRowCount(0);
            for (DonHang order : orders) {
                KhachHang kh = order.getMaKhachHang() != null ? khachHangMap.get(order.getMaKhachHang()) : null;
                BigDecimal giaNhapValue = orderCostMap.getOrDefault(order.getMaDonHang(), BigDecimal.ZERO);
                tableModel.addRow(new Object[]{
                        order.getMaDonHang(),
                        kh != null ? kh.getHoTen() : "Khách lẻ",
                        kh != null ? kh.getSoDienThoai() : "",
                        formatCurrency(order.getTongTien()),
                        formatCurrency(order.getTienGiam()),
                        formatCurrency(order.getTienThue()),
                        formatCurrency(order.getTongCuoi()),
                        formatCurrency(giaNhapValue),
                        order.getTrangThai(),
                        formatTimestamp(order.getThoiGianTao())
                });
            }
        }

        private String formatCurrency(BigDecimal value) {
            if (value == null) {
                value = BigDecimal.ZERO;
            }
            return currencyFormat.format(value);
        }

        private String formatTimestamp(java.sql.Timestamp ts) {
            if (ts == null) {
                return "";
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime ldt = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
            return fmt.format(ldt);
        }
    }

    static class ExportPanel extends JPanel {
        private final ReportData reportData;
        private final JButton pdfButton = new JButton("Xuất ra file PDF");
        private final JButton csvButton = new JButton("Xuất ra file CSV");
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        public ExportPanel(ReportData reportData) {
            this.reportData = reportData;
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
            add(new JLabel("Xuất báo cáo:"));
            pdfButton.addActionListener(e -> exportPdf());
            csvButton.addActionListener(e -> exportCsv());
            add(pdfButton);
            add(csvButton);
            refreshState();
        }

        void refreshState() {
            boolean hasData = !reportData.getOrders().isEmpty();
            pdfButton.setEnabled(hasData);
            csvButton.setEnabled(hasData);
        }

        private void exportPdf() {
            exportFile(true);
        }

        private void exportCsv() {
            exportFile(false);
        }

        private void exportFile(boolean pdf) {
            if (reportData.getOrders().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa có dữ liệu để xuất", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(pdf ? "bao-cao-doanh-thu.pdf" : "bao-cao-doanh-thu.csv"));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = ensureExtension(chooser.getSelectedFile(), pdf ? ".pdf" : ".csv");
            try {
                if (pdf) {
                    writePdf(file);
                } else {
                    writeCsv(file);
                }
                JOptionPane.showMessageDialog(this, "Đã xuất báo cáo: " + file.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Xuất báo cáo thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        private File ensureExtension(File file, String ext) {
            String lower = file.getName().toLowerCase(Locale.ROOT);
            if (!lower.endsWith(ext)) {
                String path = file.getAbsolutePath() + ext;
                return new File(path);
            }
            return file;
        }

        private void writePdf(File file) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write("BÁO CÁO DOANH THU");
                writer.newLine();
                writer.write("Chu kỳ: " + reportData.getPeriodLabel());
                writer.newLine();
                writer.write("Khoảng thời gian: " + reportData.describeRange());
                writer.newLine();
                writer.write("Tổng doanh thu: " + currencyFormat.format(reportData.getRevenue()));
                writer.newLine();
                writer.write("Lợi nhuận: " + currencyFormat.format(reportData.getProfit()));
                writer.newLine();
                writer.newLine();
                writer.write(String.format("%-10s %-20s %-15s %-15s", "Mã", "Ngày tạo", "Tổng cuối", "Trạng thái"));
                writer.newLine();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (DonHang order : reportData.getOrders()) {
                    String date = order.getThoiGianTao() != null
                            ? fmt.format(LocalDateTime.ofInstant(order.getThoiGianTao().toInstant(), ZoneId.systemDefault()))
                            : "";
                    String total = order.getTongCuoi() != null ? currencyFormat.format(order.getTongCuoi()) : currencyFormat.format(BigDecimal.ZERO);
                    writer.write(String.format("%-10d %-20s %-15s %-15s", order.getMaDonHang(), date, total, Objects.toString(order.getTrangThai(), "")));
                    writer.newLine();
                }
            }
        }

        private void writeCsv(File file) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write("ma_don,thoi_gian_tao,ma_khach,tong_tien,tien_giam,tien_thue,tong_cuoi,trang_thai,loai_don");
                writer.newLine();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (DonHang order : reportData.getOrders()) {
                    String date = order.getThoiGianTao() != null
                            ? fmt.format(LocalDateTime.ofInstant(order.getThoiGianTao().toInstant(), ZoneId.systemDefault()))
                            : "";
                    writer.write(String.join(",",
                            String.valueOf(order.getMaDonHang()),
                            escapeCsv(date),
                            order.getMaKhachHang() == null ? "" : String.valueOf(order.getMaKhachHang()),
                            safeNumeric(order.getTongTien()),
                            safeNumeric(order.getTienGiam()),
                            safeNumeric(order.getTienThue()),
                            safeNumeric(order.getTongCuoi()),
                            escapeCsv(Objects.toString(order.getTrangThai(), "")),
                            escapeCsv(Objects.toString(order.getLoaiDon(), ""))
                    ));
                    writer.newLine();
                }
            }
        }

        private String safeNumeric(BigDecimal value) {
            return value != null ? value.toPlainString() : "0";
        }

        private String escapeCsv(String input) {
            if (input == null) {
                return "";
            }
            if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
                return '"' + input.replace("\"", "\"\"") + '"';
            }
            return input;
        }
    }

    static class ReportData {
        private List<DonHang> orders = new ArrayList<>();
        private PeriodRange range;
        private String periodLabel = "";
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;

        public void update(List<DonHang> orders, PeriodRange range, String periodLabel, BigDecimal revenue, BigDecimal profit) {
            this.orders = new ArrayList<>(orders);
            this.range = range;
            this.periodLabel = periodLabel;
            this.revenue = revenue;
            this.profit = profit;
        }

        public List<DonHang> getOrders() {
            return new ArrayList<>(orders);
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public String describeRange() {
            if (range == null) {
                return "-";
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return fmt.format(range.getStart()) + " - " + fmt.format(range.getEnd());
        }
    }

    enum PeriodOption {
        DAY("Theo ngày") {
            @Override
            public PeriodRange createRange(LocalDateTime now) {
                LocalDate startDate = now.toLocalDate();
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = start.plusDays(1);
                return new PeriodRange(start, end);
            }
        },
        MONTH("Theo tháng") {
            @Override
            public PeriodRange createRange(LocalDateTime now) {
                LocalDate startDate = now.toLocalDate().withDayOfMonth(1);
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = start.plusMonths(1);
                return new PeriodRange(start, end);
            }
        },
        YEAR("Theo năm") {
            @Override
            public PeriodRange createRange(LocalDateTime now) {
                LocalDate startDate = now.toLocalDate().withDayOfYear(1);
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = start.plusYears(1);
                return new PeriodRange(start, end);
            }
        };

        private final String displayName;

        PeriodOption(String displayName) {
            this.displayName = displayName;
        }

        public abstract PeriodRange createRange(LocalDateTime now);

        public String getDisplayName() {
            return displayName;
        }

        public String describeRange(PeriodRange range) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return fmt.format(range.getStart()) + " - " + fmt.format(range.getEnd());
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static class PeriodRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        PeriodRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }
    }
}

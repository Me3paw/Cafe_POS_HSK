package graphicUI;

import javax.swing.*;
import java.awt.*;

/**
 * ReportPanel contains tabs for daily/weekly/monthly revenue, top items, and export.
 */
public class ReportPanel extends JPanel {
    private JTabbedPane tabs;

    public ReportPanel() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Doanh thu", new SalesReportPanel());
        tabs.addTab("Mặt hàng bán chạy", new TopItemsReportPanel());
        tabs.addTab("Xuất", new ExportPanel());
        add(tabs, BorderLayout.CENTER);
    }

    static class SalesReportPanel extends JPanel {
        public SalesReportPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("Báo cáo doanh thu ngày/tuần/tháng (placeholder)"), BorderLayout.NORTH);
            JPanel p = new JPanel();
            p.add(new JComboBox<>(new String[] {"Ngày","Tuần","Tháng"}));
            p.add(new JButton("Tạo báo cáo"));
            add(p, BorderLayout.CENTER);
        }
    }

    static class TopItemsReportPanel extends JPanel {
        public TopItemsReportPanel() {
            setLayout(new BorderLayout(8,8));
            add(new JLabel("Báo cáo mặt hàng bán chạy (placeholder)"), BorderLayout.NORTH);
            add(new JScrollPane(new JTable(new String[][] {{"P001","Cà phê","50"}}, new String[] {"Mã","Tên","Số lượng"})), BorderLayout.CENTER);
        }
    }

    static class ExportPanel extends JPanel {
        public ExportPanel() {
            setLayout(new FlowLayout());
            add(new JLabel("Xuất báo cáo:"));
            add(new JButton("Xuất Excel (placeholder)"));
            add(new JButton("Xuất PDF (placeholder)"));
        }
    }
}

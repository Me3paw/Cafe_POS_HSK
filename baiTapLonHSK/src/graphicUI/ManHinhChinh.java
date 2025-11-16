package graphicUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import components.GiaoDienKhuVucBan;
import dao.BanDAO;
import entity.Ban;
import entity.NguoiDung;
import java.util.List;

/**
 * MainFrame is the main application window. It has a left sidebar menu and a CardLayout content area.
 */
public class ManHinhChinh extends JFrame {
    private JPanel menuPanel;
    private JPanel contentPanel;
    private CardLayout contentLayout;

    // Card names
    public static final String SYSTEM = "SYSTEM";
    public static final String CATALOG = "CATALOG";
    public static final String UPDATE = "UPDATE";
    public static final String SEARCH = "SEARCH";
    public static final String OPERATION = "OPERATION";
    public static final String REPORT = "REPORT";

    private SystemPanel systemPanel;
    private DanhMuc catalogPanel;
    private CapNhat updatePanel;
    private TimKiem searchPanel;
    private XuLi operationPanel;
    private BaoBieu reportPanel;

    // single shared TableModel instance
    private final GiaoDienKhuVucBan.TableModel sharedTableModel = new GiaoDienKhuVucBan.TableModel(GiaoDienKhuVucBan.copyDefaultLayout());

    public ManHinhChinh() {
        super("Café POS - Phiên bản mẫu");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setPreferredSize(new Dimension(220, 0));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        addMenuButton("1. Hệ thống", SYSTEM);
        addMenuButton("2. Danh mục", CATALOG);
        addMenuButton("3. Cập nhật", UPDATE);
        addMenuButton("4. Tìm kiếm", SEARCH);
        addMenuButton("5. Xử lý", OPERATION);
        addMenuButton("6. Báo biểu", REPORT);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);

        systemPanel = new SystemPanel();
        catalogPanel = new DanhMuc();
        // initialize panels with shared model
        updatePanel = new CapNhat(sharedTableModel);
        searchPanel = new TimKiem(this);
        operationPanel = new XuLi(this, sharedTableModel);
        reportPanel = new BaoBieu();

        contentPanel.add(systemPanel, SYSTEM);
        contentPanel.add(catalogPanel, CATALOG);
        contentPanel.add(updatePanel, UPDATE);
        contentPanel.add(searchPanel, SEARCH);
        contentPanel.add(operationPanel, OPERATION);
        contentPanel.add(reportPanel, REPORT);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(menuPanel, BorderLayout.WEST);
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        // After UI components are created, load statuses from DB and merge into model
        mergeTableStatusesFromDB();

        // Default view: Update -> Đơn hàng mới
        showCard(UPDATE);
        updatePanel.showDefault();
    }

    private void mergeTableStatusesFromDB() {
        try {
            BanDAO dao = new BanDAO();
            // Ensure DB has rows for our default layout when empty
            dao.khoiTao(sharedTableModel.getTables());
            List<Ban> list = dao.layHet();
            if (list != null) {
                // mergeStatuses expects integer maBan values in the DB rows
                sharedTableModel.mergeStatuses(list);
            }
        } catch (Exception ex) {
            // do not block UI; just log
            ex.printStackTrace();
        }
    }

    private void addMenuButton(String title, String card) {
        JButton btn = new JButton(title);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btn.addActionListener((ActionEvent e) -> {
            showCard(card);
            if (UPDATE.equals(card)) {
                updatePanel.showDefault();
            }
        });
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(btn);
    }

    private void showCard(String card) {
        contentLayout.show(contentPanel, card);
    }

    public static void main(String[] args) {
        // Use system look and feel (still pure Swing)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            DangNhapDialog loginDialog = new DangNhapDialog(null);
            NguoiDung user = loginDialog.showDialog();
            if (user == null) {
                System.exit(0);
                return;
            }
            SessionContext.setCurrentUser(user);
            ManHinhChinh f = new ManHinhChinh();
            f.setTitle("Café POS - " + user.getHoTen() + " (" + user.getVaiTro() + ")");
            f.setVisible(true);
        });
    }
}
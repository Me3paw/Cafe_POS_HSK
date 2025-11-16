package graphicUI;

import connectDB.DBConnection;
import dao.DanhMucDAO;
import dao.MonDAO;
import dao.TonKhoDAO;
import entity.Mon;
import entity.TonKho;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CatalogPanel manages product and inventory (Tồn Kho) CRUD forms.
 */
public class DanhMuc extends JPanel {
    private JTabbedPane tabs;
    private ProductCRUDPanel productPanel;
    private TonKhoPanel tonKhoPanel;

    public DanhMuc() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        productPanel = new ProductCRUDPanel();
        tonKhoPanel = new TonKhoPanel(productPanel);
        tabs.addTab("Sản phẩm", productPanel);
        tabs.addTab("Tồn Kho", tonKhoPanel);
        add(tabs, BorderLayout.CENTER);
    }

    // Product CRUD inner class
    static class ProductCRUDPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;
        private JTextField txtId, txtName, txtPrice, txtMoTa;
        private JCheckBox chkConBan;
        private JComboBox<entity.DanhMuc> cboDanhMuc;
        private boolean editing = false;
        private int editingRow = -1;

        private MonDAO monDAO = new MonDAO();
        private DanhMucDAO danhMucDAO = new DanhMucDAO();
        private Map<Integer, String> danhMucMap = new HashMap<>();

        public ProductCRUDPanel() {
            setLayout(new BorderLayout(8,8));
            model = new DefaultTableModel(new Object[] {"Mã món", "Tên", "Danh mục", "Giá bán", "Còn bán", "Mô tả"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.EAST);

            // When a row is clicked, load it into the form
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int r = table.getSelectedRow();
                    if (r >= 0) {
                        loadFromSelectedRow(r);
                    }
                }
            });

            loadCategories();
            loadData();
        }

        private JPanel buildForm() {
            JPanel p = new JPanel(new GridLayout(0,1,6,6));
            p.setBorder(BorderFactory.createTitledBorder("Thông tin sản phẩm"));
            txtId = new JTextField(); txtId.setEditable(false);
            txtName = new JTextField();
            txtPrice = new JTextField();
            cboDanhMuc = new JComboBox<>();
            chkConBan = new JCheckBox("Còn bán");
            txtMoTa = new JTextField();

            p.add(new JLabel("Mã (auto):")); p.add(txtId);
            p.add(new JLabel("Tên:")); p.add(txtName);
            p.add(new JLabel("Danh mục:")); p.add(cboDanhMuc);
            p.add(new JLabel("Giá:")); p.add(txtPrice);
            p.add(chkConBan);
            p.add(new JLabel("Mô tả:")); p.add(txtMoTa);

            // Buttons under the form: Lưu, Xóa, Sửa
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
            JButton btnSave = new JButton("Lưu");
            JButton btnDelete = new JButton("Xóa");
            JButton btnEdit = new JButton("Xóa trắng");

            btnSave.addActionListener((ActionEvent e) -> {
                String name = txtName.getText().trim();
                String priceS = txtPrice.getText().trim();
                entity.DanhMuc sel = (entity.DanhMuc) cboDanhMuc.getSelectedItem();
                boolean conBan = chkConBan.isSelected();
                String moTa = txtMoTa.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Tên là bắt buộc.");
                    return;
                }
                BigDecimal price;
                try {
                    price = new BigDecimal(priceS.isEmpty() ? "0" : priceS);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Giá không hợp lệ.");
                    return;
                }
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(this, "Giá phải lớn hơn 0.");
                    return;
                }

                // Validate price against TonKho giaNhap
                String maTonS = txtId.getText().trim();
                if (!maTonS.isEmpty()) {
                    try {
                        int maMon = Integer.parseInt(maTonS);
                        TonKho tk = new TonKhoDAO().layTheoMaMon(maMon);
                        if (tk != null && tk.getGiaNhap() != null && price.compareTo(tk.getGiaNhap()) <= 0) {
                            JOptionPane.showMessageDialog(this, "Giá bán phải lớn hơn giá nhập (" + tk.getGiaNhap() + ").");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        // Not a valid number yet, let DAO handle validation
                    }
                }

                Mon m = new Mon();
                if (sel != null) m.setMaDanhMuc(sel.getMaDanhMuc());
                m.setTenMon(name);
                m.setGiaBan(price);
                m.setConBan(conBan);
                m.setMoTa(moTa);

                boolean ok;
                // If an ID is present and exists in DB, update. Otherwise create new.
                String idS = txtId.getText().trim();
                if (!idS.isEmpty()) {
                    try {
                        int ma = Integer.parseInt(idS);
                        if (monDAO.kiemTraMa(ma)) {
                            m.setMaMon(ma);
                            ok = monDAO.capNhat(m);
                        } else {
                            // id provided but not found -> create new (ignore id)
                            ok = monDAO.them(m);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Id không hợp lệ.");
                        return;
                    }
                } else {
                    ok = monDAO.them(m);
                }
                if (ok) {
                    loadData();
                    clearForm();
                    editing = false;
                    editingRow = -1;
                } else {
                    JOptionPane.showMessageDialog(this, "Lưu thất bại.");
                }
            });

            btnDelete.addActionListener((ActionEvent e) -> {
                String idS = txtId.getText().trim();
                if (idS.isEmpty()) {
                    // try table selection
                    int r = table.getSelectedRow();
                    if (r < 0) { JOptionPane.showMessageDialog(this, "Chọn 1 dòng hoặc điền Mã để xóa."); return; }
                    idS = String.valueOf(model.getValueAt(r,0));
                }
                int maMon;
                try { maMon = Integer.parseInt(idS); } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Mã không hợp lệ."); return; }
                boolean ok = monDAO.xoa(maMon);
                if (ok) {
                    loadData();
                    clearForm();
                    editing = false;
                    editingRow = -1;
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại.");
                }
            });

            // Xóa trắng: clear all fields in the form
            btnEdit.addActionListener((ActionEvent e) -> {
                clearForm();
                editing = false;
                editingRow = -1;
            });

            btnPanel.add(btnSave); btnPanel.add(btnDelete); btnPanel.add(btnEdit);
            p.add(btnPanel);

            p.setPreferredSize(new Dimension(320,0));
            return p;
        }

        private void loadFromSelectedRow(int r) {
            txtId.setText(String.valueOf(model.getValueAt(r,0)));
            txtName.setText(String.valueOf(model.getValueAt(r,1)));
            String tenDM = String.valueOf(model.getValueAt(r,2));
            // select danh muc in combo
            for (int i = 0; i < cboDanhMuc.getItemCount(); i++) {
                entity.DanhMuc dm = cboDanhMuc.getItemAt(i);
                if (dm != null && dm.getTenDanhMuc().equals(tenDM)) {
                    cboDanhMuc.setSelectedIndex(i);
                    break;
                }
            }
            txtPrice.setText(String.valueOf(model.getValueAt(r,3)));
            chkConBan.setSelected(Boolean.parseBoolean(String.valueOf(model.getValueAt(r,4))));
            txtMoTa.setText(String.valueOf(model.getValueAt(r,5)));
        }

        private void clearForm() {
            txtId.setText("");
            txtName.setText("");
            txtPrice.setText("");
            txtMoTa.setText("");
            chkConBan.setSelected(false);
            if (cboDanhMuc.getItemCount() > 0) cboDanhMuc.setSelectedIndex(0);
        }

        private void loadCategories() {
            cboDanhMuc.removeAllItems();
            danhMucMap.clear();
            List<entity.DanhMuc> ds = danhMucDAO.layHet();
            if (ds != null) {
                for (entity.DanhMuc d : ds) {
                    cboDanhMuc.addItem(d);
                    danhMucMap.put(d.getMaDanhMuc(), d.getTenDanhMuc());
                }
            }
        }

        private void loadData() {
            model.setRowCount(0);
            List<Mon> ds = monDAO.layHet();
            if (ds != null) {
                for (Mon m : ds) {
                    Integer maDM = m.getMaDanhMuc();
                    String tenDM = maDM == null ? "" : danhMucMap.getOrDefault(maDM, "");
                    model.addRow(new Object[] {
                            m.getMaMon(),
                            m.getTenMon(),
                            tenDM,
                            m.getGiaBan(),
                            m.isConBan(),
                            m.getMoTa()
                    });
                }
            }
        }

        public void refreshData() {
            loadData();
        }
    }

    // TonKho panel - list and simple edit of donVi, soLuong, giaNhap
    static class TonKhoPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;
        private JTextField txtMaTon, txtMon, txtDonVi, txtSoLuong, txtGiaNhap, txtMucCanhBao, txtCapNhat;
        private TonKhoDAO tonKhoDAO = new TonKhoDAO();
        private MonDAO monDAO = new MonDAO();
        private ProductCRUDPanel productCRUDPanel;

        public TonKhoPanel(ProductCRUDPanel productCRUDPanel) {
            this.productCRUDPanel = productCRUDPanel;
            setLayout(new BorderLayout(8,8));
            model = new DefaultTableModel(new Object[] {"Mã Tồn", "Mã Món", "Tên món", "Đơn vị", "Số lượng", "Giá nhập", "Mức cảnh báo", "Cập nhật"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            // form to edit selected record
            JPanel form = new JPanel(new GridLayout(0,1,6,6));
            form.setBorder(BorderFactory.createTitledBorder("Chỉnh sửa tồn kho"));
            txtMaTon = new JTextField(); txtMaTon.setEditable(false);
            txtMon = new JTextField(); txtMon.setEditable(false);
            txtDonVi = new JTextField();
            txtSoLuong = new JTextField();
            txtGiaNhap = new JTextField();
            txtMucCanhBao = new JTextField(); txtMucCanhBao.setEditable(false);
            txtCapNhat = new JTextField(); txtCapNhat.setEditable(false);

            form.add(new JLabel("Mã tồn:")); form.add(txtMaTon);
            form.add(new JLabel("Mã món / Tên:")); form.add(txtMon);
            form.add(new JLabel("Đơn vị:")); form.add(txtDonVi);
            form.add(new JLabel("Số lượng:")); form.add(txtSoLuong);
            form.add(new JLabel("Giá nhập:")); form.add(txtGiaNhap);
            form.add(new JLabel("Mức cảnh báo:")); form.add(txtMucCanhBao);
            form.add(new JLabel("Cập nhật cuối:")); form.add(txtCapNhat);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER,6,6));
            JButton btnSave = new JButton("Lưu");
            JButton btnRefresh = new JButton("Tải lại");
            btns.add(btnSave); btns.add(btnRefresh);
            form.add(btns);
            form.setPreferredSize(new Dimension(360,0));
            add(form, BorderLayout.EAST);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int r = table.getSelectedRow();
                    if (r >= 0) loadFromRow(r);
                }
            });

            btnRefresh.addActionListener((ActionEvent e) -> loadData());

            btnSave.addActionListener((ActionEvent e) -> {
                String maTonS = txtMaTon.getText().trim();
                if (maTonS.isEmpty()) { 
                    JOptionPane.showMessageDialog(this, "Chọn 1 bản ghi để lưu."); 
                    return; 
                }

                int maTon;
                try { 
                    maTon = Integer.parseInt(maTonS); 
                } catch (NumberFormatException ex) { 
                    JOptionPane.showMessageDialog(this, "Mã tồn không hợp lệ."); 
                    return; 
                }

                // Lấy bản ghi tồn kho theo mã món (đang chọn trong bảng)
                TonKho t = tonKhoDAO.layTheoMaMon(
                    Integer.parseInt(String.valueOf(model.getValueAt(table.getSelectedRow(),1)))
                );

                if (t == null) { 
                    JOptionPane.showMessageDialog(this, "Bản ghi tồn kho không còn tồn tại."); 
                    loadData(); 
                    return; 
                }

                // Parse and update fields
                t.setDonVi(txtDonVi.getText().trim());

                try { 
                    t.setSoLuong(new BigDecimal(txtSoLuong.getText().trim())); 
                } catch (Exception ex) { 
                    JOptionPane.showMessageDialog(this, "Số lượng không hợp lệ."); 
                    return; 
                }

                try { 
                    t.setGiaNhap(new BigDecimal(txtGiaNhap.getText().trim())); 
                } catch (Exception ex) { 
                    JOptionPane.showMessageDialog(this, "Giá nhập không hợp lệ."); 
                    return; 
                }

                Timestamp now = new Timestamp(System.currentTimeMillis());
                t.setCapNhatCuoi(now);
                txtCapNhat.setText(now.toString());

                boolean ok = tonKhoDAO.capNhat(t);

                if (ok) { 
                    JOptionPane.showMessageDialog(this, "Lưu thành công."); 
                    loadData();

                    // Refresh Product panel too
                    if (productCRUDPanel != null) {
                        productCRUDPanel.refreshData();
                    }
                } else { 
                    JOptionPane.showMessageDialog(this, "Lưu thất bại."); 
                }
            });


            loadData();
        }

        private void loadFromRow(int r) {
            txtMaTon.setText(String.valueOf(model.getValueAt(r,0)));
            txtMon.setText(String.valueOf(model.getValueAt(r,1)) + " / " + String.valueOf(model.getValueAt(r,2)));
            txtDonVi.setText(String.valueOf(model.getValueAt(r,3)));
            txtSoLuong.setText(String.valueOf(model.getValueAt(r,4)));
            txtGiaNhap.setText(String.valueOf(model.getValueAt(r,5)));
            txtMucCanhBao.setText(String.valueOf(model.getValueAt(r,6)));
            txtCapNhat.setText(String.valueOf(model.getValueAt(r,7)));
        }

        private void loadData() {
            model.setRowCount(0);
            List<TonKho> ds = tonKhoDAO.layHet();
            Map<Integer, String> monNames = new HashMap<>();
            List<Mon> mons = monDAO.layHet();
            if (mons != null) for (Mon m : mons) monNames.put(m.getMaMon(), m.getTenMon());
            if (ds != null) {
                for (TonKho t : ds) {
                    String ten = monNames.getOrDefault(t.getMaMon(), "");
                    model.addRow(new Object[] {
                            t.getMaTon(),
                            t.getMaMon(),
                            ten,
                            t.getDonVi(),
                            t.getSoLuong(),
                            t.getGiaNhap(),
                            t.getMucCanhBao(),
                            t.getCapNhatCuoi()
                    });
                }
            }
        }
    }
}

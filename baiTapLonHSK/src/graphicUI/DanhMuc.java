package graphicUI;

import connectDB.DBConnection;
import dao.DanhMucDAO;
import dao.MonDAO;
import entity.Mon;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CatalogPanel manages product and combo CRUD forms.
 */
public class DanhMuc extends JPanel {
    private JTabbedPane tabs;

    public DanhMuc() {
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Sản phẩm", new ProductCRUDPanel());
        tabs.addTab("Combo món", new ComboCRUDPanel());
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
            model = new DefaultTableModel(new Object[] {"MaMon", "TenMon", "TenDanhMuc", "GiaBan", "ConBan", "MoTa"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.EAST);
            add(buildButtons(), BorderLayout.SOUTH);

            loadCategories();
            loadData();
        }

        private JPanel buildForm() {
            JPanel p = new JPanel(new GridLayout(12,1,6,6));
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
            p.setPreferredSize(new Dimension(320,0));
            return p;
        }

        private JPanel buildButtons() {
            JPanel b = new JPanel();
            JButton add = new JButton("Thêm");
            JButton edit = new JButton("Cập nhật");
            JButton del = new JButton("Xóa");
            JButton save = new JButton("Lưu");
            JButton cancel = new JButton("Sửa");

            add.addActionListener((ActionEvent e) -> {
                clearForm();
                editing = false;
                editingRow = -1;
            });
            edit.addActionListener((ActionEvent e) -> {
                int r = table.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Chọn 1 dòng để sửa.");
                    return;
                }
                editing = true;
                editingRow = r;
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
            });
            del.addActionListener((ActionEvent e) -> {
                int r = table.getSelectedRow();
                if (r < 0) { JOptionPane.showMessageDialog(this, "Chọn 1 dòng để xóa."); return; }
                int maMon = Integer.parseInt(String.valueOf(model.getValueAt(r,0)));
                boolean ok = monDAO.xoa(maMon);
                if (ok) {
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại.");
                }
            });
            save.addActionListener((ActionEvent e) -> {
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
                Mon m = new Mon();
                if (sel != null) m.setMaDanhMuc(sel.getMaDanhMuc());
                m.setTenMon(name);
                m.setGiaBan(price);
                m.setConBan(conBan);
                m.setMoTa(moTa);

                boolean ok;
                if (editing && editingRow >= 0) {
                    try {
                        m.setMaMon(Integer.parseInt(txtId.getText()));
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Id không hợp lệ.");
                        return;
                    }
                    ok = monDAO.capNhat(m);
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
            cancel.addActionListener((ActionEvent e) -> {
                clearForm();
                editing = false;
                editingRow = -1;
            });

            b.add(add); b.add(edit); b.add(del); b.add(save); b.add(cancel);
            return b;
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
    }

    // Combo CRUD inner class (placeholder)
    static class ComboCRUDPanel extends JPanel {
        private DefaultTableModel model;
        private JTable table;

        public ComboCRUDPanel() {
            setLayout(new BorderLayout(8,8));
            model = new DefaultTableModel(new Object[] {"ID", "Tên combo", "Giá"}, 0);
            table = new JTable(model);
            model.addRow(new Object[] {"C001","Combo 1","65000"});
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel buttons = new JPanel();
            buttons.add(new JButton("Add"));
            buttons.add(new JButton("Edit"));
            buttons.add(new JButton("Delete"));
            buttons.add(new JButton("Save"));
            buttons.add(new JButton("Cancel"));
            add(buttons, BorderLayout.SOUTH);
        }
    }
}
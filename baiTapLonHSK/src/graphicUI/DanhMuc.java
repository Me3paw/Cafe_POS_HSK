package graphicUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

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
        private JTextField txtId, txtName, txtPrice, txtStock;
        private boolean editing = false;
        private int editingRow = -1;

        public ProductCRUDPanel() {
            setLayout(new BorderLayout(8,8));
            model = new DefaultTableModel(new Object[] {"ID", "Tên", "Giá", "Tồn kho"}, 0);
            table = new JTable(model);
            model.addRow(new Object[] {"P001","Cà phê sữa","25000","20"});
            model.addRow(new Object[] {"P002","Trà sữa","30000","15"});

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.EAST);
            add(buildButtons(), BorderLayout.SOUTH);
        }

        private JPanel buildForm() {
            JPanel p = new JPanel(new GridLayout(8,1,6,6));
            p.setBorder(BorderFactory.createTitledBorder("Thông tin sản phẩm"));
            txtId = new JTextField();
            txtName = new JTextField();
            txtPrice = new JTextField();
            txtStock = new JTextField();
            p.add(new JLabel("Mã:")); p.add(txtId);
            p.add(new JLabel("Tên:")); p.add(txtName);
            p.add(new JLabel("Giá:")); p.add(txtPrice);
            p.add(new JLabel("Tồn kho:")); p.add(txtStock);
            p.setPreferredSize(new Dimension(260,0));
            return p;
        }

        private JPanel buildButtons() {
            JPanel b = new JPanel();
            JButton add = new JButton("Add");
            JButton edit = new JButton("Edit");
            JButton del = new JButton("Delete");
            JButton save = new JButton("Save");
            JButton cancel = new JButton("Cancel");

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
                txtPrice.setText(String.valueOf(model.getValueAt(r,2)));
                txtStock.setText(String.valueOf(model.getValueAt(r,3)));
            });
            del.addActionListener((ActionEvent e) -> {
                int r = table.getSelectedRow();
                if (r < 0) { JOptionPane.showMessageDialog(this, "Chọn 1 dòng để xóa."); return; }
                model.removeRow(r);
            });
            save.addActionListener((ActionEvent e) -> {
                String id = txtId.getText().trim();
                String name = txtName.getText().trim();
                String price = txtPrice.getText().trim();
                String stock = txtStock.getText().trim();
                if (id.isEmpty() || name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Mã và tên là bắt buộc.");
                    return;
                }
                if (editing && editingRow >= 0) {
                    model.setValueAt(id, editingRow, 0);
                    model.setValueAt(name, editingRow, 1);
                    model.setValueAt(price, editingRow, 2);
                    model.setValueAt(stock, editingRow, 3);
                } else {
                    model.addRow(new Object[] {id, name, price, stock});
                }
                clearForm();
                editing = false;
                editingRow = -1;
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
            txtStock.setText("");
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

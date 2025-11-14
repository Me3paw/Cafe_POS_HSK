package components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TableLayoutPanel extends JPanel {

    // Table object
    static class CafeTable {
        String name;
        int x, y, size;
        boolean isCircle;

        CafeTable(String name, int x, int y, int size, boolean isCircle) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.size = size;
            this.isCircle = isCircle;
        }

        boolean contains(int px, int py) {
            if (isCircle) {
                int cx = x + size / 2;
                int cy = y + size / 2;
                int r = size / 2;
                return Math.pow(px - cx, 2) + Math.pow(py - cy, 2) <= r * r;
            } else {
                return (px >= x && px <= x + size) && (py >= y && py <= y + size);
            }
        }
    }

    private final List<CafeTable> tables = new ArrayList<>();

    public TableLayoutPanel() {
        setBackground(new Color(200, 230, 255));  // blue background

        // TOP row (square)
        tables.add(new CafeTable("T1®1", 40, 40, 60, false));
        tables.add(new CafeTable("T2®1", 140, 40, 60, false));
        tables.add(new CafeTable("T3®1", 240, 40, 60, false));
        tables.add(new CafeTable("T4®1", 340, 40, 60, false));

        // MIDDLE row (circle)
        tables.add(new CafeTable("T6®1", 80, 150, 70, true));
        tables.add(new CafeTable("T8®1", 200, 150, 70, true));
        tables.add(new CafeTable("T9®1", 320, 150, 70, true));

        // BOTTOM row (square)
        tables.add(new CafeTable("T11®1", 40, 280, 60, false));
        tables.add(new CafeTable("T12®1", 140, 280, 60, false));
        tables.add(new CafeTable("T13®1", 240, 280, 60, false));
        tables.add(new CafeTable("T14®1", 340, 280, 60, false));

        // CLICK listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (CafeTable t : tables) {
                    if (t.contains(e.getX(), e.getY())) {
                        JOptionPane.showMessageDialog(null, "Clicked: " + t.name);
                    }
                }
            }
        });
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.DARK_GRAY);

        for (CafeTable t : tables) {
            if (t.isCircle) {
                g2.fillOval(t.x, t.y, t.size, t.size);
            } else {
                g2.fillRect(t.x, t.y, t.size, t.size);
            }

            // Draw label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(t.name);
            int textH = fm.getHeight();

            int tx = t.x + (t.size - textW) / 2;
            int ty = t.y + (t.size + textH / 2) / 2;

            g2.drawString(t.name, tx, ty);
            g2.setColor(Color.DARK_GRAY);
        }
    }
}


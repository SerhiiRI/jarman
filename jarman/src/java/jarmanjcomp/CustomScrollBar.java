package jarman.jarmanjcomp;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneLayout;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class CustomScrollBar {
    public static void main(String[] args) {
        JTextArea cmp = new JTextArea();
        String str = "a";
        for (int i = 0; i < 20; i++) {
            cmp.append(str + str + "\n");
        }
        cmp.setBackground(Color.decode("#eeeeee"));
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.getContentPane().add(myScrollPane(cmp, "#cccccc"));
        f.setSize(320, 240);
        f.setVisible(true);
    }

    public static JScrollPane myScrollPane(JComponent cmp, String clr) {
        JScrollPane scrollPane = new JScrollPane(cmp);
        scrollPane.setComponentZOrder(scrollPane.getVerticalScrollBar(), 0);
        scrollPane.setComponentZOrder(scrollPane.getViewport(), 1);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new MyScrollBarUI(clr));
        scrollPane.setLayout(new ScrollPaneLayout() {
            @Override
            public void layoutContainer(Container parent) {
                JScrollPane scrollPane = (JScrollPane) parent;
                scrollPane.setBackground(Color.BLUE);
                Rectangle availR = scrollPane.getBounds();
                availR.x = availR.y = 0;

                Insets parentInsets = parent.getInsets();
                availR.x = parentInsets.left;
                availR.y = parentInsets.top;
                availR.width -= parentInsets.left + parentInsets.right;
                availR.height -= parentInsets.top + parentInsets.bottom;

                Rectangle vsbR = new Rectangle();
                vsbR.width = 16;
                vsbR.height = availR.height;
                vsbR.x = availR.x + availR.width - vsbR.width;
                vsbR.y = availR.y;

                if (viewport != null) {
                    viewport.setBounds(availR);
                }
                if (vsb != null) {
                    vsb.setVisible(true);
                    vsb.setBounds(vsbR);
                }
            }
        });
        return scrollPane;
    }

    static class MyScrollBarUI extends BasicScrollBarUI {
        private final Dimension d = new Dimension();
	public String clr;
	public MyScrollBarUI(String clr){
	    this.clr = clr;
	}

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d;
                }
            };
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d;
                }
            };
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = null;
            JScrollBar sb = (JScrollBar) c ;
            if (!sb.isEnabled() || r.width > r.height) {
                return;
            } else if (isDragging) {
                color = (Color.decode(this.clr));
            } else if (isThumbRollover()) {
                color = (Color.decode(this.clr));
            } else {
                color = (Color.decode(this.clr));
            }
	    sb.setBorder(new EmptyBorder(0, 8, 0, 0));
            g2.setPaint(color);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 0, 0);
            g2.setPaint(Color.decode(this.clr));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 0, 0);
            g2.dispose();
        }

        @Override
        protected void setThumbBounds(int x, int y, int width, int height) {
            super.setThumbBounds(x, y, width, height);
            scrollbar.repaint();
        }
    }
}


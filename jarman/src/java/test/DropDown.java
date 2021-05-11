package jarman.test;


import java.awt.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class DropDown {
    static class ColorArrowUI extends BasicComboBoxUI {
        public static ComboBoxUI createUI(JComponent c) {
            return new ColorArrowUI();
        }

        @Override protected JButton createArrowButton() {
            return new BasicArrowButton(
                    BasicArrowButton.SOUTH,
                    (new java.awt.Color(221, 221, 221)), (new java.awt.Color(221, 221, 221)),
                    Color.gray, (new java.awt.Color(221, 221, 221)));
        }
    }

    public static JComboBox getBar(String[] data){
        UIManager.put("ScrollBar.width", new Integer(0));
        JComboBox comboBox;
        comboBox = new JComboBox(data);
        comboBox.setSelectedItem(null);
        comboBox.setUI(ColorArrowUI.createUI(comboBox));
        comboBox.setForeground(Color.darkGray);
        comboBox.setBounds(1500, 200, 300, 40);
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup basicComboPopup = new BasicComboPopup(comboBox);
                basicComboPopup.setBorder(new LineBorder(Color.lightGray));
                return basicComboPopup;
            }
        });
        return comboBox;
    }

    public static void main(final String[] args) {
        String[] data = new String[]{"a","b","c","d","e","f","g","h","i"};
        String[] dat = new String[]{"a","b"};
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(getBar(data));
        final JFrame frame = new JFrame("MainComboBoxUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

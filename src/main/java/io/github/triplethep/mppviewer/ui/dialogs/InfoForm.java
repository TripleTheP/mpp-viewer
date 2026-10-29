package io.github.triplethep.mppviewer.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Formular mit schreibgeschützten Feldern in zwei Spaltenpaaren (Beschriftung | Wert | Beschriftung | Wert).
 */
final class InfoForm extends JPanel
{
   private static final long serialVersionUID = 1L;
   private int row;
   private int col;

   InfoForm()
   {
      super(new GridBagLayout());
      setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
   }

   /**
    * Feld über die volle Breite.
    */
   InfoForm wide(String label, String value)
   {
      if (col != 0)
      {
         row++;
         col = 0;
      }
      add(label(label), gbc(0, row, 1, 0));
      add(text(label, value), gbc(1, row, 3, 1));
      row++;
      return this;
   }

   /**
    * Feld in der linken oder rechten Hälfte.
    */
   InfoForm half(String label, String value)
   {
      add(label(label), gbc(col * 2, row, 1, 0));
      add(text(label, value), gbc(col * 2 + 1, row, 1, 1));
      col++;
      if (col == 2)
      {
         col = 0;
         row++;
      }
      return this;
   }

   InfoForm check(String label, boolean value)
   {
      JCheckBox box = new JCheckBox(label, value);
      box.setEnabled(false);
      box.setOpaque(false);
      add(box, gbc(col * 2, row, 2, 0));
      col++;
      if (col == 2)
      {
         col = 0;
         row++;
      }
      return this;
   }

   InfoForm component(JComponent c)
   {
      if (col != 0)
      {
         row++;
         col = 0;
      }
      GridBagConstraints g = gbc(0, row++, 4, 1);
      g.weighty = 1;
      g.fill = GridBagConstraints.BOTH;
      add(c, g);
      return this;
   }

   InfoForm finish()
   {
      GridBagConstraints g = gbc(0, ++row, 4, 1);
      g.weighty = 1;
      add(new JLabel(), g);
      return this;
   }

   private static JLabel label(String text)
   {
      JLabel l = new JLabel(text == null ? "" : text.replace("\u00AD", ""));
      l.setForeground(new Color(0x404040));
      return l;
   }

   private static JTextField text(String label, String value)
   {
      JTextField t = new JTextField(value == null ? "" : value, 14);
      t.setEditable(false);
      t.setBackground(new Color(0xFAFAFA));
      t.getAccessibleContext().setAccessibleName(label);
      return t;
   }

   private static GridBagConstraints gbc(int x, int y, int w, double weightx)
   {
      GridBagConstraints g = new GridBagConstraints();
      g.gridx = x;
      g.gridy = y;
      g.gridwidth = w;
      g.weightx = weightx;
      g.fill = weightx > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
      g.anchor = GridBagConstraints.WEST;
      g.insets = new Insets(3, x == 0 ? 0 : 6, 3, 6);
      return g;
   }
}

package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.CellContent;
import io.github.triplethep.mppviewer.render.Style;
import org.mpxj.Resource;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * „Ressource: Tabelle“.
 */
public final class ResourceView extends JPanel
{
   private static final long serialVersionUID = 1L;

   private final ProjectViewModel model;
   private final SheetTable table;
   private Consumer<Resource> onOpenResource = r -> {
   };

   public ResourceView(ProjectViewModel model)
   {
      super(new BorderLayout());
      this.model = model;
      List<Resource> resources = model.getDoc().getResources();
      table = new SheetTable(model.getResourceColumns(), resources::size, (row, col) -> cell(row.intValue(), col))
      {
         private static final long serialVersionUID = 1L;

         @Override
         protected String tooltipForIcons(int row, CellContent c)
         {
            Resource r = resources.get(row);
            if (r.getNotes() == null || r.getNotes().isBlank())
            {
               return null;
            }
            return "<html><b>" + I18n.t("indicator.NOTE") + ":</b> " + r.getNotes().strip().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</html>";
         }
      };
      JScrollPane scroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(Color.WHITE);
      JPanel corner = new JPanel();
      corner.setBackground(Style.HEADER_BG);
      scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
      add(scroll, BorderLayout.CENTER);

      table.addMouseListener(new MouseAdapter()
      {
         @Override
         public void mouseClicked(MouseEvent e)
         {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
            {
               int row = table.rowAtPoint(e.getPoint());
               if (row >= 0 && row < resources.size())
               {
                  onOpenResource.accept(resources.get(row));
               }
            }
         }
      });
      KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
      table.getInputMap(JComponent.WHEN_FOCUSED).put(copy, "copyRows");
      table.getActionMap().put("copyRows", new javax.swing.AbstractAction()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e)
         {
            copySelection();
         }
      });
   }

   private CellContent cell(int row, ColumnDef col)
   {
      List<Resource> resources = model.getDoc().getResources();
      if (row < 0 || row >= resources.size())
      {
         return CellContent.plain("", col.getAlign());
      }
      return model.getPresenter().resource(resources.get(row), col);
   }

   public void refresh()
   {
      table.refresh();
   }

   public SheetTable getTable()
   {
      return table;
   }

   public List<ColumnDef> currentColumns()
   {
      return table.currentColumnDefs();
   }

   public void setOnOpenResource(Consumer<Resource> handler)
   {
      this.onOpenResource = handler;
   }

   public Resource getSelectedResource()
   {
      int row = table.getSelectedRow();
      List<Resource> resources = model.getDoc().getResources();
      return row >= 0 && row < resources.size() ? resources.get(row) : null;
   }

   public void copySelection()
   {
      int[] selected = table.getSelectedRows();
      if (selected.length == 0)
      {
         return;
      }
      List<ColumnDef> cols = table.currentColumnDefs();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < cols.size(); i++)
      {
         if (i > 0)
         {
            sb.append('\t');
         }
         sb.append(cols.get(i).getKind() == ColumnDef.Kind.INDICATORS ? I18n.t("column.indicators") : cols.get(i).getTitle());
      }
      sb.append('\n');
      List<Resource> resources = model.getDoc().getResources();
      for (int r : selected)
      {
         for (int i = 0; i < cols.size(); i++)
         {
            if (i > 0)
            {
               sb.append('\t');
            }
            Object v = model.getPresenter().resourceExportValue(resources.get(r), cols.get(i));
            String text = cols.get(i).getKind() == ColumnDef.Kind.FIELD ? model.getPresenter().resource(resources.get(r), cols.get(i)).text() : v == null ? "" : v.toString();
            sb.append(text.replace('\t', ' ').replace('\n', ' '));
         }
         sb.append('\n');
      }
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
   }
}

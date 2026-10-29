package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.i18n.I18n;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Führt längere Arbeiten (Export, Druck, Laden) im Hintergrund aus und meldet Fehler verständlich.
 */
public final class Tasks
{
   private static Consumer<String> statusSink = s -> {
   };

   private Tasks()
   {
   }

   /**
    * Ziel für Statusmeldungen (Statusleiste des Hauptfensters).
    */
   public static void setStatusSink(Consumer<String> sink)
   {
      statusSink = sink == null ? s -> {
      } : sink;
   }

   public static <T> void run(Component parent, String message, Callable<T> work, Consumer<T> onSuccess)
   {
      Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
      Component target = window != null ? window : parent;
      if (target != null)
      {
         target.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      statusSink.accept(message);
      new SwingWorker<T, Void>()
      {
         @Override
         protected T doInBackground() throws Exception
         {
            return work.call();
         }

         @Override
         protected void done()
         {
            if (target != null)
            {
               target.setCursor(Cursor.getDefaultCursor());
            }
            statusSink.accept(I18n.t("status.ready"));
            try
            {
               T result = get();
               if (onSuccess != null)
               {
                  onSuccess.accept(result);
               }
            }
            catch (InterruptedException ex)
            {
               Thread.currentThread().interrupt();
            }
            catch (ExecutionException ex)
            {
               Throwable cause = ex.getCause() == null ? ex : ex.getCause();
               showError(parent, cause);
            }
         }
      }.execute();
   }

   public static void showError(Component parent, Throwable t)
   {
      String detail = t.getLocalizedMessage();
      if (detail == null || detail.isBlank())
      {
         detail = t.getClass().getSimpleName();
      }
      JOptionPane.showMessageDialog(parent, I18n.t("error.generic", detail), AppInfo.NAME, JOptionPane.ERROR_MESSAGE);
   }
}

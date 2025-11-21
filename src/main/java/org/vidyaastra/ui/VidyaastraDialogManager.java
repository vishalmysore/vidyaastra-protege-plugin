package org.vidyaastra.ui;

import java.awt.Component;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.protege.editor.core.ui.util.UIUtil;

public class VidyaastraDialogManager
{
   public int showConfirmDialog(Component parent, String title, String message)
   {
      return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
   }

   public String showInputDialog(Component parent, String message)
   {
      return JOptionPane.showInputDialog(parent, message, "Input", JOptionPane.OK_CANCEL_OPTION);
   }

   public void showMessageDialog(Component parent, String message)
   {
      JOptionPane.showMessageDialog(parent, message, "Message", JOptionPane.INFORMATION_MESSAGE);
   }

   public void showErrorMessageDialog(Component parent, String message)
   {
      JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
   }

   public File showOpenFileChooser(Component parent, String title, String fileExtension, String fileDescription)
   {
      Set<String> extensions = new HashSet<>();
      for (String ext : fileExtension.split(",")) {
         extensions.add(ext.trim());
      }
      return UIUtil.openFile(parent, title, fileDescription, extensions);
   }

   public File showSaveFileChooser(Component parent, String title, String fileExtension, String fileDescription,
         boolean overwrite)
   {
      Set<String> extensions = new HashSet<>();
      extensions.add(fileExtension);
      return UIUtil.saveFile(parent, title, fileDescription, extensions, null);
   }
}

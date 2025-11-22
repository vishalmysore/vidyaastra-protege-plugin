package org.vidyaastra.action;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import org.vidyaastra.ui.view.AIQueryPanel;
import org.vidyaastra.ui.VidyaastraDialogManager;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.model.OWLOntology;

public class VidyaastraAction extends ProtegeOWLAction
{
   private static final long serialVersionUID = 1L;

   private VidyaastraDialogManager dialogManager;
   private OWLEditorKit editorKit;

   @Override
   public void initialise() throws Exception
   {
      dialogManager = new VidyaastraDialogManager();
      editorKit = getOWLEditorKit();
   }

   @Override
   public void actionPerformed(ActionEvent e)
   {
      try {
         showVidyaastraDialog();
      } catch (Exception ex) {
         dialogManager.showErrorMessageDialog(null, "Error opening Vidyaastra AI Integration: " + ex.getMessage());
         ex.printStackTrace();
      }
   }

   private void showVidyaastraDialog()
   {
      final OWLOntology currentOntology = getOWLModelManager().getActiveOntology();
      final OWLWorkspace editorWindow = editorKit.getOWLWorkspace();
      AIQueryPanel.showDialog(currentOntology, editorKit);
   }

   @Override
   public void dispose() throws Exception
   {
      // NO-OP
   }
}

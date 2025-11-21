package org.vidyaastra.ui.view;

import java.awt.BorderLayout;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VidyaastraGraphView is a tab view component for Protege that displays
 * an interactive graph visualization of the ontology structure.
 */
public class VidyaastraGraphView extends AbstractOWLViewComponent
{
   private static final long serialVersionUID = 1L;
   private static final Logger logger = LoggerFactory.getLogger(VidyaastraGraphView.class);

   private VidyaastraGraphPanel graphPanel;

   @Override
   protected void initialiseOWLView() throws Exception
   {
      setLayout(new BorderLayout());
      
      graphPanel = new VidyaastraGraphPanel(getOWLModelManager());
      add(graphPanel, BorderLayout.CENTER);
      
      logger.info("VidyaastraGraphView initialized");
   }

   @Override
   protected void disposeOWLView()
   {
      if (graphPanel != null) {
         graphPanel.dispose();
      }
   }

   protected void updateView()
   {
      OWLEntity selectedEntity = getOWLWorkspace().getOWLSelectionModel().getSelectedEntity();
      if (graphPanel != null && selectedEntity != null) {
         graphPanel.setSelectedEntity(selectedEntity);
      }
   }
}

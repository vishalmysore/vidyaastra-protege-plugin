package org.vidyaastra.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.vidyaastra.ui.VidyaastraDialogManager;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.OntologyIRIShortFormProvider;

/**
 * This is the main AI Integration user interface for Vidyaastra Plugin.
 * It contains a query input area and a response display area for LLM interaction.
 */
public class AIQueryPanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   private OWLOntology ontology;
   private OWLEditorKit editorKit;
   private VidyaastraDialogManager dialogHelper;

   private JTextArea queryTextArea;
   private JTextArea responseTextArea;
   private JButton sendQueryButton;
   private JButton clearButton;

   public AIQueryPanel(OWLOntology ontology, OWLEditorKit editorKit, VidyaastraDialogManager dialogHelper)
   {
      this.ontology = ontology;
      this.editorKit = editorKit;
      this.dialogHelper = dialogHelper;

      setLayout(new BorderLayout());

      // Header Panel with Ontology Information
      JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      headerPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
      add(headerPanel, BorderLayout.NORTH);

      JLabel lblTargetOntology = new JLabel("Target Ontology: ");
      lblTargetOntology.setForeground(Color.DARK_GRAY);
      headerPanel.add(lblTargetOntology);

      JLabel lblOntologyID = new JLabel(getTitle(ontology));
      lblOntologyID.setForeground(Color.DARK_GRAY);
      headerPanel.add(lblOntologyID);

      // Main Content Panel
      JPanel mainPanel = new JPanel(new GridBagLayout());
      mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
      add(mainPanel, BorderLayout.CENTER);

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.insets = new Insets(5, 5, 5, 5);

      // Query Input Section
      JLabel lblQuery = new JLabel("Enter Your Query to LLM:");
      lblQuery.setFont(new Font("Arial", Font.BOLD, 12));
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 2;
      gbc.weightx = 1.0;
      gbc.weighty = 0.0;
      mainPanel.add(lblQuery, gbc);

      queryTextArea = new JTextArea(8, 60);
      queryTextArea.setLineWrap(true);
      queryTextArea.setWrapStyleWord(true);
      queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      queryTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
      queryScrollPane.setPreferredSize(new Dimension(700, 200));
      gbc.gridy = 1;
      gbc.weighty = 0.3;
      mainPanel.add(queryScrollPane, gbc);

      // Button Panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      gbc.gridy = 2;
      gbc.weighty = 0.0;
      mainPanel.add(buttonPanel, gbc);

      sendQueryButton = new JButton("Send Query");
      sendQueryButton.setFont(new Font("Arial", Font.BOLD, 12));
      sendQueryButton.addActionListener(e -> sendQuery());
      buttonPanel.add(sendQueryButton);

      clearButton = new JButton("Clear");
      clearButton.addActionListener(e -> clearFields());
      buttonPanel.add(clearButton);

      // Response Display Section
      JLabel lblResponse = new JLabel("LLM Response:");
      lblResponse.setFont(new Font("Arial", Font.BOLD, 12));
      gbc.gridy = 3;
      mainPanel.add(lblResponse, gbc);

      responseTextArea = new JTextArea(12, 60);
      responseTextArea.setLineWrap(true);
      responseTextArea.setWrapStyleWord(true);
      responseTextArea.setEditable(false);
      responseTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      responseTextArea.setBackground(new Color(245, 245, 245));
      responseTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      JScrollPane responseScrollPane = new JScrollPane(responseTextArea);
      responseScrollPane.setPreferredSize(new Dimension(700, 300));
      gbc.gridy = 4;
      gbc.weighty = 0.7;
      mainPanel.add(responseScrollPane, gbc);

      validate();
   }

   private String getTitle(OWLOntology ontology)
   {
      if (ontology.getOntologyID().isAnonymous()) {
         return ontology.getOntologyID().toString();
      }
      final com.google.common.base.Optional<IRI> iri = ontology.getOntologyID().getDefaultDocumentIRI();
      return getOntologyLabelText(iri);
   }

   private String getOntologyLabelText(com.google.common.base.Optional<IRI> iri)
   {
      StringBuilder sb = new StringBuilder();
      if (iri.isPresent()) {
         String shortForm = new OntologyIRIShortFormProvider().getShortForm(iri.get());
         sb.append(shortForm);
      } else {
         sb.append("Anonymous ontology");
      }
      sb.append(" (");
      if (iri.isPresent()) {
         sb.append(iri.get().toString());
      }
      sb.append(")");
      return sb.toString();
   }

   private void sendQuery()
   {
      String query = queryTextArea.getText().trim();
      
      if (query.isEmpty()) {
         dialogHelper.showMessageDialog(this, "Please enter a query before sending.");
         return;
      }

      // Simulate LLM response (placeholder - actual LLM integration would go here)
      responseTextArea.setText("Processing query...\n\n");
      responseTextArea.append("Query received: " + query + "\n\n");
      responseTextArea.append("=== AI Response ===\n");
      responseTextArea.append("This is a placeholder response. In a production environment, this would be replaced\n");
      responseTextArea.append("with actual LLM API integration (e.g., OpenAI, Azure OpenAI, or other LLM services).\n\n");
      responseTextArea.append("Your query about the ontology: " + getTitle(ontology) + "\n");
      responseTextArea.append("has been received and would be processed by the configured LLM service.\n\n");
      responseTextArea.append("To integrate with an actual LLM:\n");
      responseTextArea.append("1. Configure API credentials\n");
      responseTextArea.append("2. Implement HTTP client for LLM API calls\n");
      responseTextArea.append("3. Parse and display the response\n");
      
      dialogHelper.showMessageDialog(this, "Query sent successfully!");
   }

   private void clearFields()
   {
      queryTextArea.setText("");
      responseTextArea.setText("");
   }

   public OWLOntology getActiveOntology()
   {
      return ontology;
   }

   public OWLEditorKit getEditorKit()
   {
      return editorKit;
   }

   public VidyaastraDialogManager getApplicationDialogManager()
   {
      return dialogHelper;
   }

   public static JDialog createDialog(OWLOntology ontology, OWLEditorKit editorKit, VidyaastraDialogManager dialogHelper)
   {
      final JDialog dialog = new JDialog(null, "Vidyaastra - AI Integration", Dialog.ModalityType.MODELESS);
      
      final AIQueryPanel aiQueryPanel = new AIQueryPanel(ontology, editorKit, dialogHelper);
      aiQueryPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
      aiQueryPanel.getActionMap().put("CLOSE_DIALOG", new AbstractAction()
      {
         private static final long serialVersionUID = 1L;
         
         @Override
         public void actionPerformed(ActionEvent e)
         {
            int answer = dialogHelper.showConfirmDialog(dialog, "Confirm Exit", "Exit Vidyaastra AI Integration?");
            switch (answer) {
               case JOptionPane.YES_OPTION:
                  dialog.setVisible(false);
                  break;
            }
         }
      });
      
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter()
      {
         @Override
         public void windowClosing(WindowEvent e) {
            int answer = dialogHelper.showConfirmDialog(dialog, "Confirm Exit", "Exit Vidyaastra AI Integration?");
            switch (answer) {
               case JOptionPane.YES_OPTION:
                  dialog.setVisible(false);
                  break;
            }
         }
      });
      
      dialog.setContentPane(aiQueryPanel);
      dialog.setSize(900, 700);
      dialog.setResizable(true);
      return dialog;
   }
}

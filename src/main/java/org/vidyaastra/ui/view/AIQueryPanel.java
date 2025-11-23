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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.vidyaastra.ui.OntologyGenerator;
import org.vidyaastra.ui.OntologyModifier;
import org.vidyaastra.ui.OntologyOperationType;
import org.vidyaastra.ui.VidyaastraDialogManager;
import org.vidyaastra.ui.VidyaastraPreferences;
import org.vidyaastra.OpenAiCaller;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.OntologyIRIShortFormProvider;

/**
 * This is the main AI Integration user interface for Vidyaastra Plugin.
 * It contains OpenAI configuration fields, a query input area and a response display area.
 */
public class AIQueryPanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   private OWLOntology ontology;
   private OWLEditorKit editorKit;
   private VidyaastraDialogManager dialogHelper;

   // Configuration fields
   private JTextField baseUrlField;
   private JPasswordField apiKeyField;
   private JTextField modelField;
   
   // Operation type selection
   private JRadioButton basicQueryRadio;
   private JRadioButton createOntologyRadio;
   private JRadioButton modifyOntologyRadio;
   private OntologyOperationType currentOperationType = OntologyOperationType.BASIC_QUERY;
   
   // Query/Response fields
   private JLabel queryLabel;
   private JTextArea queryTextArea;
   private JTextArea responseTextArea;
   private JButton sendButton;
   private JButton clearButton;

   public AIQueryPanel(OWLOntology ontology, OWLEditorKit editorKit, VidyaastraDialogManager dialogHelper)
   {
      this.ontology = ontology;
      this.editorKit = editorKit;
      this.dialogHelper = dialogHelper;

      setLayout(new BorderLayout());

      // Main Content Panel with vertical layout
      JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
      mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
      add(mainPanel, BorderLayout.CENTER);

      // Operation Type Selection Panel (top)
      JPanel operationTypePanel = createOperationTypePanel();
      mainPanel.add(operationTypePanel, BorderLayout.NORTH);
      
      // Center panel with config and query/response
      JPanel centerPanel = new JPanel(new BorderLayout(0, 10));

      // OpenAI Configuration Section
      JPanel configPanel = createConfigurationPanel();
      centerPanel.add(configPanel, BorderLayout.NORTH);

      // Query and Response Section (center)
      JPanel queryResponsePanel = createQueryResponsePanel();
      centerPanel.add(queryResponsePanel, BorderLayout.CENTER);
      
      mainPanel.add(centerPanel, BorderLayout.CENTER);

      // Button Panel (bottom)
      JPanel buttonPanel = createButtonPanel();
      add(buttonPanel, BorderLayout.SOUTH);

      // Load saved preferences
      loadPreferences();
   }

   private JPanel createOperationTypePanel()
   {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
      TitledBorder border = BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Operation Mode");
      border.setTitleFont(new Font("Arial", Font.BOLD, 12));
      panel.setBorder(border);
      
      basicQueryRadio = new JRadioButton(OntologyOperationType.BASIC_QUERY.getDisplayName());
      basicQueryRadio.setSelected(true);
      basicQueryRadio.setToolTipText(OntologyOperationType.BASIC_QUERY.getDescription());
      basicQueryRadio.addActionListener(e -> updateOperationType(OntologyOperationType.BASIC_QUERY));
      
      createOntologyRadio = new JRadioButton(OntologyOperationType.CREATE_ONTOLOGY.getDisplayName());
      createOntologyRadio.setToolTipText(OntologyOperationType.CREATE_ONTOLOGY.getDescription());
      createOntologyRadio.addActionListener(e -> updateOperationType(OntologyOperationType.CREATE_ONTOLOGY));
      
      modifyOntologyRadio = new JRadioButton(OntologyOperationType.MODIFY_ONTOLOGY.getDisplayName());
      modifyOntologyRadio.setToolTipText(OntologyOperationType.MODIFY_ONTOLOGY.getDescription());
      modifyOntologyRadio.addActionListener(e -> updateOperationType(OntologyOperationType.MODIFY_ONTOLOGY));
      
      ButtonGroup group = new ButtonGroup();
      group.add(basicQueryRadio);
      group.add(createOntologyRadio);
      group.add(modifyOntologyRadio);
      
      panel.add(basicQueryRadio);
      panel.add(createOntologyRadio);
      panel.add(modifyOntologyRadio);
      
      return panel;
   }
   
   private void updateOperationType(OntologyOperationType type)
   {
      currentOperationType = type;
      
      // Update UI labels based on operation type
      switch (type) {
         case BASIC_QUERY:
            queryLabel.setText("Your Query:");
            sendButton.setText("Send Query");
            break;
         case CREATE_ONTOLOGY:
            queryLabel.setText("New Ontology Description:");
            sendButton.setText("Generate Ontology");
            break;
         case MODIFY_ONTOLOGY:
            queryLabel.setText("Modification Request:");
            sendButton.setText("Apply Modifications");
            break;
      }
   }

   private JPanel createConfigurationPanel()
   {
      JPanel panel = new JPanel(new GridBagLayout());
      TitledBorder border = BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "OpenAI Configuration");
      border.setTitleFont(new Font("Arial", Font.BOLD, 12));
      panel.setBorder(border);

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      gbc.insets = new Insets(5, 10, 5, 5);
      gbc.fill = GridBagConstraints.HORIZONTAL;

      // Base URL
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Base URL:"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1.0;
      baseUrlField = new JTextField(30);
      panel.add(baseUrlField, gbc);

      // API Key
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.weightx = 0.0;
      panel.add(new JLabel("API Key:"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1.0;
      apiKeyField = new JPasswordField(30);
      panel.add(apiKeyField, gbc);

      // Model
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Model:"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1.0;
      modelField = new JTextField(30);
      panel.add(modelField, gbc);

      // Help text
      gbc.gridx = 0;
      gbc.gridy = 3;
      gbc.gridwidth = 2;
      gbc.insets = new Insets(5, 10, 5, 10);
      JLabel helpLabel = new JLabel("<html><i><font size='-2'>Examples: " +
         "https://api.openai.com/v1 | Model: gpt-4o-mini, gpt-4o | " +
         "Demo: http://yourdemoserver (API Key: demokey)</font></i></html>");
      helpLabel.setForeground(Color.DARK_GRAY);
      panel.add(helpLabel, gbc);

      return panel;
   }

   private JPanel createQueryResponsePanel()
   {
      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.insets = new Insets(5, 5, 5, 5);

      // Ontology Info
      JLabel lblOntology = new JLabel("Ontology: " + getTitle(ontology));
      lblOntology.setFont(new Font("Arial", Font.BOLD, 11));
      lblOntology.setForeground(Color.DARK_GRAY);
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 2;
      gbc.weightx = 1.0;
      gbc.weighty = 0.0;
      panel.add(lblOntology, gbc);

      // Query Input Section
      queryLabel = new JLabel("Your Query:");
      queryLabel.setFont(new Font("Arial", Font.BOLD, 12));
      gbc.gridy = 1;
      panel.add(queryLabel, gbc);

      queryTextArea = new JTextArea(6, 60);
      queryTextArea.setLineWrap(true);
      queryTextArea.setWrapStyleWord(true);
      queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      queryTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
      queryScrollPane.setPreferredSize(new Dimension(700, 150));
      gbc.gridy = 2;
      gbc.weighty = 0.3;
      panel.add(queryScrollPane, gbc);

      // Response Display Section
      JLabel lblResponse = new JLabel("AI Response:");
      lblResponse.setFont(new Font("Arial", Font.BOLD, 12));
      gbc.gridy = 3;
      gbc.weighty = 0.0;
      panel.add(lblResponse, gbc);

      responseTextArea = new JTextArea(12, 60);
      responseTextArea.setLineWrap(true);
      responseTextArea.setWrapStyleWord(true);
      responseTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
      responseTextArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      responseTextArea.setEditable(false);
      responseTextArea.setBackground(new Color(245, 245, 245));
      JScrollPane responseScrollPane = new JScrollPane(responseTextArea);
      responseScrollPane.setPreferredSize(new Dimension(700, 300));
      gbc.gridy = 4;
      gbc.weighty = 0.7;
      panel.add(responseScrollPane, gbc);

      return panel;
   }

   private JPanel createButtonPanel()
   {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.setBorder(new EmptyBorder(5, 10, 10, 10));

      sendButton = new JButton("Send Query");
      sendButton.setFont(new Font("Arial", Font.BOLD, 12));
      sendButton.addActionListener(e -> sendQuery());
      panel.add(sendButton);

      clearButton = new JButton("Clear");
      clearButton.addActionListener(e -> clearFields());
      panel.add(clearButton);

      JButton saveConfigButton = new JButton("Save Config");
      saveConfigButton.addActionListener(e -> savePreferences());
      panel.add(saveConfigButton);

      return panel;
   }

   private void loadPreferences()
   {
      baseUrlField.setText(VidyaastraPreferences.getOpenAiBaseUrl());
      apiKeyField.setText(VidyaastraPreferences.getOpenAiApiKey());
      modelField.setText(VidyaastraPreferences.getOpenAiModel());
   }

   private void savePreferences()
   {
      VidyaastraPreferences.setOpenAiBaseUrl(baseUrlField.getText().trim());
      VidyaastraPreferences.setOpenAiApiKey(new String(apiKeyField.getPassword()));
      VidyaastraPreferences.setOpenAiModel(modelField.getText().trim());
      dialogHelper.showMessageDialog(this, "Configuration saved successfully!");
   }

   private void sendQuery()
   {
      String query = queryTextArea.getText().trim();
      String baseUrl = baseUrlField.getText().trim();
      String apiKey = new String(apiKeyField.getPassword()).trim();
      String model = modelField.getText().trim();

      if (query.isEmpty()) {
         dialogHelper.showMessageDialog(this, "Please enter " + 
            (currentOperationType == OntologyOperationType.BASIC_QUERY ? "a query" : 
             currentOperationType == OntologyOperationType.CREATE_ONTOLOGY ? "an ontology description" : 
             "a modification request") + " before sending.");
         return;
      }

      if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
         responseTextArea.setText("Please configure all OpenAI settings above:\n\n");
         responseTextArea.append("✗ Base URL (e.g., https://api.openai.com/v1)\n");
         responseTextArea.append("✗ API Key (from OpenAI or 'demo' for test endpoint)\n");
         responseTextArea.append("✗ Model (e.g., gpt-4o-mini)\n\n");
         responseTextArea.append("Then click 'Save Config' to persist settings.\n");
         dialogHelper.showMessageDialog(this, "Please fill in all configuration fields.");
         return;
      }

      // Show processing message
      responseTextArea.setText("⏳ Processing " + currentOperationType.getDisplayName().toLowerCase() + "...\n\n");
      responseTextArea.append("Contacting " + model + " at " + baseUrl + "\n\n");
      sendButton.setEnabled(false);

      // Execute based on operation type
      switch (currentOperationType) {
         case BASIC_QUERY:
            executeBasicQuery(query, apiKey, model, baseUrl);
            break;
         case CREATE_ONTOLOGY:
            executeCreateOntology(query, apiKey, model, baseUrl);
            break;
         case MODIFY_ONTOLOGY:
            executeModifyOntology(query, apiKey, model, baseUrl);
            break;
      }
   }
   
   private void executeBasicQuery(String query, String apiKey, String model, String baseUrl)
   {
      // Use SwingWorker to call OpenAI in background
      SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
         @Override
         protected String doInBackground() throws Exception {
            // Create OpenAiCaller
            OpenAiCaller caller = new OpenAiCaller(apiKey, model, baseUrl);

            // Build system prompt with ontology context
            String systemPrompt = "You are an expert ontology assistant helping with the ontology: " + 
                                  getTitle(ontology) + ". Provide clear, concise answers about ontology structure, " +
                                  "classes, properties, and relationships.";

            // Call OpenAI
            return caller.generateCompletion(systemPrompt, query);
         }

         @Override
         protected void done() {
            try {
               String response = get();
               responseTextArea.setText("=== AI Response ===\n\n");
               responseTextArea.append(response);
               responseTextArea.append("\n\n===================\n");
            } catch (Exception e) {
               displayError(e);
            } finally {
               sendButton.setEnabled(true);
            }
         }
      };

      worker.execute();
   }
   
   private void executeCreateOntology(String description, String apiKey, String model, String baseUrl)
   {
      SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
         @Override
         protected String doInBackground() throws Exception {
            OntologyGenerator generator = new OntologyGenerator(editorKit, dialogHelper);
            return generator.generateOntologyContent(description, apiKey, model, baseUrl);
         }

         @Override
         protected void done() {
            try {
               String owlContent = get();
               responseTextArea.setText("=== Generated Ontology ===\n\n");
               
               // Show a preview (first 1000 chars)
               String preview = owlContent.length() > 1000 ? 
                  owlContent.substring(0, 1000) + "\n\n... [truncated] ..." : owlContent;
               responseTextArea.append(preview);
               responseTextArea.append("\n\n===================\n\n");
               
               // Ask user if they want to save
               int result = dialogHelper.showConfirmDialog(AIQueryPanel.this, 
                  "Save Ontology", 
                  "Ontology generated successfully! Do you want to save it to a file?");
               
               System.out.println("User save dialog result: " + result + " (0=YES, 1=NO)");
               
               if (result == 0) { // YES
                  System.out.println("User chose to save. Creating OntologyGenerator...");
                  try {
                     OntologyGenerator generator = new OntologyGenerator(editorKit, dialogHelper);
                     System.out.println("OntologyGenerator created successfully");
                     
                     // Generate a meaningful filename from the description
                     String suggestedFileName = AIQueryPanel.this.generateFileName(description);
                     
                     // saveOntology will handle file chooser and ask about loading into Protege
                     java.io.File savedFile = generator.saveOntology(owlContent, suggestedFileName, AIQueryPanel.this);
                     
                     if (savedFile != null) {
                        responseTextArea.append("\n✓ Ontology saved to: " + savedFile.getAbsolutePath() + "\n");
                     } else {
                        responseTextArea.append("\n⚠ Save cancelled by user.\n");
                     }
                  } catch (Exception saveEx) {
                     responseTextArea.append("\n❌ Error saving ontology: " + saveEx.getMessage() + "\n");
                     saveEx.printStackTrace();
                     dialogHelper.showErrorMessageDialog(AIQueryPanel.this, 
                        "Error saving ontology: " + saveEx.getMessage());
                  }
               }
            } catch (Exception e) {
               displayError(e);
            } finally {
               sendButton.setEnabled(true);
            }
         }
      };

      worker.execute();
   }
   
   private void executeModifyOntology(String modificationRequest, String apiKey, String model, String baseUrl)
   {
      if (ontology == null) {
         responseTextArea.setText("❌ Error: No ontology loaded.\n\n");
         responseTextArea.append("Please load an ontology in Protege before attempting modifications.\n");
         sendButton.setEnabled(true);
         return;
      }
      
      SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
         @Override
         protected String doInBackground() throws Exception {
            OntologyModifier modifier = new OntologyModifier(editorKit, dialogHelper);
            return modifier.modifyOntology(ontology, modificationRequest, apiKey, model, baseUrl);
         }

         @Override
         protected void done() {
            try {
               String modificationSummary = get();
               responseTextArea.setText("=== Modification Summary ===\n\n");
               responseTextArea.append(modificationSummary);
               responseTextArea.append("\n\n===================\n");
               responseTextArea.append("\n✓ Modifications have been applied to the active ontology.\n");
               responseTextArea.append("   Remember to save your ontology to persist the changes.\n");
            } catch (Exception e) {
               displayError(e);
            } finally {
               sendButton.setEnabled(true);
            }
         }
      };

      worker.execute();
   }
   
   private void displayError(Exception e)
   {
      responseTextArea.setText("❌ Error:\n\n");
      responseTextArea.append(e.getMessage() + "\n\n");
      if (e.getCause() != null) {
         responseTextArea.append("Cause: " + e.getCause().getMessage() + "\n\n");
      }
      responseTextArea.append("Please check:\n");
      responseTextArea.append("✗ Your API key is correct\n");
      responseTextArea.append("✗ Your base URL is correct\n");
      responseTextArea.append("✗ You have internet connectivity\n");
      responseTextArea.append("✗ Your API key has sufficient credits\n");
      dialogHelper.showErrorMessageDialog(AIQueryPanel.this, "Error: " + e.getMessage());
   }

   private void clearFields()
   {
      queryTextArea.setText("");
      responseTextArea.setText("");
   }
   
   /**
    * Generates a filename from the user's description.
    * Takes the first few meaningful words and creates a valid filename.
    */
   private String generateFileName(String description)
   {
      if (description == null || description.trim().isEmpty()) {
         return "new-ontology.owl";
      }
      
      // Take first 50 chars to avoid too long filenames
      String shortened = description.trim().substring(0, Math.min(50, description.trim().length()));
      
      // Extract meaningful words (remove common words like "create", "ontology", etc.)
      String cleaned = shortened.toLowerCase()
         .replaceAll("\\b(create|new|ontology|for|about|on|the|a|an)\\b", "")
         .trim();
      
      // Replace spaces and special chars with hyphens
      cleaned = cleaned.replaceAll("[^a-z0-9]+", "-");
      
      // Remove leading/trailing hyphens
      cleaned = cleaned.replaceAll("^-+|-+$", "");
      
      // If nothing meaningful left, use generic name
      if (cleaned.isEmpty() || cleaned.length() < 3) {
         return "new-ontology.owl";
      }
      
      // Limit to reasonable length and add extension
      if (cleaned.length() > 30) {
         cleaned = cleaned.substring(0, 30).replaceAll("-+$", "");
      }
      
      return cleaned + "-ontology.owl";
   }

   private String getTitle(OWLOntology ontology)
   {
      if (ontology == null) {
         return "No Ontology Loaded";
      }

      OntologyIRIShortFormProvider sfp = new OntologyIRIShortFormProvider();
      IRI ontologyIRI = ontology.getOntologyID().getOntologyIRI().orNull();

      if (ontologyIRI != null) {
         return sfp.getShortForm(ontologyIRI);
      }
      return "Anonymous Ontology";
   }

   public OWLOntology getActiveOntology()
   {
      return ontology;
   }

   public OWLEditorKit getEditorKit()
   {
      return editorKit;
   }

   /**
    * Creates and shows the dialog for AI Integration.
    */
   public static void showDialog(OWLOntology ontology, OWLEditorKit editorKit)
   {
      VidyaastraDialogManager dialogHelper = new VidyaastraDialogManager();
      AIQueryPanel panel = new AIQueryPanel(ontology, editorKit, dialogHelper);

      JDialog dialog = new JDialog((Dialog) null, "VidyaAstra - AI Integration", true);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setContentPane(panel);
      dialog.setPreferredSize(new Dimension(800, 700));
      dialog.pack();
      dialog.setLocationRelativeTo(null);

      // ESC to close
      panel.registerKeyboardAction(
         e -> dialog.dispose(),
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
      );

      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            dialog.dispose();
         }
      });

      dialog.setVisible(true);
   }
}

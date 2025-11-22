package org.vidyaastra.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.protege.editor.core.ui.preferences.PreferencesLayoutPanel;
import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;

/**
 * Preferences panel for VidyaAstra plugin settings, including OpenAI configuration.
 */
public class VidyaastraPreferencesPanel extends OWLPreferencesPanel {
    
    private static final long serialVersionUID = 1L;
    
    private JTextField baseUrlField;
    private JPasswordField apiKeyField;
    private JTextField modelField;
    
    @Override
    public void initialise() throws Exception {
        setLayout(new BorderLayout());
        
        PreferencesLayoutPanel panel = new PreferencesLayoutPanel();
        add(panel, BorderLayout.NORTH);
        
        // OpenAI Settings Section
        JPanel openAiPanel = new JPanel(new GridBagLayout());
        openAiPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "OpenAI Configuration"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Base URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        openAiPanel.add(new JLabel("Base URL:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        baseUrlField = new JTextField(40);
        baseUrlField.setText(VidyaastraPreferences.getOpenAiBaseUrl());
        openAiPanel.add(baseUrlField, gbc);
        
        // API Key
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        openAiPanel.add(new JLabel("API Key:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        apiKeyField = new JPasswordField(40);
        apiKeyField.setText(VidyaastraPreferences.getOpenAiApiKey());
        openAiPanel.add(apiKeyField, gbc);
        
        // Model
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        openAiPanel.add(new JLabel("Model:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        modelField = new JTextField(40);
        modelField.setText(VidyaastraPreferences.getOpenAiModel());
        openAiPanel.add(modelField, gbc);
        
        // Help text
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 10, 5, 10);
        JLabel helpLabel = new JLabel("<html><i>Configure your OpenAI API credentials to enable AI-powered features.<br>" +
                "For OpenAI: Use https://api.openai.com/v1<br>" +
                "For Azure OpenAI: Use your Azure endpoint URL<br>" +
                "Common models: gpt-4o-mini, gpt-4o, gpt-4-turbo, gpt-3.5-turbo</i></html>");
        openAiPanel.add(helpLabel, gbc);
        
        panel.addGroup("AI Integration");
        panel.addGroupComponent(openAiPanel);
    }
    
    @Override
    public void dispose() throws Exception {
        // Cleanup if needed
    }
    
    @Override
    public void applyChanges() {
        // Save the preferences
        VidyaastraPreferences.setOpenAiBaseUrl(baseUrlField.getText().trim());
        VidyaastraPreferences.setOpenAiApiKey(new String(apiKeyField.getPassword()));
        VidyaastraPreferences.setOpenAiModel(modelField.getText().trim());
    }
}

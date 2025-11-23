package org.vidyaastra.ui;

import java.io.File;
import java.io.FileOutputStream;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vidyaastra.OpenAiCaller;

/**
 * Handles the generation of new OWL ontologies from natural language descriptions
 * using LLM assistance.
 */
public class OntologyGenerator {
    
    private final OWLEditorKit editorKit;
    private final VidyaastraDialogManager dialogManager;
    
    public OntologyGenerator(OWLEditorKit editorKit, VidyaastraDialogManager dialogManager) {
        this.editorKit = editorKit;
        this.dialogManager = dialogManager;
    }
    
    /**
     * Generates a new ontology based on the user's description.
     * 
     * @param description Natural language description of the desired ontology
     * @param apiKey OpenAI API key
     * @param model Model to use (e.g., gpt-4o-mini)
     * @param baseUrl Base URL for the API
     * @return The generated OWL content as a string
     * @throws Exception if generation fails
     */
    public String generateOntologyContent(String description, String apiKey, String model, String baseUrl) 
            throws Exception {
        
        System.out.println("=== Starting Ontology Generation ===");
        System.out.println("Model: " + model);
        System.out.println("Description length: " + description.length() + " chars");
        
        OpenAiCaller caller = new OpenAiCaller(apiKey, model, baseUrl);
        
        String systemPrompt = buildSystemPromptForCreation();
        String userPrompt = buildUserPromptForCreation(description);
        
        System.out.println("Calling LLM...");
        
        // Get the full response from LLM
        String llmResponse = caller.generateCompletion(systemPrompt, userPrompt);
        
        System.out.println("LLM response received, length: " + llmResponse.length() + " chars");
        
        // Extract OWL/RDF content from the response
        String owlContent = extractOwlContent(llmResponse);
        
        System.out.println("OWL content extracted successfully, length: " + owlContent.length() + " chars");
        System.out.println("======================================");
        
        return owlContent;
    }
    
    /**
     * Saves the generated OWL content to a file and optionally loads it into Protege.
     * 
     * @param owlContent The OWL/RDF XML content
     * @param suggestedFileName Suggested filename for the ontology
     * @param parentComponent Parent component for dialogs
     * @return The file where the ontology was saved, or null if user cancelled
     * @throws Exception if saving or loading fails
     */
    public File saveOntology(String owlContent, String suggestedFileName, java.awt.Component parentComponent) 
            throws Exception {
        
        System.out.println("=== Saving Ontology ===");
        System.out.println("Suggested filename: " + suggestedFileName);
        
        // Try to validate the OWL content
        boolean isValid = false;
        try {
            validateOwlContent(owlContent);
            isValid = true;
        } catch (OWLOntologyCreationException e) {
            System.err.println("⚠ Validation warning: " + e.getMessage());
            
            // Show error with option to save anyway
            String errorMsg = "The generated OWL content has validation errors:\n\n" +
                e.getMessage() + "\n\n" +
                "Do you want to save it anyway? You can manually fix the file later.";
            
            int choice = dialogManager.showConfirmDialog(parentComponent, "Validation Error", errorMsg);
            if (choice != 0) { // 0 = YES
                System.out.println("User chose not to save invalid content");
                return null;
            }
            System.out.println("User chose to save despite validation errors");
        }
        
        File saveFile = null;
        
        try {
            // Ask user where to save using Protege's file chooser
            saveFile = dialogManager.showSaveFileChooser(
                parentComponent, 
                "Save New Ontology", 
                "owl", 
                "OWL Ontology Files", 
                true
            );
            
            System.out.println("File chooser returned: " + (saveFile != null ? saveFile.getAbsolutePath() : "null (cancelled)"));
            
        } catch (Exception e) {
            System.err.println("Error with Protege file chooser: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to standard Java file chooser
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Save New Ontology");
            fileChooser.setSelectedFile(new File(suggestedFileName));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("OWL Ontology Files (*.owl)", "owl"));
            
            int result = fileChooser.showSaveDialog(null);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                saveFile = fileChooser.getSelectedFile();
                System.out.println("Standard file chooser returned: " + saveFile.getAbsolutePath());
            } else {
                System.out.println("Standard file chooser cancelled");
            }
        }
        
        if (saveFile == null) {
            System.out.println("User cancelled save operation");
            return null; // User cancelled
        }
        
        // Ensure .owl extension
        if (!saveFile.getName().toLowerCase().endsWith(".owl")) {
            saveFile = new File(saveFile.getAbsolutePath() + ".owl");
            System.out.println("Added .owl extension: " + saveFile.getAbsolutePath());
        }
        
        System.out.println("Saving to file: " + saveFile.getAbsolutePath());
        
        // Write to file
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            fos.write(owlContent.getBytes("UTF-8"));
            fos.flush();
        }
        
        System.out.println("✓ File saved successfully (" + owlContent.length() + " bytes)");
        
        // Ask if user wants to load into Protege (only if validation passed and we have an editor)
        if (isValid && editorKit != null) {
            int loadChoice = dialogManager.showConfirmDialog(parentComponent,
                "Load into Protege",
                "Ontology saved successfully!\n\nDo you want to load it into Protege now?");
            
            if (loadChoice == 0) { // YES
                System.out.println("Loading ontology into Protege...");
                try {
                    loadOntologyIntoProtege(saveFile);
                    System.out.println("✓ Ontology loaded into Protege");
                } catch (Exception e) {
                    System.err.println("⚠ Could not load into Protege: " + e.getMessage());
                    dialogManager.showErrorMessageDialog(parentComponent, 
                        "File saved but could not be loaded into Protege: " + e.getMessage());
                }
            } else {
                System.out.println("User chose not to load into Protege");
            }
        } else if (!isValid) {
            System.out.println("Skipping Protege load due to validation errors");
        }
        
        System.out.println("=======================");
        
        return saveFile;
    }
    
    /**
     * Validates that the content is valid OWL/RDF XML.
     */
    private void validateOwlContent(String owlContent) throws OWLOntologyCreationException {
        System.out.println("=== Validating OWL Content ===");
        
        if (owlContent == null || owlContent.trim().isEmpty()) {
            throw new OWLOntologyCreationException("OWL content is empty or null");
        }
        
        if (!owlContent.trim().startsWith("<?xml")) {
            throw new OWLOntologyCreationException(
                "OWL content does not start with XML declaration. First 100 chars: " + 
                owlContent.substring(0, Math.min(100, owlContent.length()))
            );
        }
        
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        StringDocumentSource source = new StringDocumentSource(owlContent);
        
        try {
            // This will throw an exception if the content is not valid
            OWLOntology testOntology = manager.loadOntologyFromOntologyDocument(source);
            
            System.out.println("✓ OWL content is valid");
            System.out.println("  Classes: " + testOntology.getClassesInSignature().size());
            System.out.println("  Object Properties: " + testOntology.getObjectPropertiesInSignature().size());
            System.out.println("  Data Properties: " + testOntology.getDataPropertiesInSignature().size());
            System.out.println("  Axioms: " + testOntology.getAxiomCount());
            
            // Clean up
            manager.removeOntology(testOntology);
            
        } catch (OWLOntologyCreationException e) {
            System.err.println("✗ OWL validation failed: " + e.getMessage());
            
            // Extract line/column info if available
            String detailedError = extractDetailedError(e, owlContent);
            
            // Re-throw with more context
            throw new OWLOntologyCreationException(
                "Generated OWL content is not valid:\n\n" + detailedError,
                e
            );
        }
        
        System.out.println("==============================");
    }
    
    /**
     * Loads an ontology file into Protege.
     */
    private void loadOntologyIntoProtege(File owlFile) throws Exception {
        OWLModelManager modelManager = editorKit.getOWLModelManager();
        
        // Load the ontology using the OWL ontology manager
        OWLOntologyManager manager = modelManager.getOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
        
        // Set it as the active ontology
        modelManager.setActiveOntology(ontology);
    }
    
    /**
     * Builds the system prompt for ontology creation.
     */
    private String buildSystemPromptForCreation() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert ontology engineer specializing in OWL (Web Ontology Language). ");
        sb.append("Your task is to create valid OWL ontologies in RDF/XML format based on user descriptions.\n\n");
        sb.append("IMPORTANT GUIDELINES:\n");
        sb.append("1. Generate ONLY valid OWL/RDF XML content\n");
        sb.append("2. Use proper OWL namespace declarations (owl, rdf, rdfs, xml)\n");
        sb.append("3. Include appropriate classes, properties, and individuals as described\n");
        sb.append("4. Add rdfs:label and rdfs:comment annotations for clarity\n");
        sb.append("5. Use meaningful IRIs for all entities\n");
        sb.append("6. Ensure the ontology is well-formed and can be parsed by OWL API\n");
        sb.append("7. Start with <?xml version=\"1.0\"?> declaration\n");
        sb.append("8. Do NOT include any explanatory text outside the XML - ONLY the XML content\n");
        sb.append("9. Do NOT wrap the XML in markdown code blocks or any other formatting\n");
        sb.append("10. The response should be ready to save directly as an .owl file\n\n");
        sb.append("Example structure:\n");
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<rdf:RDF xmlns=\"http://example.org/ontology#\"\n");
        sb.append("     xml:base=\"http://example.org/ontology\"\n");
        sb.append("     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        sb.append("     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n");
        sb.append("     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n");
        sb.append("     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n");
        sb.append("     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n");
        sb.append("    <owl:Ontology rdf:about=\"http://example.org/ontology\"/>\n");
        sb.append("    <!-- Classes, properties, and individuals go here -->\n");
        sb.append("</rdf:RDF>");
        
        return sb.toString();
    }
    
    /**
     * Builds the user prompt for ontology creation.
     */
    private String buildUserPromptForCreation(String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a complete OWL ontology based on the following description:\n\n");
        sb.append(description);
        sb.append("\n\nGenerate ONLY the OWL/RDF XML content. Do not include any explanations, ");
        sb.append("code blocks, or markdown formatting. The response should start with <?xml and be ");
        sb.append("valid OWL/RDF XML that can be directly saved as a .owl file.");
        
        return sb.toString();
    }
    
    /**
     * Extracts OWL content from LLM response, handling cases where it might be
     * wrapped in markdown code blocks or includes explanatory text.
     */
    private String extractOwlContent(String llmResponse) throws Exception {
        String content = llmResponse.trim();
        
        System.out.println("=== Raw LLM Response (first 500 chars) ===");
        System.out.println(content.substring(0, Math.min(500, content.length())));
        System.out.println("===========================================");
        
        // Step 1: Remove markdown code blocks if present
        if (content.contains("```xml")) {
            int start = content.indexOf("```xml") + 6;
            int end = content.lastIndexOf("```");
            if (end > start) {
                content = content.substring(start, end).trim();
                System.out.println("Extracted from ```xml code block");
            }
        } else if (content.contains("```rdf")) {
            int start = content.indexOf("```rdf") + 6;
            int end = content.lastIndexOf("```");
            if (end > start) {
                content = content.substring(start, end).trim();
                System.out.println("Extracted from ```rdf code block");
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.lastIndexOf("```");
            if (end > start) {
                content = content.substring(start, end).trim();
                System.out.println("Extracted from generic ``` code block");
            }
        }
        
        // Step 2: Find the actual XML declaration
        int xmlStart = content.indexOf("<?xml");
        if (xmlStart < 0) {
            throw new Exception("No XML declaration found in LLM response. " +
                "The response must start with <?xml version=\"1.0\"?>. " +
                "Please ensure the LLM is generating valid OWL/RDF XML content.");
        }
        
        if (xmlStart > 0) {
            System.out.println("Found XML declaration at position " + xmlStart + ", removing " + xmlStart + " leading characters");
            content = content.substring(xmlStart);
        }
        
        // Step 3: Remove any characters before <?xml (common issue)
        // Sometimes there are invisible characters or BOM
        content = content.replaceAll("^[^<]*<", "<");
        
        // Step 4: Ensure it ends with the closing RDF tag
        int rdfEnd = content.lastIndexOf("</rdf:RDF>");
        if (rdfEnd < 0) {
            throw new Exception("No closing </rdf:RDF> tag found in LLM response. " +
                "The response must be a complete OWL/RDF XML document.");
        }
        
        // Remove anything after the closing tag
        if (rdfEnd > 0) {
            content = content.substring(0, rdfEnd + 10);
            System.out.println("Trimmed content after </rdf:RDF> tag");
        }
        
        content = content.trim();
        
        // Step 5: Validate it starts correctly
        if (!content.startsWith("<?xml")) {
            throw new Exception("Extracted content does not start with <?xml declaration. " +
                "First 100 chars: " + content.substring(0, Math.min(100, content.length())));
        }
        
        // Step 6: Fix common XML issues from LLM responses
        content = fixCommonXmlIssues(content);
        
        System.out.println("=== Extracted OWL Content (first 300 chars) ===");
        System.out.println(content.substring(0, Math.min(300, content.length())));
        System.out.println("=== Last 100 chars ===");
        System.out.println(content.substring(Math.max(0, content.length() - 100)));
        System.out.println("===============================================");
        
        return content;
    }
    
    /**
     * Fixes common XML issues that LLMs sometimes generate.
     */
    private String fixCommonXmlIssues(String content) {
        System.out.println("=== Fixing Common XML Issues ===");
        
        // Issue 1: Unescaped ampersands in URLs
        // Replace & with &amp; in attribute values only (not in entity references like &lt;)
        content = content.replaceAll("&(?!(amp|lt|gt|quot|apos);)", "&amp;");
        System.out.println("✓ Fixed unescaped ampersands");
        
        // Issue 2: Spaces in namespace declarations or IRIs
        // Remove spaces around = in XML attributes
        content = content.replaceAll("\\s*=\\s*", "=");
        System.out.println("✓ Normalized attribute spacing");
        
        // Issue 3: Fix malformed rdf:resource or rdf:about attributes
        // Ensure URLs in rdf:resource and rdf:about are properly quoted
        content = content.replaceAll("rdf:resource\\s*=\\s*([^\"'][^\\s>]+)", "rdf:resource=\"$1\"");
        content = content.replaceAll("rdf:about\\s*=\\s*([^\"'][^\\s>]+)", "rdf:about=\"$1\"");
        System.out.println("✓ Fixed unquoted RDF attributes");
        
        System.out.println("================================");
        return content;
    }
    
    /**
     * Extracts detailed error information including the problematic line from the content.
     */
    private String extractDetailedError(OWLOntologyCreationException e, String owlContent) {
        String errorMsg = e.getMessage();
        StringBuilder details = new StringBuilder();
        details.append(errorMsg).append("\n\n");
        
        // Try to extract line number from error message
        java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile("lineNumber:\\s*(\\d+)");
        java.util.regex.Matcher lineMatcher = linePattern.matcher(errorMsg);
        
        if (lineMatcher.find()) {
            try {
                int lineNum = Integer.parseInt(lineMatcher.group(1));
                String[] lines = owlContent.split("\n");
                
                details.append("Problematic content around line ").append(lineNum).append(":\n");
                details.append("---\n");
                
                int start = Math.max(0, lineNum - 3);
                int end = Math.min(lines.length, lineNum + 2);
                
                for (int i = start; i < end; i++) {
                    String marker = (i == lineNum - 1) ? ">>> " : "    ";
                    details.append(String.format("%s%3d: %s\n", marker, i + 1, lines[i]));
                }
                details.append("---\n");
            } catch (Exception ex) {
                // Ignore parsing errors
            }
        }
        
        details.append("\nFirst 500 chars of content:\n");
        details.append(owlContent.substring(0, Math.min(500, owlContent.length())));
        
        return details.toString();
    }
}

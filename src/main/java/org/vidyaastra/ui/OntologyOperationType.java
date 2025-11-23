package org.vidyaastra.ui;

/**
 * Enum representing different types of operations that can be performed
 * through the AI integration interface.
 */
public enum OntologyOperationType {
    /**
     * Basic query mode - ask questions about ontology, get AI responses
     */
    BASIC_QUERY("Basic Query", "Ask questions about your ontology"),
    
    /**
     * Create new ontology mode - describe a new ontology and generate OWL file
     */
    CREATE_ONTOLOGY("Create New Ontology", "Describe a new ontology to generate"),
    
    /**
     * Modify ontology mode - describe modifications to apply to current ontology
     */
    MODIFY_ONTOLOGY("Modify Ontology", "Describe changes to apply to the ontology");
    
    private final String displayName;
    private final String description;
    
    OntologyOperationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

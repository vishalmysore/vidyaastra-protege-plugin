package org.vidyaastra.ui;

import java.util.Set;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vidyaastra.OpenAiCaller;

/**
 * Handles modifications to existing OWL ontologies using LLM guidance.
 */
public class OntologyModifier {
    
    private final OWLEditorKit editorKit;
    private final VidyaastraDialogManager dialogManager;
    
    public OntologyModifier(OWLEditorKit editorKit, VidyaastraDialogManager dialogManager) {
        this.editorKit = editorKit;
        this.dialogManager = dialogManager;
    }
    
    /**
     * Modifies an ontology based on natural language instructions.
     * 
     * @param ontology The ontology to modify
     * @param modificationRequest Natural language description of changes to make
     * @param apiKey OpenAI API key
     * @param model Model to use
     * @param baseUrl Base URL for the API
     * @return A summary of the modifications made
     * @throws Exception if modification fails
     */
    public String modifyOntology(OWLOntology ontology, String modificationRequest, 
                                 String apiKey, String model, String baseUrl) throws Exception {
        
        // Get ontology context
        String ontologyContext = buildOntologyContext(ontology);
        
        // Call LLM to get modification instructions
        OpenAiCaller caller = new OpenAiCaller(apiKey, model, baseUrl);
        String systemPrompt = buildSystemPromptForModification();
        String userPrompt = buildUserPromptForModification(ontologyContext, modificationRequest);
        
        String llmResponse = caller.generateCompletion(systemPrompt, userPrompt);
        
        // Parse and apply modifications
        String modificationSummary = applyModifications(ontology, llmResponse);
        
        return modificationSummary;
    }
    
    /**
     * Builds a textual representation of the ontology for context.
     */
    private String buildOntologyContext(OWLOntology ontology) {
        StringBuilder context = new StringBuilder();
        
        // Ontology IRI
        context.append("Ontology IRI: ");
        if (ontology.getOntologyID().getOntologyIRI().isPresent()) {
            context.append(ontology.getOntologyID().getOntologyIRI().get().toString());
        } else {
            context.append("Anonymous");
        }
        context.append("\n\n");
        
        // Classes
        Set<OWLClass> classes = ontology.getClassesInSignature();
        context.append("Classes (").append(classes.size()).append("):\n");
        for (OWLClass cls : classes) {
            if (!cls.isOWLThing() && !cls.isOWLNothing()) {
                context.append("  - ").append(getShortForm(cls)).append("\n");
            }
        }
        context.append("\n");
        
        // Object Properties
        Set<OWLObjectProperty> objectProps = ontology.getObjectPropertiesInSignature();
        context.append("Object Properties (").append(objectProps.size()).append("):\n");
        for (OWLObjectProperty prop : objectProps) {
            context.append("  - ").append(getShortForm(prop)).append("\n");
        }
        context.append("\n");
        
        // Data Properties
        Set<OWLDataProperty> dataProps = ontology.getDataPropertiesInSignature();
        context.append("Data Properties (").append(dataProps.size()).append("):\n");
        for (OWLDataProperty prop : dataProps) {
            context.append("  - ").append(getShortForm(prop)).append("\n");
        }
        context.append("\n");
        
        // Axiom count
        context.append("Total Axioms: ").append(ontology.getAxiomCount()).append("\n");
        
        return context.toString();
    }
    
    /**
     * Gets a short, readable form of an entity's IRI.
     */
    private String getShortForm(OWLEntity entity) {
        IRI iri = entity.getIRI();
        String fragment = iri.getFragment();
        if (fragment != null && !fragment.isEmpty()) {
            return fragment;
        }
        String remainder = iri.getRemainder().isPresent() ? iri.getRemainder().get() : iri.toString();
        int lastSlash = remainder.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < remainder.length() - 1) {
            return remainder.substring(lastSlash + 1);
        }
        return remainder;
    }
    
    /**
     * Builds the system prompt for modification.
     */
    private String buildSystemPromptForModification() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert ontology engineer helping to modify OWL ontologies.\n\n");
        sb.append("Given the current structure of an ontology and a modification request, ");
        sb.append("provide clear, step-by-step instructions for the modifications.\n\n");
        sb.append("Format your response as a structured list of operations:\n\n");
        sb.append("ADD_CLASS: <ClassName> - <Description>\n");
        sb.append("ADD_OBJECT_PROPERTY: <PropertyName> - <Description>\n");
        sb.append("ADD_DATA_PROPERTY: <PropertyName> - <Description>\n");
        sb.append("ADD_SUBCLASS: <SubClass> subClassOf <SuperClass>\n");
        sb.append("ADD_DOMAIN: <Property> domain <Class>\n");
        sb.append("ADD_RANGE: <Property> range <ClassOrDatatype>\n");
        sb.append("REMOVE_CLASS: <ClassName>\n");
        sb.append("REMOVE_PROPERTY: <PropertyName>\n");
        sb.append("ADD_ANNOTATION: <Entity> - <AnnotationType> - <Value>\n\n");
        sb.append("After the operations list, add a SUMMARY section explaining the changes.\n");
        sb.append("Be specific and use the exact names that should be used in the ontology.");
        
        return sb.toString();
    }
    
    /**
     * Builds the user prompt for modification.
     */
    private String buildUserPromptForModification(String ontologyContext, String modificationRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Ontology Structure:\n");
        sb.append("----------------------------\n");
        sb.append(ontologyContext);
        sb.append("\n\n");
        sb.append("Modification Request:\n");
        sb.append("--------------------\n");
        sb.append(modificationRequest);
        sb.append("\n\n");
        sb.append("Please provide the modification operations needed to fulfill this request.");
        
        return sb.toString();
    }
    
    /**
     * Parses LLM response and applies modifications to the ontology.
     */
    private String applyModifications(OWLOntology ontology, String llmResponse) throws Exception {
        OWLModelManager modelManager = editorKit.getOWLModelManager();
        OWLOntologyManager manager = modelManager.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        
        StringBuilder summary = new StringBuilder();
        summary.append("Modifications Applied:\n\n");
        
        String[] lines = llmResponse.split("\n");
        int modificationsCount = 0;
        
        String ontologyIRIString = getOntologyBaseIRI(ontology);
        
        for (String line : lines) {
            line = line.trim();
            
            try {
                if (line.startsWith("ADD_CLASS:")) {
                    String className = extractEntityName(line);
                    IRI classIRI = IRI.create(ontologyIRIString + "#" + className);
                    OWLClass newClass = factory.getOWLClass(classIRI);
                    OWLAxiom axiom = factory.getOWLDeclarationAxiom(newClass);
                    manager.addAxiom(ontology, axiom);
                    summary.append("✓ Added class: ").append(className).append("\n");
                    modificationsCount++;
                }
                else if (line.startsWith("ADD_OBJECT_PROPERTY:")) {
                    String propName = extractEntityName(line);
                    IRI propIRI = IRI.create(ontologyIRIString + "#" + propName);
                    OWLObjectProperty newProp = factory.getOWLObjectProperty(propIRI);
                    OWLAxiom axiom = factory.getOWLDeclarationAxiom(newProp);
                    manager.addAxiom(ontology, axiom);
                    summary.append("✓ Added object property: ").append(propName).append("\n");
                    modificationsCount++;
                }
                else if (line.startsWith("ADD_DATA_PROPERTY:")) {
                    String propName = extractEntityName(line);
                    IRI propIRI = IRI.create(ontologyIRIString + "#" + propName);
                    OWLDataProperty newProp = factory.getOWLDataProperty(propIRI);
                    OWLAxiom axiom = factory.getOWLDeclarationAxiom(newProp);
                    manager.addAxiom(ontology, axiom);
                    summary.append("✓ Added data property: ").append(propName).append("\n");
                    modificationsCount++;
                }
                else if (line.startsWith("ADD_SUBCLASS:")) {
                    String[] parts = line.substring("ADD_SUBCLASS:".length()).split("subClassOf");
                    if (parts.length == 2) {
                        String subClassName = parts[0].trim();
                        String superClassName = parts[1].trim();
                        
                        OWLClass subClass = factory.getOWLClass(IRI.create(ontologyIRIString + "#" + subClassName));
                        OWLClass superClass = factory.getOWLClass(IRI.create(ontologyIRIString + "#" + superClassName));
                        
                        OWLAxiom axiom = factory.getOWLSubClassOfAxiom(subClass, superClass);
                        manager.addAxiom(ontology, axiom);
                        summary.append("✓ Added subclass relation: ").append(subClassName)
                               .append(" ⊆ ").append(superClassName).append("\n");
                        modificationsCount++;
                    }
                }
                else if (line.startsWith("SUMMARY:")) {
                    // Extract and append the summary from LLM
                    summary.append("\nLLM Summary:\n");
                    summary.append("------------\n");
                    int summaryStart = llmResponse.indexOf("SUMMARY:");
                    if (summaryStart >= 0) {
                        String llmSummary = llmResponse.substring(summaryStart + 8).trim();
                        summary.append(llmSummary);
                    }
                    break;
                }
            } catch (Exception e) {
                summary.append("✗ Error processing: ").append(line).append(" - ").append(e.getMessage()).append("\n");
            }
        }
        
        summary.insert(0, "Total modifications: " + modificationsCount + "\n\n");
        
        return summary.toString();
    }
    
    /**
     * Extracts the entity name from a modification command line.
     */
    private String extractEntityName(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex < 0) {
            return "";
        }
        
        String remainder = line.substring(colonIndex + 1).trim();
        int dashIndex = remainder.indexOf('-');
        
        if (dashIndex >= 0) {
            return remainder.substring(0, dashIndex).trim();
        }
        
        return remainder.trim();
    }
    
    /**
     * Gets the base IRI for the ontology.
     */
    private String getOntologyBaseIRI(OWLOntology ontology) {
        if (ontology.getOntologyID().getOntologyIRI().isPresent()) {
            String iriString = ontology.getOntologyID().getOntologyIRI().get().toString();
            // Remove trailing # or / if present
            if (iriString.endsWith("#") || iriString.endsWith("/")) {
                iriString = iriString.substring(0, iriString.length() - 1);
            }
            return iriString;
        }
        return "http://www.example.org/ontology";
    }
}

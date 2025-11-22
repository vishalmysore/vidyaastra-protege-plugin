package org.vidyaastra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NLP Query Parsing Logic
 */
@DisplayName("NLP Query Parsing Tests")
class NLPQueryParsingTest {

    @Test
    @DisplayName("Should parse QUERY_TYPE: CLASS correctly")
    void testParseQueryTypeClass() {
        String llmResponse = "QUERY_TYPE: CLASS\nTARGET: Person";
        
        String[] lines = llmResponse.split("\n");
        String queryType = null;
        String target = null;
        
        for (String line : lines) {
            if (line.startsWith("QUERY_TYPE:")) {
                queryType = line.substring("QUERY_TYPE:".length()).trim();
            } else if (line.startsWith("TARGET:")) {
                target = line.substring("TARGET:".length()).trim();
            }
        }
        
        assertThat(queryType).isEqualTo("CLASS");
        assertThat(target).isEqualTo("Person");
    }

    @Test
    @DisplayName("Should parse QUERY_TYPE: PROPERTY correctly")
    void testParseQueryTypeProperty() {
        String llmResponse = "QUERY_TYPE: PROPERTY\nTARGET: hasName";
        
        String[] lines = llmResponse.split("\n");
        String queryType = null;
        String target = null;
        
        for (String line : lines) {
            if (line.startsWith("QUERY_TYPE:")) {
                queryType = line.substring("QUERY_TYPE:".length()).trim();
            } else if (line.startsWith("TARGET:")) {
                target = line.substring("TARGET:".length()).trim();
            }
        }
        
        assertThat(queryType).isEqualTo("PROPERTY");
        assertThat(target).isEqualTo("hasName");
    }

    @Test
    @DisplayName("Should parse QUERY_TYPE: INDIVIDUAL correctly")
    void testParseQueryTypeIndividual() {
        String llmResponse = "QUERY_TYPE: INDIVIDUAL\nTARGET: JohnDoe";
        
        String[] lines = llmResponse.split("\n");
        String queryType = null;
        String target = null;
        
        for (String line : lines) {
            if (line.startsWith("QUERY_TYPE:")) {
                queryType = line.substring("QUERY_TYPE:".length()).trim();
            } else if (line.startsWith("TARGET:")) {
                target = line.substring("TARGET:".length()).trim();
            }
        }
        
        assertThat(queryType).isEqualTo("INDIVIDUAL");
        assertThat(target).isEqualTo("JohnDoe");
    }

    @Test
    @DisplayName("Should handle escaped newlines in LLM response")
    void testParseResponseWithEscapedNewlines() {
        String llmResponseWithEscapedNewlines = "QUERY_TYPE: CLASS\\nTARGET: Person";
        
        // Simulating the replace logic
        String normalized = llmResponseWithEscapedNewlines.replace("\\n", "\n");
        
        String[] lines = normalized.split("\n");
        
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).contains("QUERY_TYPE");
        assertThat(lines[1]).contains("TARGET");
    }

    @Test
    @DisplayName("Should handle response with extra whitespace")
    void testParseResponseWithWhitespace() {
        String llmResponse = "  QUERY_TYPE:   CLASS  \n  TARGET:   Person  ";
        
        String[] lines = llmResponse.split("\n");
        String queryType = null;
        String target = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("QUERY_TYPE:")) {
                queryType = line.substring("QUERY_TYPE:".length()).trim();
            } else if (line.startsWith("TARGET:")) {
                target = line.substring("TARGET:".length()).trim();
            }
        }
        
        assertThat(queryType).isEqualTo("CLASS");
        assertThat(target).isEqualTo("Person");
    }

    @Test
    @DisplayName("Should parse multi-line LLM response with explanation")
    void testParseLLMResponseWithExplanation() {
        String llmResponse = "I found the entity you're looking for.\n" +
            "\n" +
            "QUERY_TYPE: CLASS\n" +
            "TARGET: Person\n" +
            "\n" +
            "This represents the Person class in your ontology.\n";
        
        String[] lines = llmResponse.split("\n");
        String queryType = null;
        String target = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("QUERY_TYPE:")) {
                queryType = line.substring("QUERY_TYPE:".length()).trim();
            } else if (line.startsWith("TARGET:")) {
                target = line.substring("TARGET:".length()).trim();
            }
        }
        
        assertThat(queryType).isEqualTo("CLASS");
        assertThat(target).isEqualTo("Person");
    }

    @Test
    @DisplayName("Should handle case-insensitive entity search")
    void testCaseInsensitiveEntitySearch() {
        String targetEntity = "person";
        String ontologyClassName = "Person";
        
        // Case-insensitive comparison
        boolean matches = ontologyClassName.equalsIgnoreCase(targetEntity);
        
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should handle partial entity name matching")
    void testPartialEntityNameMatching() {
        String targetEntity = "Pers";
        String ontologyClassName = "Person";
        
        // Partial match
        boolean matches = ontologyClassName.toLowerCase().contains(targetEntity.toLowerCase());
        
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should extract short form from IRI with hash")
    void testExtractShortFormFromIRIWithHash() {
        String iri = "http://example.org/ontology#Person";
        
        // Extract short form (after # or last /)
        String shortForm;
        if (iri.contains("#")) {
            shortForm = iri.substring(iri.indexOf("#") + 1);
        } else {
            shortForm = iri.substring(iri.lastIndexOf("/") + 1);
        }
        
        assertThat(shortForm).isEqualTo("Person");
    }

    @Test
    @DisplayName("Should extract short form from IRI with slash")
    void testExtractShortFormFromIRIWithSlash() {
        String iri = "http://example.org/ontology/Person";
        
        String shortForm;
        if (iri.contains("#")) {
            shortForm = iri.substring(iri.indexOf("#") + 1);
        } else {
            shortForm = iri.substring(iri.lastIndexOf("/") + 1);
        }
        
        assertThat(shortForm).isEqualTo("Person");
    }
}

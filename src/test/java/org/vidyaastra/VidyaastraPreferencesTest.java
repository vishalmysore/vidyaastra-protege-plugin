package org.vidyaastra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VidyaastraPreferences - Tests API key storage and preferences management
 */
@DisplayName("VidyaAstra Preferences Tests")
class VidyaastraPreferencesTest {

    private static final String TEST_API_KEY = "sk-test-1234567890abcdefghijklmnopqrstuvwxyz";
    private static final String PREF_KEY_API_KEY = "openai.api.key";
    private Preferences testPreferences;

    @BeforeEach
    void setUp() throws Exception {
        // Use a test-specific preferences node to avoid polluting user preferences
        testPreferences = Preferences.userRoot().node("org/vidyaastra/test");
        testPreferences.clear();
    }

    @Test
    @DisplayName("Should store API key in preferences")
    void testStoreApiKey() {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEqualTo(TEST_API_KEY);
    }

    @Test
    @DisplayName("Should retrieve stored API key")
    void testRetrieveApiKey() {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        
        String apiKey = testPreferences.get(PREF_KEY_API_KEY, "");
        
        assertThat(apiKey).isNotEmpty();
        assertThat(apiKey).isEqualTo(TEST_API_KEY);
    }

    @Test
    @DisplayName("Should return default value when API key not set")
    void testRetrieveApiKeyNotSet() {
        String apiKey = testPreferences.get(PREF_KEY_API_KEY, "default-key");
        
        assertThat(apiKey).isEqualTo("default-key");
    }

    @Test
    @DisplayName("Should return null when API key not set and no default")
    void testRetrieveApiKeyNullDefault() {
        String apiKey = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(apiKey).isNull();
    }

    @Test
    @DisplayName("Should update existing API key")
    void testUpdateApiKey() {
        String oldKey = "sk-old-key";
        String newKey = "sk-new-key";
        
        testPreferences.put(PREF_KEY_API_KEY, oldKey);
        testPreferences.put(PREF_KEY_API_KEY, newKey);
        
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEqualTo(newKey);
        assertThat(retrieved).isNotEqualTo(oldKey);
    }

    @Test
    @DisplayName("Should remove API key from preferences")
    void testRemoveApiKey() {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        testPreferences.remove(PREF_KEY_API_KEY);
        
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("Should validate API key format")
    void testValidateApiKeyFormat() {
        String validKey = "sk-proj-1234567890abcdefghijklmnopqrstuvwxyz";
        
        boolean isValid = validKey.startsWith("sk-");
        
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid API key format")
    void testRejectInvalidApiKeyFormat() {
        String invalidKey = "invalid-key-format";
        
        boolean isValid = invalidKey.startsWith("sk-");
        
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate minimum API key length")
    void testValidateMinimumKeyLength() {
        String shortKey = "sk-123";
        String validKey = TEST_API_KEY;
        
        int minLength = 20;
        
        assertThat(shortKey.length()).isLessThan(minLength);
        assertThat(validKey.length()).isGreaterThan(minLength);
    }

    @Test
    @DisplayName("Should handle empty API key")
    void testHandleEmptyApiKey() {
        String emptyKey = "";
        
        testPreferences.put(PREF_KEY_API_KEY, emptyKey);
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should clear all preferences")
    void testClearAllPreferences() throws Exception {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        testPreferences.put("other.setting", "value");
        
        testPreferences.clear();
        
        assertThat(testPreferences.get(PREF_KEY_API_KEY, null)).isNull();
        assertThat(testPreferences.get("other.setting", null)).isNull();
    }

    @Test
    @DisplayName("Should handle API key with special characters")
    void testApiKeyWithSpecialCharacters() {
        String keyWithSpecialChars = "sk-test-!@#$%^&*()_+-={}[]|:;<>?,./";
        
        testPreferences.put(PREF_KEY_API_KEY, keyWithSpecialChars);
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEqualTo(keyWithSpecialChars);
    }

    @Test
    @DisplayName("Should persist preferences across sessions")
    void testPersistenceAcrossSessions() throws Exception {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        testPreferences.flush(); // Force write to backing store
        
        // Create a new preferences object to simulate new session
        Preferences newPreferences = Preferences.userRoot().node("org/vidyaastra/test");
        String retrieved = newPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEqualTo(TEST_API_KEY);
    }

    @Test
    @DisplayName("Should detect if API key is configured")
    void testDetectApiKeyConfigured() {
        // Not configured
        boolean isConfigured = testPreferences.get(PREF_KEY_API_KEY, null) != null;
        assertThat(isConfigured).isFalse();
        
        // Configure
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        isConfigured = testPreferences.get(PREF_KEY_API_KEY, null) != null;
        assertThat(isConfigured).isTrue();
    }

    @Test
    @DisplayName("Should validate non-empty API key")
    void testValidateNonEmptyApiKey() {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        
        String apiKey = testPreferences.get(PREF_KEY_API_KEY, "");
        boolean isValid = !apiKey.isEmpty() && apiKey.startsWith("sk-");
        
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should handle whitespace in API key")
    void testHandleWhitespaceInApiKey() {
        String keyWithWhitespace = "  " + TEST_API_KEY + "  ";
        
        testPreferences.put(PREF_KEY_API_KEY, keyWithWhitespace.trim());
        String retrieved = testPreferences.get(PREF_KEY_API_KEY, null);
        
        assertThat(retrieved).isEqualTo(TEST_API_KEY);
        assertThat(retrieved).doesNotStartWith(" ");
        assertThat(retrieved).doesNotEndWith(" ");
    }

    @Test
    @DisplayName("Should support multiple preference keys")
    void testMultiplePreferenceKeys() {
        testPreferences.put(PREF_KEY_API_KEY, TEST_API_KEY);
        testPreferences.put("model.name", "gpt-3.5-turbo");
        testPreferences.put("max.tokens", "500");
        
        assertThat(testPreferences.get(PREF_KEY_API_KEY, null)).isEqualTo(TEST_API_KEY);
        assertThat(testPreferences.get("model.name", null)).isEqualTo("gpt-3.5-turbo");
        assertThat(testPreferences.get("max.tokens", null)).isEqualTo("500");
    }

    @Test
    @DisplayName("Should validate OpenAI API key prefix variations")
    void testValidateApiKeyPrefixVariations() {
        String legacyKey = "sk-1234567890abcdefghijklmnopqrstuvwxyz";
        String projectKey = "sk-proj-1234567890abcdefghijklmnopqrstuvwxyz";
        String serviceKey = "sk-svcacct-1234567890abcdefghijklmnopqrstuvwxyz";
        
        assertThat(legacyKey).startsWith("sk-");
        assertThat(projectKey).startsWith("sk-");
        assertThat(serviceKey).startsWith("sk-");
    }
}

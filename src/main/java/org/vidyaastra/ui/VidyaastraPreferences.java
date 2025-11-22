package org.vidyaastra.ui;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;

/**
 * Helper class to manage VidyaAstra preferences including OpenAI configuration.
 */
public class VidyaastraPreferences {
    
    private static final String PREFERENCES_ID = "org.vidyaastra";
    
    // Preference keys
    private static final String OPENAI_BASE_URL_KEY = "openai.baseUrl";
    private static final String OPENAI_API_KEY_KEY = "openai.apiKey";
    private static final String OPENAI_MODEL_KEY = "openai.model";
    
    // Default values
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    
    /**
     * Gets the VidyaAstra preferences instance.
     */
    private static Preferences getPreferences() {
        PreferencesManager prefMan = PreferencesManager.getInstance();
        return prefMan.getPreferencesForSet(PREFERENCES_ID, PREFERENCES_ID);
    }
    
    /**
     * Gets the OpenAI base URL from preferences.
     * @return The base URL, or default if not set
     */
    public static String getOpenAiBaseUrl() {
        return getPreferences().getString(OPENAI_BASE_URL_KEY, DEFAULT_BASE_URL);
    }
    
    /**
     * Sets the OpenAI base URL in preferences.
     * @param baseUrl The base URL to set
     */
    public static void setOpenAiBaseUrl(String baseUrl) {
        getPreferences().putString(OPENAI_BASE_URL_KEY, baseUrl);
    }
    
    /**
     * Gets the OpenAI API key from preferences.
     * @return The API key, or empty string if not set
     */
    public static String getOpenAiApiKey() {
        return getPreferences().getString(OPENAI_API_KEY_KEY, "");
    }
    
    /**
     * Sets the OpenAI API key in preferences.
     * @param apiKey The API key to set
     */
    public static void setOpenAiApiKey(String apiKey) {
        getPreferences().putString(OPENAI_API_KEY_KEY, apiKey);
    }
    
    /**
     * Gets the OpenAI model name from preferences.
     * @return The model name, or default if not set
     */
    public static String getOpenAiModel() {
        return getPreferences().getString(OPENAI_MODEL_KEY, DEFAULT_MODEL);
    }
    
    /**
     * Sets the OpenAI model name in preferences.
     * @param model The model name to set
     */
    public static void setOpenAiModel(String model) {
        getPreferences().putString(OPENAI_MODEL_KEY, model);
    }
    
    /**
     * Checks if OpenAI is configured (has an API key).
     * @return true if API key is set, false otherwise
     */
    public static boolean isOpenAiConfigured() {
        String apiKey = getOpenAiApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}

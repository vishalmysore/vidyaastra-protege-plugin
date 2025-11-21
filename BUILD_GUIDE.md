# Vidyaastra Plugin - Build and Installation Guide

## Quick Build

To build the Vidyaastra plugin, run:

```bash
cd c:\work\vidyaastra-plugin
mvn clean package
```

The compiled plugin JAR will be located at:
```
c:\work\vidyaastra-plugin\target\vidyaastra-1.0.0.jar
```

## Installation Steps

1. **Build the Plugin** (see above)

2. **Locate Protégé Plugins Folder**:
   - Windows: `%APPDATA%\Protege\plugins`
   - macOS: `~/Library/Application Support/Protege/plugins`
   - Linux: `~/.Protege/plugins`

3. **Copy the JAR file** from `target/vidyaastra-1.0.0.jar` to the Protégé plugins folder

4. **Restart Protégé**

## Accessing the Plugin

Once installed, access the plugin via:
- **Menu**: Tools > AI Integration - Send Query to LLM...

## Plugin Features

### Main Interface Components:

1. **Header Section**
   - Displays the current active ontology name and IRI

2. **Query Input Area**
   - Multi-line text area for entering queries to the LLM
   - Supports line wrapping for better readability
   - Monospaced font for technical queries

3. **Action Buttons**
   - **Send Query**: Sends the query to the LLM service
   - **Clear**: Clears both query and response areas

4. **Response Display Area**
   - Read-only text area showing LLM responses
   - Light gray background for distinction
   - Monospaced font for formatted responses

## White-Label Implementation

This plugin is a complete white-label of the Cellfie plugin with the following changes:

### Branding Changes:
- Package name: `org.mm.cellfie.*` → `org.vidyaastra.*`
- Plugin name: "Cellfie" → "Vidyaastra"
- Menu item: "Create axioms from Excel workbook..." → "AI Integration - Send Query to LLM..."
- Artifact ID: `cellfie` → `vidyaastra`

### Functionality Changes:
- Removed Excel/spreadsheet processing UI
- Added AI query interface with:
  - Query input text area
  - Response display text area
  - Send/Clear action buttons
- Simplified dialog without complex tabbed panels
- Focused on LLM interaction workflow

### Dependencies:
- **No new dependencies added**
- Uses exactly the same dependencies as Cellfie plugin
- Compatible with Protégé 5.6.4+
- Java 11 target

## Project Structure

```
vidyaastra-plugin/
├── src/main/java/org/vidyaastra/
│   ├── action/
│   │   ├── VidyaastraAction.java          # Main entry point
│   │   ├── OWLProtegeEntityResolver.java  # Entity resolution
│   │   └── OWLProtegeOntology.java        # Ontology access
│   └── ui/
│       ├── exception/
│       │   └── VidyaastraException.java   # Custom exception
│       └── view/
│           ├── AIQueryPanel.java          # Main UI panel
│           └── LogUtils.java              # Logging utilities
├── src/main/resources/
│   └── readme.html                        # Plugin documentation
├── plugin.xml                             # Protégé plugin configuration
├── pom.xml                                # Maven build configuration
└── README.md                              # Project documentation
```

## Customization Guide

### Adding LLM API Integration

To integrate with an actual LLM service (OpenAI, Azure OpenAI, etc.):

1. **Add HTTP Client** (using existing dependencies):
   ```java
   // Use java.net.http.HttpClient (Java 11+)
   // No additional dependencies required
   ```

2. **Modify AIQueryPanel.sendQuery()** method:
   ```java
   private void sendQuery() {
       String query = queryTextArea.getText().trim();
       
       // Add your LLM API call here
       // Example:
       // String response = callLLMAPI(query);
       // responseTextArea.setText(response);
   }
   ```

3. **Add Configuration** for API keys:
   - Create a settings panel
   - Store API keys securely
   - Add configuration file support

### Extending the UI

The `AIQueryPanel` class can be extended with:
- History panel for previous queries
- Export button for saving responses
- Template selector for common queries
- Multi-turn conversation support

## Testing

After building and installing:

1. Open Protégé
2. Load any ontology
3. Go to Tools > AI Integration - Send Query to LLM...
4. Enter a test query
5. Click "Send Query"
6. Verify the placeholder response appears

## Troubleshooting

### Plugin doesn't appear in Tools menu
- Check that the JAR is in the correct plugins folder
- Verify Protégé was restarted after installation
- Check Protégé logs for loading errors

### Build fails
- Ensure Java 11 or higher is installed
- Verify Maven is properly configured
- Check internet connection for dependency downloads

### Runtime errors
- Check that all dependencies are properly bundled
- Verify plugin.xml configuration
- Review Protégé console for error messages

## Next Steps

1. **Implement LLM Integration**: Add actual API calls to OpenAI, Azure OpenAI, or custom endpoints
2. **Add Configuration UI**: Create settings panel for API keys and preferences
3. **Enhance UI**: Add query history, templates, and export functionality
4. **Add Error Handling**: Implement robust error handling for API failures
5. **Add Tests**: Create unit and integration tests for the plugin

## License

GNU Lesser General Public License (LGPL)

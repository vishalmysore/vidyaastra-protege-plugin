# Vidyaastra Plugin

A Protégé Desktop plugin for AI Integration with Large Language Models and Interactive Graph Visualization.

## Overview

Vidyaastra is a white-labeled plugin based on the Cellfie plugin architecture, designed to provide AI integration capabilities and interactive graph visualization within Protégé. It enables users to:

1. Send queries to Large Language Models (LLMs) and receive intelligent responses
2. Visualize ontology structure through interactive graph views

## Features

### AI Integration
- **Interactive AI Query Interface**: Intuitive UI integrated into Protégé's Tools menu
- **Context-Aware**: Displays current ontology information in the interface
- **Clean Design**: Separate query input and response display areas
- **Extensible**: Easy to integrate with various LLM APIs (OpenAI, Azure OpenAI, etc.)

### VidyaAstra Graph
- **Interactive Graph Visualization**: Tab view showing ontology structure as a graph
- **Multiple Layout Algorithms**: FR, Circle, KK, Spring, and ISOM layouts
- **Entity Distinction**: Visual differentiation between classes and object properties
- **Interactive Controls**: Zoom, pan, and click-to-select functionality
- **Relationship Display**: Automatic visualization of subclass relationships
- **Customizable**: Toolbar with refresh and layout switching options

## Requirements

- Java 11 or higher
- Protégé 5.6.4 or higher
- Maven 3.x (for building from source)

## Installation

### From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/protegeproject/vidyaastra-plugin.git
   cd vidyaastra-plugin
   ```

2. Build the plugin:
   ```bash
   mvn clean package
   ```

3. Copy the generated JAR file from `target/vidyaastra-1.0.0.jar` to your Protégé plugins folder:
   - Windows: `%APPDATA%\Protege\plugins`
   - macOS: `~/Library/Application Support/Protege/plugins`
   - Linux: `~/.Protege/plugins`

4. Restart Protégé

## Usage

### AI Integration

1. Open your ontology in Protégé
2. Navigate to **Tools > AI Integration - Send Query to LLM...**
3. Enter your query in the text area
4. Click **Send Query** to interact with the AI service
5. View the response in the response panel below

### VidyaAstra Graph

1. Open your ontology in Protégé
2. Navigate to **Window > Views > Ontology views > VidyaAstra Graph**
3. The graph will automatically display your ontology structure
4. Use the toolbar to:
   - Switch between layout algorithms (FR, Circle, KK, Spring, ISOM)
   - Zoom in/out
   - Reset view
   - Refresh graph after ontology changes
5. Click on nodes to select entities
6. Drag to pan the view
7. Use mouse wheel to zoom

## Project Structure

```
vidyaastra-plugin/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/
│       │       └── vidyaastra/
│       │           ├── action/
│       │           │   └── VidyaastraAction.java
│       │           └── ui/
│       │               ├── VidyaastraDialogManager.java
│       │               ├── exception/
│       │               │   └── VidyaastraException.java
│       │               └── view/
│       │                   ├── AIQueryPanel.java
│       │                   ├── LogUtils.java
│       │                   ├── VidyaastraGraphView.java
│       │                   └── VidyaastraGraphPanel.java
│       └── resources/
│           └── readme.html
├── update-info/
│   ├── protege-4/
│   │   └── update.properties
│   └── protege-5/
│       └── update.properties
├── plugin.xml
├── pom.xml
└── README.md
```

## Configuration

The plugin uses the following dependencies:
- Protégé Editor Core & OWL (5.6.4)
- Apache POI (5.4.0)
- Log4j (2.20.0)
- Apache Commons libraries
- JUNG 2.1.1 (Java Universal Network/Graph Framework)
- JGraphT 1.5.2

## Future Enhancements

### AI Integration
- Integration with OpenAI API
- Integration with Azure OpenAI Service
- Support for custom LLM endpoints
- Query history and session management
- Export responses to files
- Advanced prompt templates
- Configuration UI for API keys and settings

### Graph Visualization
- Graph export (PNG, SVG, PDF)
- Advanced filtering options
- Custom node and edge styling
- Show/hide specific relationship types
- Search and highlight in graph
- Clustering and grouping options

## License

This project is licensed under the GNU Lesser General Public License (LGPL).
See [LICENSE](https://www.gnu.org/licenses/lgpl.html) for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

This plugin is based on the architecture of the [Cellfie Plugin](https://github.com/protegeproject/cellfie-plugin) for Protégé.

## Contact

For questions, issues, or suggestions, please open an issue on the GitHub repository.

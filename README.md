# VidyaAstra Plugin

**VidyaAstra** is an AI-powered plugin for Protégé that brings intelligent ontology visualization and AI integration capabilities to your knowledge engineering workflow.

![VidyaAstra Screenshot](vidyaastra.jpg)

## Features

### 🤖 AI Integration
**Three powerful AI-powered modes for ontology engineering**

#### 1. Basic Query Mode
- **Interactive AI Query Interface**: Ask questions about your ontology in plain English
- **Context-Aware Responses**: The AI understands your current ontology context
- **Test & Explore**: Perfect for testing basic functionality and exploring ontology structure

#### 2. Create New Ontology Mode ✨ NEW!
- **Natural Language Ontology Generation**: Describe an ontology in plain English
- **Complete OWL/RDF XML Output**: LLM generates valid, ready-to-use ontology files
- **Save & Load**: Save generated ontologies and optionally load them directly into Protégé
- **Rapid Prototyping**: Quickly create ontology scaffolds for your projects

#### 3. Modify Ontology Mode ✨ NEW!
- **AI-Guided Modifications**: Describe changes in natural language
- **Intelligent Analysis**: AI analyzes your current ontology structure
- **Automatic Application**: Changes are applied directly using OWL API
- **Comprehensive Operations**: Add/remove classes, properties, axioms, domains, ranges, and more

**Common Features Across All Modes:**
- **LLM Integration Ready**: Supports OpenAI, Azure OpenAI, and custom endpoints
- **Clean, Intuitive UI**: Radio buttons to switch modes, with dynamic labels
- **Configuration Management**: Save API settings for reuse
- **Accessible from Tools Menu**: **Tools > AI Integration - Send Query to LLM...**

📖 **See [FEATURE_GUIDE.md](FEATURE_GUIDE.md) for detailed usage instructions and examples**

### 📊 VidyaAstra Graph - Interactive Ontology Visualization
**Explore your ontology structure through powerful, interactive graph visualization**

- **Hierarchical Display**: Automatically shows class hierarchies with expand/collapse functionality
- **Instance Visualization**: View individuals and their relationships to classes
- **Object Property Relationships**: Expand individuals to see all their connections (just like OntoGraf!)
- **Visual Differentiation**: 
  - **Blue circles** for classes
  - **Pink squares** for individuals
  - **Green lines** for object properties
  - **Gray dashed lines** for class hierarchies
  - **Purple dashed lines** for instance relationships
- **Interactive Controls**:
  - **Double-click** nodes to expand/collapse relationships
  - **Drag** individual nodes to reposition
  - **Pan and Zoom** for easy navigation
  - **[+]/[-]** indicators show expandable/collapsible nodes
- **Multiple Layout Algorithms**: Choose from FR (Fruchterman-Reingold), Circle, KK (Kamada-Kawai), Spring, and ISOM layouts
- **Edge Labels**: See relationship names (alliedWith, mentored, killed, etc.) directly on the graph
- **Smart Node Sizing**: Nodes automatically resize to fit labels
- **Gold Selection Highlight**: Selected entities are highlighted in gold for easy identification

## Requirements

- **Java**: 11 or higher
- **Protégé**: 5.6.4 or higher
- **Maven**: 3.x (for building from source)

## Installation

1. **Build the plugin**:
   ```bash
   mvn clean package
   ```

2. **Copy the JAR** from `target/vidyaastra-1.0.0.jar` to your Protégé plugins folder:
   - **Windows**: `C:\Users\<username>\protege\Protege-5.6.x\plugins\`
   - **macOS**: `~/Library/Application Support/Protege/plugins`
   - **Linux**: `~/.Protege/plugins`

3. **Restart Protégé**

## Usage

### Using VidyaAstra Graph

1. Open your ontology in Protégé
2. Navigate to **Window > Views > Miscellaneous views > VidyaAstra Graph**
3. The graph displays root classes automatically
4. **Double-click** any class with **[+]** to expand and see:
   - Subclasses (connected with gray dashed lines)
   - Instances (connected with purple dashed lines)
5. **Double-click** any individual with **[+]** to see:
   - Related individuals via object properties (connected with green solid lines)
   - Property names displayed as edge labels
6. **Single-click** to select entities (turns gold)
7. Use toolbar buttons to:
   - Switch layout algorithms
   - Toggle between **Drag Nodes** and **Pan/Zoom** modes
   - Zoom in/out or reset view
   - Refresh the graph

### Using AI Integration

1. Open your ontology in Protégé (or start without one for creating new ontologies)
2. Navigate to **Tools > AI Integration - Send Query to LLM...**
3. **Configure OpenAI settings** (one-time setup):
   - Base URL: `https://api.openai.com/v1`
   - API Key: Your OpenAI API key
   - Model: `gpt-4o-mini` or `gpt-4o`
   - Click **Save Config**
4. **Select your operation mode**:
   - **Basic Query**: Ask questions about your ontology
   - **Create New Ontology**: Generate a new ontology from description
   - **Modify Ontology**: Apply changes to the current ontology
5. Enter your request in the text area
6. Click the action button and view results

**Example - Create New Ontology:**
```
Create a simple university ontology with:
- Classes: University, Department, Professor, Student, Course
- Object properties: teaches, enrollsIn, belongsTo
- Data properties: name, email, studentID
```

**Example - Modify Ontology:**
```
Add a new class "GraduateStudent" as a subclass of Student
Add a data property "thesisTitle" with range string
```

**Example - Basic Query:**
```
List all the object properties and their domains and ranges
```

## Technology Stack

- **Protégé API** (5.6.4) - Ontology access and manipulation
- **JUNG 2.1.1** - Java Universal Network/Graph Framework for visualization
- **JGraphT 1.5.2** - Graph algorithms and data structures
- **OWL API** - Ontology processing
- **Apache POI** (5.4.0) - Document processing
- **Log4j 2** (2.20.0) - Logging



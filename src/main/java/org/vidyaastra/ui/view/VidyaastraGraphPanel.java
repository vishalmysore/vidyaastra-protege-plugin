package org.vidyaastra.ui.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.vidyaastra.OpenAiCaller;
import org.vidyaastra.ui.VidyaastraPreferences;

import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Collectors;
import java.io.StringWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;

/**
 * VidyaastraGraphPanel provides an interactive graph visualization
 * of the OWL ontology using JUNG framework.
 */
public class VidyaastraGraphPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   private static final Logger logger = LoggerFactory.getLogger(VidyaastraGraphPanel.class);

   private OWLModelManager modelManager;
   private Graph<OWLEntity, OWLRelationship> graph;
   private VisualizationViewer<OWLEntity, OWLRelationship> viewer;
   private OWLEntity selectedEntity;
   private Layout<OWLEntity, OWLRelationship> currentLayout;
   private DefaultModalGraphMouse<OWLEntity, OWLRelationship> graphMouse;
   private Set<OWLEntity> expandedNodes;
   private OWLOntology ontology;

   // NLP Query components
   private JTextField queryField;
   private JTextArea resultsArea;
   private JButton executeQueryButton;

   public VidyaastraGraphPanel(OWLModelManager modelManager) {
      this.modelManager = modelManager;
      this.expandedNodes = new HashSet<>();
      setLayout(new BorderLayout());

      initializeGraph();
      createViewer();
      createToolbar();
      createQueryPanel();
   }

   private void initializeGraph() {
      graph = new DirectedSparseGraph<>();
      buildGraphFromOntology();
   }

   private void buildGraphFromOntology() {
      if (modelManager == null)
         return;

      ontology = modelManager.getActiveOntology();
      if (ontology == null)
         return;

      Set<OWLClass> classes = ontology.getClassesInSignature();

      // Start with top-level classes (those that are direct subclasses of owl:Thing)
      for (OWLClass cls : classes) {
         if (!cls.isOWLThing() && !cls.isOWLNothing()) {
            Set<OWLClass> superClasses = ontology.getSubClassAxiomsForSubClass(cls)
                  .stream()
                  .filter(ax -> !ax.getSuperClass().isAnonymous())
                  .map(ax -> ax.getSuperClass().asOWLClass())
                  .collect(java.util.stream.Collectors.toSet());

            // If this class has only owl:Thing as superclass, it's a root
            if (superClasses.isEmpty() || superClasses.stream().allMatch(sc -> sc.isOWLThing())) {
               graph.addVertex(cls);
               expandedNodes.add(cls); // Auto-expand root nodes
               addChildrenToGraph(cls);
            }
         }
      }

      // Add object properties
      Set<OWLObjectProperty> properties = ontology.getObjectPropertiesInSignature();
      for (OWLObjectProperty prop : properties) {
         graph.addVertex(prop);
      }
   }

   private void addChildrenToGraph(OWLEntity parent) {
      if (!expandedNodes.contains(parent))
         return;

      // Handle Classes - show subclasses and instances
      if (parent instanceof OWLClass) {
         OWLClass parentClass = (OWLClass) parent;

         // Get direct subclasses
         Set<OWLClass> subClasses = ontology.getAxioms(org.semanticweb.owlapi.model.AxiomType.SUBCLASS_OF)
               .stream()
               .filter(ax -> !ax.getSuperClass().isAnonymous() &&
                     ax.getSuperClass().asOWLClass().equals(parentClass))
               .filter(ax -> !ax.getSubClass().isAnonymous())
               .map(ax -> ax.getSubClass().asOWLClass())
               .collect(java.util.stream.Collectors.toSet());

         for (OWLClass subClass : subClasses) {
            if (!subClass.isOWLThing() && !subClass.isOWLNothing()) {
               graph.addVertex(subClass);
               graph.addEdge(new OWLRelationship(subClass, parentClass, "subClassOf"),
                     subClass, parentClass);

               // Recursively add children if this node is expanded
               if (expandedNodes.contains(subClass)) {
                  addChildrenToGraph(subClass);
               }
            }
         }

         // Also get instances of this class
         Set<org.semanticweb.owlapi.model.OWLNamedIndividual> instances = ontology
               .getAxioms(org.semanticweb.owlapi.model.AxiomType.CLASS_ASSERTION)
               .stream()
               .filter(ax -> !ax.getClassExpression().isAnonymous() &&
                     ax.getClassExpression().asOWLClass().equals(parentClass))
               .map(ax -> ax.getIndividual())
               .filter(ind -> ind.isNamed())
               .map(ind -> ind.asOWLNamedIndividual())
               .collect(java.util.stream.Collectors.toSet());

         for (org.semanticweb.owlapi.model.OWLNamedIndividual instance : instances) {
            graph.addVertex(instance);
            graph.addEdge(new OWLRelationship(instance, parentClass, "instanceOf"),
                  instance, parentClass);
         }
      }
      // Handle Individuals - show object property relationships
      else if (parent instanceof OWLNamedIndividual) {
         OWLNamedIndividual individual = (OWLNamedIndividual) parent;

         // Get all object property assertions where this individual is the subject
         ontology.getObjectPropertyAssertionAxioms(individual).forEach(ax -> {
            OWLObjectProperty property = ax.getProperty().asOWLObjectProperty();
            org.semanticweb.owlapi.model.OWLIndividual object = ax.getObject();

            if (object.isNamed()) {
               OWLNamedIndividual targetIndividual = object.asOWLNamedIndividual();

               // Add the target individual if not already in graph
               graph.addVertex(targetIndividual);

               // Add edge with property name
               String propertyName = property.getIRI().getShortForm();
               graph.addEdge(new OWLRelationship(individual, targetIndividual, propertyName),
                     individual, targetIndividual);
            }
         });
      }
   }

   public void toggleNodeExpansion(OWLEntity entity) {
      if (expandedNodes.contains(entity)) {
         // Collapse: remove this node from expanded set and remove its descendants
         expandedNodes.remove(entity);
         removeDescendants(entity);
      } else {
         // Expand: add to expanded set and add children
         expandedNodes.add(entity);
         addChildrenToGraph(entity);
      }

      // Refresh layout
      Dimension size = viewer.getSize();
      if (size.width == 0 || size.height == 0) {
         size = new Dimension(800, 600);
      }
      currentLayout.setSize(size);
      currentLayout.initialize();
      viewer.repaint();
   }

   private void removeDescendants(OWLEntity parent) {
      Set<OWLEntity> toRemove = new HashSet<>();

      // Handle Classes - remove subclasses and instances
      if (parent instanceof OWLClass) {
         OWLClass parentClass = (OWLClass) parent;

         for (OWLEntity vertex : new HashSet<>(graph.getVertices())) {
            // Remove subclasses
            if (vertex instanceof OWLClass && isDescendantOf((OWLClass) vertex, parentClass)) {
               toRemove.add(vertex);
               expandedNodes.remove(vertex);
            }
            // Remove instances of this class
            else if (vertex instanceof OWLNamedIndividual) {
               boolean isInstanceOfParent = ontology.getClassAssertionAxioms((OWLNamedIndividual) vertex)
                     .stream()
                     .anyMatch(ax -> !ax.getClassExpression().isAnonymous() &&
                           ax.getClassExpression().asOWLClass().equals(parentClass));
               if (isInstanceOfParent) {
                  toRemove.add(vertex);
               }
            }
         }
      }
      // Handle Individuals - remove related individuals added via object properties
      else if (parent instanceof OWLNamedIndividual) {
         OWLNamedIndividual individual = (OWLNamedIndividual) parent;

         // Remove all individuals that were added as targets of this individual's
         // relationships
         for (OWLEntity vertex : new HashSet<>(graph.getVertices())) {
            if (vertex instanceof OWLNamedIndividual && !vertex.equals(parent)) {
               // Check if there's an edge from parent to this vertex
               for (OWLRelationship edge : new HashSet<>(graph.getEdges())) {
                  if (edge.getSource().equals(parent) && edge.getTarget().equals(vertex)) {
                     // Only remove if this vertex was added solely through this relationship
                     // (i.e., it's not also an instance shown from a class expansion)
                     toRemove.add(vertex);
                     expandedNodes.remove(vertex);
                  }
               }
            }
         }
      }

      for (OWLEntity entity : toRemove) {
         graph.removeVertex(entity);
      }
   }

   private boolean isDescendantOf(OWLClass child, OWLClass ancestor) {
      Set<OWLClass> superClasses = ontology.getSubClassAxiomsForSubClass(child)
            .stream()
            .filter(ax -> !ax.getSuperClass().isAnonymous())
            .map(ax -> ax.getSuperClass().asOWLClass())
            .collect(java.util.stream.Collectors.toSet());

      if (superClasses.contains(ancestor)) {
         return true;
      }

      for (OWLClass superClass : superClasses) {
         if (!superClass.isOWLThing() && isDescendantOf(superClass, ancestor)) {
            return true;
         }
      }

      return false;
   }

   private boolean hasChildren(OWLEntity entity) {
      if (ontology == null)
         return false;

      // For classes: check for subclasses or instances
      if (entity instanceof OWLClass) {
         OWLClass parentClass = (OWLClass) entity;

         // Check for subclasses
         boolean hasSubClasses = ontology.getAxioms(org.semanticweb.owlapi.model.AxiomType.SUBCLASS_OF)
               .stream()
               .anyMatch(ax -> !ax.getSuperClass().isAnonymous() &&
                     ax.getSuperClass().asOWLClass().equals(parentClass) &&
                     !ax.getSubClass().isAnonymous());

         // Check for instances
         boolean hasInstances = ontology.getAxioms(org.semanticweb.owlapi.model.AxiomType.CLASS_ASSERTION)
               .stream()
               .anyMatch(ax -> !ax.getClassExpression().isAnonymous() &&
                     ax.getClassExpression().asOWLClass().equals(parentClass) &&
                     ax.getIndividual().isNamed());

         return hasSubClasses || hasInstances;
      }
      // For individuals: check for object property relationships
      else if (entity instanceof OWLNamedIndividual) {
         OWLNamedIndividual individual = (OWLNamedIndividual) entity;
         return !ontology.getObjectPropertyAssertionAxioms(individual).isEmpty();
      }

      return false;
   }

   private void createViewer() {
      // Use Fruchterman-Reingold layout by default with reasonable dimensions
      Dimension viewerSize = new Dimension(800, 600);
      currentLayout = new FRLayout<>(graph);
      currentLayout.setSize(viewerSize);

      viewer = new VisualizationViewer<>(currentLayout);
      viewer.setPreferredSize(viewerSize);
      viewer.setBackground(new Color(250, 250, 250)); // Very light gray background

      // Set up renderers
      setupNodeRenderer();
      setupEdgeRenderer();
      setupMouseControl();

      // We'll add viewer to a split pane in createQueryPanel()
   }

   private void setupNodeRenderer() {
      // Custom vertex label with expand/collapse indicator
      viewer.getRenderContext().setVertexLabelTransformer(entity -> {
         String label = entity.getIRI().getShortForm();

         // Add indicator for expandable nodes (both classes and individuals)
         if (hasChildren(entity)) {
            if (expandedNodes.contains(entity)) {
               return "[-] " + label; // Expanded (can collapse)
            } else {
               return "[+] " + label; // Collapsed (can expand)
            }
         }

         return label;
      });

      // Dynamic vertex shapes based on text length
      viewer.getRenderContext().setVertexShapeTransformer(entity -> {
         String label = entity.getIRI().getShortForm();
         int textLength = label.length();

         if (entity instanceof OWLClass) {
            // Circles that grow with text length
            int diameter = Math.max(60, Math.min(textLength * 7, 150));
            return new Ellipse2D.Double(-diameter / 2.0, -diameter / 2.0, diameter, diameter);
         } else if (entity instanceof OWLObjectProperty) {
            // Rectangles that expand to fit text
            int width = Math.max(80, textLength * 8);
            int height = 50;
            return new Rectangle2D.Double(-width / 2.0, -height / 2.0, width, height);
         } else if (entity instanceof OWLNamedIndividual) {
            // Hexagons/diamonds for individuals (using rectangles rotated visually via
            // size)
            int size = Math.max(50, Math.min(textLength * 6, 120));
            return new Rectangle2D.Double(-size / 2.0, -size / 2.0, size, size);
         }
         int diameter = Math.max(60, Math.min(textLength * 7, 150));
         return new Ellipse2D.Double(-diameter / 2.0, -diameter / 2.0, diameter, diameter);
      });

      // Professional gradient-like colors
      viewer.getRenderContext().setVertexFillPaintTransformer(entity -> {
         if (entity.equals(selectedEntity)) {
            return new Color(255, 140, 0); // Bright orange for selected - very visible!
         } else if (entity instanceof OWLClass) {
            return new Color(100, 149, 237); // Cornflower blue - more vibrant
         } else if (entity instanceof OWLObjectProperty) {
            return new Color(60, 179, 113); // Medium sea green - professional
         } else if (entity instanceof OWLNamedIndividual) {
            return new Color(255, 182, 193); // Light pink - for individuals
         }
         return new Color(176, 196, 222); // Light steel blue
      });

      // Thicker, more visible borders
      viewer.getRenderContext().setVertexDrawPaintTransformer(entity -> {
         if (entity.equals(selectedEntity)) {
            return new Color(220, 20, 60); // Crimson for selected - bold border!
         }
         return new Color(47, 79, 79); // Dark slate gray
      });

      // Custom vertex stroke (border thickness)
      viewer.getRenderContext().setVertexStrokeTransformer(entity -> {
         if (entity.equals(selectedEntity)) {
            return new BasicStroke(4.0f); // Much thicker border for selected
         }
         return new BasicStroke(2.0f); // Normal border
      });

      // Vertex label position - centered
      viewer.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

      // Better font - smaller for longer text
      viewer.getRenderContext().setVertexFontTransformer(entity -> {
         String label = entity.getIRI().getShortForm();
         int fontSize = label.length() > 12 ? 10 : 11;

         if (entity.equals(selectedEntity)) {
            return new Font("SansSerif", Font.BOLD, fontSize + 2); // Bigger and bolder!
         }
         return new Font("SansSerif", Font.BOLD, fontSize);
      });
   }

   private void setupEdgeRenderer() {
      // Show edge labels - display relationship type
      viewer.getRenderContext().setEdgeLabelTransformer((OWLRelationship rel) -> {
         String relationshipType = rel.getRelationType();
         // Show property names for object properties, but hide "subClassOf" and
         // "instanceOf" for cleaner look
         if (relationshipType.equals("subClassOf") || relationshipType.equals("instanceOf")) {
            return "";
         }
         return relationshipType;
      });

      // Professional edge color - different colors for different relationship types
      viewer.getRenderContext().setEdgeDrawPaintTransformer((OWLRelationship rel) -> {
         String relType = rel.getRelationType();
         if (relType.equals("subClassOf")) {
            return new Color(105, 105, 105); // Dim gray for hierarchy
         } else if (relType.equals("instanceOf")) {
            return new Color(147, 112, 219); // Medium purple for instances
         } else {
            return new Color(34, 139, 34); // Forest green for object properties
         }
      });

      // Edge stroke - solid for object properties, dashed for hierarchy
      viewer.getRenderContext().setEdgeStrokeTransformer((OWLRelationship rel) -> {
         String relType = rel.getRelationType();
         if (relType.equals("subClassOf") || relType.equals("instanceOf")) {
            float[] dash = { 10.0f, 5.0f };
            return new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                  10.0f, dash, 0.0f); // Dashed line for hierarchy
         } else {
            return new BasicStroke(2.5f); // Solid, thicker line for object properties
         }
      });

      // Show directional arrows for edges
      viewer.getRenderContext().setEdgeArrowPredicate(edge -> true);

      // Larger arrow size
      viewer.getRenderContext().setEdgeArrowStrokeTransformer(edge -> new BasicStroke(1.5f));

      // Edge label font - make property names visible
      viewer.getRenderContext().setEdgeFontTransformer(edge -> new Font("SansSerif", Font.PLAIN, 10));
   }

   private void setupMouseControl() {
      graphMouse = new DefaultModalGraphMouse<>();
      // Start in PICKING mode so users can drag nodes immediately
      graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
      viewer.setGraphMouse(graphMouse);

      // Add mouse click listener for entity selection and expansion
      viewer.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            OWLEntity entity = viewer.getPickSupport().getVertex(
                  viewer.getGraphLayout(), e.getX(), e.getY());
            if (entity != null) {
               if (e.getClickCount() == 2) {
                  // Double-click to expand/collapse
                  toggleNodeExpansion(entity);
               } else {
                  // Single click to select
                  setSelectedEntity(entity);
               }
            }
         }
      });
   }

   private void createToolbar() {
      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);

      // Mouse mode buttons
      JButton pickingModeBtn = new JButton("Drag Nodes");
      JButton transformingModeBtn = new JButton("Pan/Zoom");

      pickingModeBtn.setToolTipText("Switch to picking mode - drag and select nodes");
      pickingModeBtn.addActionListener(e -> {
         graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
         pickingModeBtn.setEnabled(false);
         transformingModeBtn.setEnabled(true);
      });
      pickingModeBtn.setEnabled(false); // Start disabled since we begin in PICKING mode
      toolbar.add(pickingModeBtn);

      transformingModeBtn.setToolTipText("Switch to transforming mode - pan and zoom the view");
      transformingModeBtn.addActionListener(e -> {
         graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
         transformingModeBtn.setEnabled(false);
         pickingModeBtn.setEnabled(true);
      });
      toolbar.add(transformingModeBtn);

      toolbar.addSeparator();

      // Layout buttons
      JButton frLayoutBtn = new JButton("FR Layout");
      frLayoutBtn.addActionListener(e -> changeLayout(new FRLayout<>(graph)));
      toolbar.add(frLayoutBtn);

      JButton circleLayoutBtn = new JButton("Circle Layout");
      circleLayoutBtn.addActionListener(e -> changeLayout(new CircleLayout<>(graph)));
      toolbar.add(circleLayoutBtn);

      JButton kkLayoutBtn = new JButton("KK Layout");
      kkLayoutBtn.addActionListener(e -> changeLayout(new KKLayout<>(graph)));
      toolbar.add(kkLayoutBtn);

      JButton springLayoutBtn = new JButton("Spring Layout");
      springLayoutBtn.addActionListener(e -> changeLayout(new SpringLayout<>(graph)));
      toolbar.add(springLayoutBtn);

      JButton isomLayoutBtn = new JButton("ISOM Layout");
      isomLayoutBtn.addActionListener(e -> changeLayout(new ISOMLayout<>(graph)));
      toolbar.add(isomLayoutBtn);

      toolbar.addSeparator();

      // Zoom buttons
      JButton zoomInBtn = new JButton("Zoom In");
      zoomInBtn.addActionListener(e -> viewer.getRenderContext().getMultiLayerTransformer()
            .getTransformer(edu.uci.ics.jung.visualization.Layer.VIEW).scale(1.1, 1.1,
                  viewer.getCenter()));
      toolbar.add(zoomInBtn);

      JButton zoomOutBtn = new JButton("Zoom Out");
      zoomOutBtn.addActionListener(e -> viewer.getRenderContext().getMultiLayerTransformer()
            .getTransformer(edu.uci.ics.jung.visualization.Layer.VIEW).scale(0.9, 0.9,
                  viewer.getCenter()));
      toolbar.add(zoomOutBtn);

      JButton resetBtn = new JButton("Reset View");
      resetBtn.addActionListener(e -> {
         viewer.getRenderContext().getMultiLayerTransformer().setToIdentity();
         viewer.repaint();
      });
      toolbar.add(resetBtn);

      toolbar.addSeparator();

      // Refresh button
      JButton refreshBtn = new JButton("Refresh Graph");
      refreshBtn.addActionListener(e -> refreshGraph());
      toolbar.add(refreshBtn);

      // Explain Ontology button
      JButton explainBtn = new JButton("Explain Ontology");
      explainBtn.addActionListener(e -> explainOntology());
      toolbar.add(explainBtn);

      add(toolbar, BorderLayout.NORTH);
   }

   /**
    * Creates the NLP query panel at the bottom for natural language to SPARQL
    * conversion
    */
   private void createQueryPanel() {
      JPanel queryPanel = new JPanel(new BorderLayout(5, 5));
      queryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Natural Language Query (NLP → Ontology)"),
            new EmptyBorder(5, 5, 5, 5)));

      // Top section: Query input
      JPanel inputPanel = new JPanel(new BorderLayout(5, 0));

      queryField = new JTextField();
      queryField.setFont(new Font("Arial", Font.PLAIN, 12));
      queryField.setToolTipText("Enter your query in natural language (e.g., 'Tell me about Arjuna')");

      executeQueryButton = new JButton("Execute Query");
      executeQueryButton.setFont(new Font("Arial", Font.BOLD, 11));
      executeQueryButton.addActionListener(e -> executeNLPQuery());

      inputPanel.add(new JLabel("Query: "), BorderLayout.WEST);
      inputPanel.add(queryField, BorderLayout.CENTER);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
      buttonPanel.add(executeQueryButton);

      JButton exportButton = new JButton("Export");
      exportButton.setFont(new Font("Arial", Font.PLAIN, 11));
      exportButton.setToolTipText("Export results to file");
      exportButton.addActionListener(e -> exportResults());
      buttonPanel.add(exportButton);

      inputPanel.add(buttonPanel, BorderLayout.EAST);

      queryPanel.add(inputPanel, BorderLayout.NORTH);

      // Bottom section: Results display
      resultsArea = new JTextArea(6, 60);
      resultsArea.setEditable(false);
      resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
      resultsArea.setBackground(new Color(245, 245, 245));
      resultsArea.setLineWrap(true);
      resultsArea.setWrapStyleWord(true);
      resultsArea.setText("💡 Try asking: \"Tell me about Arjuna\" or \"List all warriors\"");

      JScrollPane resultsScroll = new JScrollPane(resultsArea);
      resultsScroll.setPreferredSize(new Dimension(0, 120));
      queryPanel.add(resultsScroll, BorderLayout.CENTER);

      // Create split pane with graph viewer on top and query panel on bottom
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

      // Create scroll pane for viewer with both scrollbars
      JScrollPane viewerScroll = new JScrollPane(viewer);
      viewerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      viewerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

      splitPane.setTopComponent(viewerScroll);
      splitPane.setBottomComponent(queryPanel);
      splitPane.setDividerLocation(0.7); // 70% for graph, 30% for query
      splitPane.setResizeWeight(0.7); // Graph gets 70% of extra space
      splitPane.setOneTouchExpandable(true); // Add expand/collapse arrows
      splitPane.setDividerSize(8);

      add(splitPane, BorderLayout.CENTER);
   }

   /**
    * Executes NLP query: converts natural language to SPARQL using OpenAI, then
    * runs it
    */
   private void executeNLPQuery() {
      String nlQuery = queryField.getText().trim();

      if (nlQuery.isEmpty()) {
         resultsArea.setText("❌ Please enter a query.");
         return;
      }

      // Check OpenAI configuration
      if (!VidyaastraPreferences.isOpenAiConfigured()) {
         resultsArea.setText("❌ OpenAI not configured!\n\n" +
               "Please configure OpenAI settings in Tools → AI Integration first.\n" +
               "You need to set:\n" +
               "  • Base URL (e.g., https://api.openai.com/v1)\n" +
               "  • API Key\n" +
               "  • Model (e.g., gpt-4o-mini)");
         return;
      }

      executeQueryButton.setEnabled(false);
      resultsArea.setText("⏳ Analyzing query...\n\nNatural Language: " + nlQuery);

      // Use SwingWorker for background processing
      SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
         private String targetEntity = "";
         private String queryType = "";
         private String clarificationQuestion = "";
         private java.util.List<Map<String, String>> filters = new java.util.ArrayList<>();

         @Override
         protected String doInBackground() throws Exception {
            // Get OpenAI configuration
            String baseUrl = VidyaastraPreferences.getOpenAiBaseUrl();
            String apiKey = VidyaastraPreferences.getOpenAiApiKey();
            String model = VidyaastraPreferences.getOpenAiModel();

            OpenAiCaller caller = new OpenAiCaller(apiKey, model, baseUrl);

            // Build system prompt
            String systemPrompt = buildSparqlSystemPrompt();

            logger.info("=== NLP Query Execution ===");
            logger.info("User Query: {}", nlQuery);

            // Call OpenAI to understand the query
            String response = caller.generateCompletion(systemPrompt, nlQuery, 0.3);

            logger.info("Raw LLM Response: {}", response);

            // Parse JSON response manually
            parseJsonResponse(response);

            logger.info("Parsed - Type: {}, Target: {}, Filters: {}", queryType, targetEntity, filters.size());

            // Execute query on the actual ontology graph
            // Execute query on the actual ontology graph
            if ("ambiguous".equalsIgnoreCase(queryType)) {
               return "AMBIGUOUS: " + clarificationQuestion;
            } else if ("complex".equalsIgnoreCase(queryType)) {
               return executeComplexQuery(targetEntity, filters);
            } else {
               return executeQueryOnOntology(queryType, targetEntity);
            }
         }

         private void parseJsonResponse(String response) {
            try {
               // Check if response is in plain text format (QUERY_TYPE: xxx\nTARGET: xxx)
               if (response.contains("QUERY_TYPE:") && response.contains("TARGET:")) {
                  // Parse plain text format
                  String[] lines = response.split("\n");
                  for (String line : lines) {
                     line = line.trim();
                     if (line.startsWith("QUERY_TYPE:")) {
                        queryType = line.substring("QUERY_TYPE:".length()).trim();
                     } else if (line.startsWith("TARGET:")) {
                        targetEntity = line.substring("TARGET:".length()).trim();
                     }
                  }
                  return;
               }
               
               // Otherwise, try JSON parsing
               String json = response;
               // Remove markdown code blocks if present
               json = json.replaceAll("```json", "").replaceAll("```", "").trim();

               // Simple manual parsing to avoid dependencies
               // Extract type
               int typeIdx = json.indexOf("\"type\"");
               if (typeIdx != -1) {
                  int start = json.indexOf("\"", typeIdx + 7) + 1;
                  int end = json.indexOf("\"", start);
                  queryType = json.substring(start, end);
               }

               // Extract target
               int targetIdx = json.indexOf("\"target\"");
               if (targetIdx != -1) {
                  int start = json.indexOf("\"", targetIdx + 9) + 1;
                  int end = json.indexOf("\"", start);
                  targetEntity = json.substring(start, end);
               }

               // Extract question (for ambiguity)
               int questionIdx = json.indexOf("\"question\"");
               if (questionIdx != -1) {
                  int start = json.indexOf("\"", questionIdx + 11) + 1;
                  int end = json.indexOf("\"", start);
                  clarificationQuestion = json.substring(start, end);
               }

               // Extract filters (very basic parsing)
               int filtersIdx = json.indexOf("\"filters\"");
               if (filtersIdx != -1) {
                  String filtersSection = json.substring(filtersIdx);
                  int arrayStart = filtersSection.indexOf("[");
                  int arrayEnd = filtersSection.lastIndexOf("]");
                  if (arrayStart != -1 && arrayEnd != -1) {
                     String arrayContent = filtersSection.substring(arrayStart + 1, arrayEnd);
                     String[] filterObjs = arrayContent.split("},");
                     for (String obj : filterObjs) {
                        Map<String, String> filter = new HashMap<>();
                        extractField(obj, "property", filter);
                        extractField(obj, "value", filter);
                        extractField(obj, "operator", filter);
                        if (!filter.isEmpty()) {
                           filters.add(filter);
                        }
                     }
                  }
               }
            } catch (Exception e) {
               logger.error("Error parsing JSON response", e);
               queryType = "error";
            }
         }

         private void extractField(String source, String field, Map<String, String> map) {
            int idx = source.indexOf("\"" + field + "\"");
            if (idx != -1) {
               int start = source.indexOf("\"", idx + field.length() + 3) + 1;
               int end = source.indexOf("\"", start);
               if (start != -1 && end != -1) {
                  map.put(field, source.substring(start, end));
               }
            }
         }

         @Override
         protected void done() {
            try {
               String results = get();

               if (results.startsWith("AMBIGUOUS: ")) {
                  String question = results.substring(11);
                  String answer = javax.swing.JOptionPane.showInputDialog(VidyaastraGraphPanel.this,
                        question, "Clarification Needed", javax.swing.JOptionPane.QUESTION_MESSAGE);

                  if (answer != null && !answer.trim().isEmpty()) {
                     // Append clarification to original query and retry
                     queryField.setText(nlQuery + " " + answer);
                     executeNLPQuery();
                  } else {
                     resultsArea.setText("❌ Query cancelled or clarification not provided.");
                  }
               } else {
                  // Highlight the entity in the graph
                  highlightEntityInGraph(targetEntity);

                  resultsArea.setText("✅ Query Results from Ontology\n\n" +
                        "Natural Language: " + nlQuery + "\n\n" +
                        "Query Type: " + queryType + "\n" +
                        "Target: " + targetEntity + "\n" +
                        (filters.isEmpty() ? "" : "Filters: " + filters.size() + "\n") + "\n" +
                        results);
               }
            } catch (Exception e) {
               resultsArea.setText("❌ Error executing query:\n\n" + e.getMessage());
               e.printStackTrace();
            } finally {
               executeQueryButton.setEnabled(true);
            }
         }
      };

      worker.execute();
   }

   private String executeComplexQuery(String className, java.util.List<Map<String, String>> filters) {
      StringBuilder result = new StringBuilder();
      result.append("🔍 Complex Query Results for '").append(className).append("':\n\n");

      OWLClass targetClass = ontology.getClassesInSignature().stream()
            .filter(cls -> cls.getIRI().getShortForm().equalsIgnoreCase(className))
            .findFirst().orElse(null);

      if (targetClass == null) {
         return "❌ Class '" + className + "' not found.";
      }

      Set<OWLNamedIndividual> candidates = ontology.getClassAssertionAxioms(targetClass).stream()
            .filter(ax -> ax.getIndividual().isNamed())
            .map(ax -> ax.getIndividual().asOWLNamedIndividual())
            .collect(Collectors.toSet());

      int matchCount = 0;
      for (OWLNamedIndividual ind : candidates) {
         boolean matchesAll = true;

         for (Map<String, String> filter : filters) {
            String propName = filter.get("property");
            String value = filter.get("value");

            // Check object property assertions
            boolean hasProp = ontology.getObjectPropertyAssertionAxioms(ind).stream()
                  .anyMatch(ax -> {
                     String p = ax.getProperty().asOWLObjectProperty().getIRI().getShortForm();
                     String v = ax.getObject().asOWLNamedIndividual().getIRI().getShortForm();
                     return p.equalsIgnoreCase(propName) && v.equalsIgnoreCase(value);
                  });

            if (!hasProp) {
               matchesAll = false;
               break;
            }
         }

         if (matchesAll) {
            matchCount++;
            result.append("  • ").append(ind.getIRI().getShortForm()).append("\n");
            highlightEntityInGraph(ind.getIRI().getShortForm());
         }
      }

      if (matchCount == 0) {
         result.append("  (No matching instances found)\n");
      } else {
         result.append("\nTotal: ").append(matchCount).append(" match(es)\n");
      }

      return result.toString();
   }

   /**
    * Builds the system prompt for OpenAI to generate simple queries
    */
   private String buildSparqlSystemPrompt() {
      StringBuilder prompt = new StringBuilder();
      prompt.append("You are an ontology query analyzer. Parse the user's natural language question ");
      prompt.append(
            "and identify what they're asking about. You must handle complex queries with multiple conditions.\n\n");

      prompt.append("DO NOT answer the question yourself. Just identify the search criteria.\n");

      if (ontology != null) {
         prompt.append("Current Ontology: ").append(ontology.getOntologyID().getOntologyIRI().orNull()).append("\n\n");

         // Add ontology context
         prompt.append("Available Classes:\n");
         ontology.getClassesInSignature().stream()
               .filter(cls -> !cls.isOWLThing() && !cls.isOWLNothing())
               .limit(30)
               .forEach(cls -> prompt.append("  - ").append(cls.getIRI().getShortForm()).append("\n"));

         prompt.append("\nAvailable Object Properties:\n");
         ontology.getObjectPropertiesInSignature().stream()
               .limit(20)
               .forEach(prop -> prompt.append("  - ").append(prop.getIRI().getShortForm()).append("\n"));

         prompt.append("\nAvailable Individuals:\n");
         ontology.getIndividualsInSignature().stream()
               .limit(20)
               .forEach(ind -> prompt.append("  - ").append(ind.getIRI().getShortForm()).append("\n"));
      }

      prompt.append("\nYour response MUST be in this exact format:\n");
      prompt.append("QUERY_TYPE: [instances|classes|properties|relationships|individual]\n");
      prompt.append("TARGET: [search term]\n\n");

      return prompt.toString();
   }

   /**
    * Method to execute query based on parsed AI response
    */
   private String executeQueryOnOntology(String queryType, String target) {
      try {
         StringBuilder results = new StringBuilder();

         if (queryType == null || target == null || target.isEmpty()) {
            return "⚠️ Could not parse query. Please try rephrasing.";
         }

         // Execute based on query type and build answer from ontology
         switch (queryType.toLowerCase()) {
            case "instances":
               results.append(findInstancesWithDetails(target));
               break;
            case "classes":
               results.append(findClassesWithDetails(target));
               break;
            case "properties":
               results.append(findPropertiesWithDetails(target));
               break;
            case "relationships":
               results.append(findRelationshipsWithDetails(target));
               break;
            case "individual":
            case "entity":
               results.append(findIndividualDetails(target));
               break;
            default:
               results.append("⚠️ Unknown query type: " + queryType);
         }

         return results.toString();

      } catch (Exception e) {
         return "❌ Error: " + e.getMessage();
      }
   }

   /**
    * Highlights the target entity in the graph
    */
   private void highlightEntityInGraph(String entityName) {
      if (entityName == null || entityName.isEmpty()) {
         logger.warn("highlightEntityInGraph called with null or empty entityName");
         return;
      }

      logger.info("=== Highlighting Entity in Graph ===");
      logger.info("Searching for entity: '{}'", entityName);

      // Find the entity in the graph
      OWLEntity targetEntity = null;

      // Search in individuals first
      int individualCount = 0;
      for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
         individualCount++;
         if (ind.getIRI().getShortForm().equalsIgnoreCase(entityName)) {
            targetEntity = ind;
            logger.info("Found as INDIVIDUAL: {}", ind.getIRI().getShortForm());
            break;
         }
      }
      logger.info("Searched {} individuals", individualCount);

      // If not found, search in classes
      if (targetEntity == null) {
         int classCount = 0;
         for (OWLClass cls : ontology.getClassesInSignature()) {
            classCount++;
            if (cls.getIRI().getShortForm().equalsIgnoreCase(entityName)) {
               targetEntity = cls;
               logger.info("Found as CLASS: {}", cls.getIRI().getShortForm());
               break;
            }
         }
         logger.info("Searched {} classes", classCount);
      }

      // If not found, search in properties
      if (targetEntity == null) {
         int propCount = 0;
         for (OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()) {
            propCount++;
            if (prop.getIRI().getShortForm().equalsIgnoreCase(entityName)) {
               targetEntity = prop;
               logger.info("Found as PROPERTY: {}", prop.getIRI().getShortForm());
               break;
            }
         }
         logger.info("Searched {} properties", propCount);
      }

      // Highlight the entity if found
      if (targetEntity != null) {
         final OWLEntity entity = targetEntity;

         logger.info("Entity found! Type: {}", entity.getClass().getSimpleName());

         // Add to graph if not already present
         if (!graph.containsVertex(entity)) {
            graph.addVertex(entity);
            logger.info("Added entity to graph (was not present)");
         } else {
            logger.info("Entity already in graph");
         }

         // Expand the entity to show its relationships
         expandEntityWithRelationships(entity);

         // Set as selected entity (this will trigger visual highlighting)
         selectedEntity = entity;
         logger.info("Set as selected entity");

         // Center the view on this entity
         centerViewOnEntity(entity);

         // Refresh the viewer to show the highlighting
         viewer.repaint();
         logger.info("Triggered viewer repaint");
      } else {
         logger.warn("Entity '{}' NOT FOUND in ontology!", entityName);
      }
   }

   /**
    * Expands an entity to show its relationships in the graph
    */
   private void expandEntityWithRelationships(OWLEntity entity) {
      logger.info("Expanding entity to show relationships: {}", entity.getIRI().getShortForm());

      // If it's an individual, show its class and relationships
      if (entity instanceof OWLNamedIndividual) {
         OWLNamedIndividual individual = (OWLNamedIndividual) entity;

         // Add its classes
         for (var axiom : ontology.getClassAssertionAxioms(individual)) {
            if (!axiom.getClassExpression().isAnonymous()) {
               OWLClass cls = axiom.getClassExpression().asOWLClass();
               if (!graph.containsVertex(cls)) {
                  graph.addVertex(cls);
                  logger.info("Added class: {}", cls.getIRI().getShortForm());
               }
               if (!graph.containsEdge(new OWLRelationship(individual, cls, "type"))) {
                  graph.addEdge(new OWLRelationship(individual, cls, "type"), individual, cls);
                  logger.info("Added type edge to: {}", cls.getIRI().getShortForm());
               }
            }
         }

         // Add related individuals via object properties
         int relationCount = 0;
         for (var axiom : ontology.getObjectPropertyAssertionAxioms(individual)) {
            OWLObjectProperty property = axiom.getProperty().asOWLObjectProperty();
            OWLNamedIndividual target = axiom.getObject().asOWLNamedIndividual();

            if (!graph.containsVertex(target)) {
               graph.addVertex(target);
               logger.info("Added related individual: {}", target.getIRI().getShortForm());
            }

            String propertyName = property.getIRI().getShortForm();
            OWLRelationship edge = new OWLRelationship(individual, target, propertyName);
            if (!graph.containsEdge(edge)) {
               graph.addEdge(edge, individual, target);
               logger.info("Added relationship: {} --{}-> {}",
                     individual.getIRI().getShortForm(), propertyName, target.getIRI().getShortForm());
               relationCount++;
            }
         }
         logger.info("Added {} relationships", relationCount);

         // Mark as expanded
         expandedNodes.add(entity);
      }
      // If it's a class, show instances and subclasses
      else if (entity instanceof OWLClass) {
         expandedNodes.add(entity);
         addChildrenToGraph(entity);
         logger.info("Expanded class to show subclasses and instances");
      }

      // Refresh layout
      currentLayout.initialize();
   }

   /**
    * Centers the view on the specified entity
    */
   private void centerViewOnEntity(OWLEntity entity) {
      try {
         // Get the entity's position in the layout
         java.awt.geom.Point2D entityPos = currentLayout.apply(entity);

         if (entityPos != null) {
            logger.info("Entity position: ({}, {})", entityPos.getX(), entityPos.getY());

            // Get the center of the viewer
            Dimension viewerSize = viewer.getSize();
            double centerX = viewerSize.width / 2.0;
            double centerY = viewerSize.height / 2.0;

            // Calculate the offset needed to center the entity
            double dx = centerX - entityPos.getX();
            double dy = centerY - entityPos.getY();

            logger.info("Centering entity - offset: ({}, {})", dx, dy);

            // Get the current render context transform and update it
            viewer.getRenderContext().getMultiLayerTransformer()
                  .getTransformer(edu.uci.ics.jung.visualization.Layer.LAYOUT)
                  .translate(dx, dy);

            logger.info("View centered on entity");
         } else {
            logger.warn("Could not get entity position for centering");
         }
      } catch (Exception e) {
         logger.error("Error centering view on entity", e);
      }
   }

   /**
    * Finds an individual and returns detailed information
    */
   private String findIndividualDetails(String individualName) {
      StringBuilder result = new StringBuilder();

      // Find the individual
      OWLNamedIndividual targetInd = ontology.getIndividualsInSignature().stream()
            .filter(ind -> ind.getIRI().getShortForm().equalsIgnoreCase(individualName))
            .findFirst().orElse(null);

      if (targetInd == null) {
         return "❌ Individual '" + individualName + "' not found in ontology.";
      }

      result.append("📋 Individual: ").append(individualName).append("\n\n");

      // Get types (classes)
      result.append("🏷️ Types:\n");
      int typeCount = 0;
      for (var axiom : ontology.getClassAssertionAxioms(targetInd)) {
         if (!axiom.getClassExpression().isAnonymous()) {
            typeCount++;
            OWLClass cls = axiom.getClassExpression().asOWLClass();
            result.append("  • ").append(cls.getIRI().getShortForm()).append("\n");
         }
      }
      if (typeCount == 0) {
         result.append("  (No types found)\n");
      }

      // Get relationships
      result.append("\n🔗 Relationships:\n");
      int relCount = 0;
      for (var axiom : ontology.getObjectPropertyAssertionAxioms(targetInd)) {
         relCount++;
         String property = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
         String object = axiom.getObject().asOWLNamedIndividual().getIRI().getShortForm();
         result.append("  • ").append(property).append(" → ").append(object).append("\n");
      }
      if (relCount == 0) {
         result.append("  (No relationships found)\n");
      }

      // Get data properties
      result.append("\n📊 Data Properties:\n");
      int dataCount = 0;
      for (var axiom : ontology.getDataPropertyAssertionAxioms(targetInd)) {
         dataCount++;
         String property = axiom.getProperty().asOWLDataProperty().getIRI().getShortForm();
         String value = axiom.getObject().getLiteral();
         result.append("  • ").append(property).append(" = ").append(value).append("\n");
      }
      if (dataCount == 0) {
         result.append("  (No data properties found)\n");
      }

      return result.toString();
   }

   private String findInstancesWithDetails(String className) {
      StringBuilder result = new StringBuilder();
      result.append("📦 Instances of '").append(className).append("':\n\n");

      OWLClass targetClass = ontology.getClassesInSignature().stream()
            .filter(cls -> cls.getIRI().getShortForm().equalsIgnoreCase(className))
            .findFirst().orElse(null);

      if (targetClass != null) {
         Set<OWLNamedIndividual> instances = ontology.getClassAssertionAxioms(targetClass).stream()
               .filter(ax -> ax.getIndividual().isNamed())
               .map(ax -> ax.getIndividual().asOWLNamedIndividual())
               .collect(Collectors.toSet());

         if (instances.isEmpty()) {
            result.append("  (No instances found)\n");
         } else {
            for (OWLNamedIndividual ind : instances) {
               result.append("  • ").append(ind.getIRI().getShortForm()).append("\n");
            }
            result.append("\nTotal: ").append(instances.size()).append(" instance(s)\n");
         }
      } else {
         result.append("❌ Class not found: ").append(className).append("\n");
      }

      return result.toString();
   }

   private String findClassesWithDetails(String pattern) {
      StringBuilder result = new StringBuilder();
      result.append("🗂️ Classes matching '").append(pattern).append("':\n\n");

      Set<OWLClass> matchingClasses = ontology.getClassesInSignature().stream()
            .filter(cls -> !cls.isOWLThing() && !cls.isOWLNothing())
            .filter(cls -> cls.getIRI().getShortForm().toLowerCase().contains(pattern.toLowerCase()))
            .collect(Collectors.toSet());

      if (matchingClasses.isEmpty()) {
         result.append("  (No classes found)\n");
      } else {
         for (OWLClass cls : matchingClasses) {
            result.append("  • ").append(cls.getIRI().getShortForm()).append("\n");
         }
         result.append("\nTotal: ").append(matchingClasses.size()).append(" class(es)\n");
      }

      return result.toString();
   }

   private String findPropertiesWithDetails(String pattern) {
      StringBuilder result = new StringBuilder();
      result.append("🔗 Object Properties matching '").append(pattern).append("':\n\n");

      Set<OWLObjectProperty> matchingProps = ontology.getObjectPropertiesInSignature().stream()
            .filter(prop -> pattern.isEmpty()
                  || prop.getIRI().getShortForm().toLowerCase().contains(pattern.toLowerCase()))
            .collect(Collectors.toSet());

      if (matchingProps.isEmpty()) {
         result.append("  (No properties found)\n");
      } else {
         for (OWLObjectProperty prop : matchingProps) {
            result.append("  • ").append(prop.getIRI().getShortForm()).append("\n");
         }
         result.append("\nTotal: ").append(matchingProps.size()).append(" propert(y/ies)\n");
      }

      return result.toString();
   }

   private String findRelationshipsWithDetails(String individualName) {
      StringBuilder result = new StringBuilder();
      result.append("🔗 Relationships for '").append(individualName).append("':\n\n");

      OWLNamedIndividual targetInd = ontology.getIndividualsInSignature().stream()
            .filter(ind -> ind.getIRI().getShortForm().equalsIgnoreCase(individualName))
            .findFirst().orElse(null);

      if (targetInd != null) {
         int count = 0;
         for (var axiom : ontology.getObjectPropertyAssertionAxioms(targetInd)) {
            count++;
            String property = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
            String object = axiom.getObject().asOWLNamedIndividual().getIRI().getShortForm();
            result.append("  • ").append(property).append(" → ").append(object).append("\n");
         }

         if (count == 0) {
            result.append("  (No relationships found)\n");
         } else {
            result.append("\nTotal: ").append(count).append(" relationship(s)\n");
         }
      } else {
         result.append("❌ Individual not found: ").append(individualName).append("\n");
      }

      return result.toString();
   }

   /**
    * Extracts query information from OpenAI response
    */
   private String extractSparqlQuery(String response) {
      // For the simplified approach, we just return the response
      return response != null ? response.trim() : "";
   }

   /**
    * Executes query on the ontology using OWL API
    */
   private String executeSparqlQuery(String aiResponse) {
      try {
         StringBuilder results = new StringBuilder();
         results.append("✅ Query Processed!\n\n");
         results.append("📊 Results:\n");
         results.append("─────────────────────────────────────\n\n");

         // Parse AI response to detect query type
         String queryType = extractQueryType(aiResponse);
         String target = extractTarget(aiResponse);

         if (queryType != null && target != null) {
            // Execute based on query type
            switch (queryType.toLowerCase()) {
               case "instances":
                  results.append(findInstances(target));
                  break;
               case "classes":
                  results.append(findClasses(target));
                  break;
               case "properties":
                  results.append(findProperties(target));
                  break;
               case "relationships":
                  results.append(findRelationships(target));
                  break;
               default:
                  results.append(aiResponse);
            }
         } else {
            // Just show AI's response
            results.append(aiResponse);
         }

         results.append("\n\n─────────────────────────────────────\n");
         return results.toString();

      } catch (Exception e) {
         return "❌ Error: " + e.getMessage();
      }
   }

   private String extractQueryType(String response) {
      if (response.contains("QUERY_TYPE:")) {
         String[] lines = response.split("\n");
         for (String line : lines) {
            if (line.startsWith("QUERY_TYPE:")) {
               return line.substring(11).trim();
            }
         }
      }
      return null;
   }

   private String extractTarget(String response) {
      if (response.contains("TARGET:")) {
         String[] lines = response.split("\n");
         for (String line : lines) {
            if (line.startsWith("TARGET:")) {
               return line.substring(7).trim();
            }
         }
      }
      return null;
   }

   private String findInstances(String className) {
      StringBuilder result = new StringBuilder();
      result.append("Instances of ").append(className).append(":\n\n");

      // Find the class
      OWLClass targetClass = ontology.getClassesInSignature().stream()
            .filter(cls -> cls.getIRI().getShortForm().equalsIgnoreCase(className))
            .findFirst().orElse(null);

      if (targetClass != null) {
         Set<OWLNamedIndividual> instances = ontology.getClassAssertionAxioms(targetClass).stream()
               .filter(ax -> ax.getIndividual().isNamed())
               .map(ax -> ax.getIndividual().asOWLNamedIndividual())
               .collect(Collectors.toSet());

         int count = 0;
         for (OWLNamedIndividual ind : instances) {
            count++;
            result.append("  ").append(count).append(". ").append(ind.getIRI().getShortForm()).append("\n");
         }

         if (count == 0) {
            result.append("  (No instances found)\n");
         } else {
            result.append("\nTotal: ").append(count).append(" instance(s)\n");
         }
      } else {
         result.append("  Class not found: ").append(className).append("\n");
      }

      return result.toString();
   }

   private String findClasses(String pattern) {
      StringBuilder result = new StringBuilder();
      result.append("Classes matching '").append(pattern).append("':\n\n");

      int count = 0;
      for (OWLClass cls : ontology.getClassesInSignature()) {
         if (!cls.isOWLThing() && !cls.isOWLNothing()) {
            String name = cls.getIRI().getShortForm();
            if (name.toLowerCase().contains(pattern.toLowerCase())) {
               count++;
               result.append("  ").append(count).append(". ").append(name).append("\n");
            }
         }
      }

      if (count == 0) {
         result.append("  (No classes found)\n");
      } else {
         result.append("\nTotal: ").append(count).append(" class(es)\n");
      }

      return result.toString();
   }

   private String findProperties(String pattern) {
      StringBuilder result = new StringBuilder();
      result.append("Object Properties matching '").append(pattern).append("':\n\n");

      int count = 0;
      for (OWLObjectProperty prop : ontology.getObjectPropertiesInSignature()) {
         String name = prop.getIRI().getShortForm();
         if (pattern.isEmpty() || name.toLowerCase().contains(pattern.toLowerCase())) {
            count++;
            result.append("  ").append(count).append(". ").append(name).append("\n");
         }
      }

      if (count == 0) {
         result.append("  (No properties found)\n");
      } else {
         result.append("\nTotal: ").append(count).append(" propert(y/ies)\n");
      }

      return result.toString();
   }

   private String findRelationships(String individualName) {
      StringBuilder result = new StringBuilder();
      result.append("Relationships for '").append(individualName).append("':\n\n");

      // Find the individual
      OWLNamedIndividual targetInd = ontology.getIndividualsInSignature().stream()
            .filter(ind -> ind.getIRI().getShortForm().equalsIgnoreCase(individualName))
            .findFirst().orElse(null);

      if (targetInd != null) {
         int count = 0;
         for (var axiom : ontology.getObjectPropertyAssertionAxioms(targetInd)) {
            count++;
            String property = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
            String object = axiom.getObject().asOWLNamedIndividual().getIRI().getShortForm();
            result.append("  ").append(count).append(". ").append(property)
                  .append(" → ").append(object).append("\n");
         }

         if (count == 0) {
            result.append("  (No relationships found)\n");
         } else {
            result.append("\nTotal: ").append(count).append(" relationship(s)\n");
         }
      } else {
         result.append("  Individual not found: ").append(individualName).append("\n");
      }

      return result.toString();
   }

   private void changeLayout(Layout<OWLEntity, OWLRelationship> newLayout) {
      Dimension size = viewer.getSize();
      if (size.width == 0 || size.height == 0) {
         size = new Dimension(800, 600);
      }
      newLayout.setSize(size);
      currentLayout = newLayout;
      viewer.setGraphLayout(newLayout);
      viewer.repaint();
   }

   public void setSelectedEntity(OWLEntity entity) {
      this.selectedEntity = entity;
      viewer.repaint();
   }

   public void refreshGraph() {
      // Create a new graph instead of trying to clear the unmodifiable collections
      graph = new DirectedSparseGraph<>();
      expandedNodes.clear();
      selectedEntity = null;

      // Rebuild from ontology
      buildGraphFromOntology();

      // Update the layout with the new graph
      currentLayout = new FRLayout<>(graph);
      Dimension size = viewer.getSize();
      if (size.width == 0 || size.height == 0) {
         size = new Dimension(800, 600);
      }
      currentLayout.setSize(size);

      // Update the viewer with the new layout
      viewer.setGraphLayout(currentLayout);

      logger.info("Graph refreshed - {} vertices, {} edges",
            graph.getVertexCount(), graph.getEdgeCount());

      viewer.repaint();
   }

   /**
    * Explains the ontology by sending the full OWL content to the LLM
    * and displaying a bullet-point summary
    */
   private void explainOntology() {
      if (ontology == null) {
         resultsArea.setText("No ontology loaded.");
         return;
      }

      SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
         @Override
         protected String doInBackground() throws Exception {
            resultsArea.setText("Generating ontology explanation...\n\nPlease wait, analyzing ontology structure...");

            try {
               // Get the ontology as OWL functional syntax
               OWLOntologyManager manager = ontology.getOWLOntologyManager();
               StringDocumentTarget documentTarget = new StringDocumentTarget();
               manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), documentTarget);
               String owlContent = documentTarget.toString();

               logger.info("Ontology serialized - {} characters", owlContent.length());

               // Truncate if too large (keep first ~15000 chars to stay within token limits)
               if (owlContent.length() > 15000) {
                  owlContent = owlContent.substring(0, 15000) + "\n\n... (truncated for length)";
                  logger.info("Ontology content truncated to 15000 characters");
               }

               // Build prompt for LLM
               String systemPrompt = "You are an expert in ontology analysis and OWL (Web Ontology Language). " +
                     "Analyze the provided ontology and create a clear, concise explanation. " +
                     "Format your response as bullet points describing:\n" +
                     "- The main purpose/domain of this ontology\n" +
                     "- Key classes and their relationships\n" +
                     "- Important properties\n" +
                     "- Notable individuals (if any)\n" +
                     "- Overall structure and organization\n\n" +
                     "Keep it clear and accessible, avoiding overly technical jargon where possible.";

               String userMessage = "Please analyze this OWL ontology and explain what it represents:\n\n" + owlContent;

               // Get API configuration
               String apiKey = VidyaastraPreferences.getOpenAiApiKey();
               String model = VidyaastraPreferences.getOpenAiModel();
               String baseUrl = VidyaastraPreferences.getOpenAiBaseUrl();

               if (apiKey == null || apiKey.trim().isEmpty()) {
                  return "ERROR: OpenAI API key not configured. Please set it in Preferences.";
               }
               if (model == null || model.trim().isEmpty()) {
                  model = "gpt-4o-mini"; // Default model
               }
               if (baseUrl == null || baseUrl.trim().isEmpty()) {
                  baseUrl = "https://api.openai.com/v1"; // Default base URL
               }

               // Call OpenAI
               OpenAiCaller caller = new OpenAiCaller(apiKey, model, baseUrl);
               String response = caller.generateCompletion(systemPrompt, userMessage);

               logger.info("Received ontology explanation from LLM - {} characters", response.length());

               return response;

            } catch (OWLOntologyStorageException e) {
               logger.error("Failed to serialize ontology", e);
               return "ERROR: Failed to serialize ontology: " + e.getMessage();
            } catch (Exception e) {
               logger.error("Failed to explain ontology", e);
               return "ERROR: Failed to get explanation from LLM: " + e.getMessage();
            }
         }

         @Override
         protected void done() {
            try {
               String explanation = get();

               // Display the explanation
               resultsArea.setText("=== ONTOLOGY EXPLANATION ===\n\n" + explanation);
               resultsArea.setCaretPosition(0); // Scroll to top

            } catch (Exception e) {
               logger.error("Error displaying ontology explanation", e);
               resultsArea.setText("ERROR: " + e.getMessage());
            }
         }
      };

      worker.execute();
   }

   public void dispose() {
      // Cleanup resources
      if (viewer != null) {
         viewer.setGraphLayout(null);
      }
      graph = null;
   }

   private void exportResults() {
      String content = resultsArea.getText();
      if (content.isEmpty() || content.startsWith("💡")) {
         JOptionPane.showMessageDialog(this, "Nothing to export.");
         return;
      }

      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Export Query Results");
      fileChooser.setSelectedFile(new File("vidyaastra-graph-results.txt"));

      int userSelection = fileChooser.showSaveDialog(this);
      if (userSelection == JFileChooser.APPROVE_OPTION) {
         File fileToSave = fileChooser.getSelectedFile();
         try (FileWriter writer = new FileWriter(fileToSave)) {
            writer.write(content);
            JOptionPane.showMessageDialog(this, "Results saved to " + fileToSave.getAbsolutePath());
         } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error",
                  JOptionPane.ERROR_MESSAGE);
         }
      }
   }

   /**
    * Inner class to represent relationships between OWL entities
    */
   public static class OWLRelationship {
      private final OWLEntity source;
      private final OWLEntity target;
      private final String relationType;

      public OWLRelationship(OWLEntity source, OWLEntity target, String relationType) {
         this.source = source;
         this.target = target;
         this.relationType = relationType;
      }

      public OWLEntity getSource() {
         return source;
      }

      public OWLEntity getTarget() {
         return target;
      }

      public String getRelationType() {
         return relationType;
      }

      @Override
      public String toString() {
         return relationType;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((source == null) ? 0 : source.hashCode());
         result = prime * result + ((target == null) ? 0 : target.hashCode());
         result = prime * result + ((relationType == null) ? 0 : relationType.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         OWLRelationship other = (OWLRelationship) obj;
         if (source == null) {
            if (other.source != null)
               return false;
         } else if (!source.equals(other.source))
            return false;
         if (target == null) {
            if (other.target != null)
               return false;
         } else if (!target.equals(other.target))
            return false;
         if (relationType == null) {
            if (other.relationType != null)
               return false;
         } else if (!relationType.equals(other.relationType))
            return false;
         return true;
      }
   }
}

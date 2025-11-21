package org.vidyaastra.ui.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JButton;

import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

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
public class VidyaastraGraphPanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   private OWLModelManager modelManager;
   private Graph<OWLEntity, OWLRelationship> graph;
   private VisualizationViewer<OWLEntity, OWLRelationship> viewer;
   private OWLEntity selectedEntity;
   private Layout<OWLEntity, OWLRelationship> currentLayout;
   private DefaultModalGraphMouse<OWLEntity, OWLRelationship> graphMouse;
   private Set<OWLEntity> expandedNodes;
   private OWLOntology ontology;

   public VidyaastraGraphPanel(OWLModelManager modelManager)
   {
      this.modelManager = modelManager;
      this.expandedNodes = new HashSet<>();
      setLayout(new BorderLayout());

      initializeGraph();
      createViewer();
      createToolbar();
   }

   private void initializeGraph()
   {
      graph = new DirectedSparseGraph<>();
      buildGraphFromOntology();
   }

   private void buildGraphFromOntology()
   {
      if (modelManager == null) return;

      ontology = modelManager.getActiveOntology();
      if (ontology == null) return;

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
   
   private void addChildrenToGraph(OWLEntity parent)
   {
      if (!(parent instanceof OWLClass)) return;
      if (!expandedNodes.contains(parent)) return;
      
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
      Set<org.semanticweb.owlapi.model.OWLNamedIndividual> instances = ontology.getAxioms(org.semanticweb.owlapi.model.AxiomType.CLASS_ASSERTION)
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
   
   public void toggleNodeExpansion(OWLEntity entity)
   {
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
   
   private void removeDescendants(OWLEntity parent)
   {
      if (!(parent instanceof OWLClass)) return;
      
      OWLClass parentClass = (OWLClass) parent;
      
      // Find and remove all descendants (subclasses and instances)
      Set<OWLEntity> toRemove = new HashSet<>();
      for (OWLEntity vertex : new HashSet<>(graph.getVertices())) {
         // Remove subclasses
         if (vertex instanceof OWLClass && isDescendantOf((OWLClass)vertex, parentClass)) {
            toRemove.add(vertex);
            expandedNodes.remove(vertex);
         }
         // Remove instances of this class
         else if (vertex instanceof OWLNamedIndividual) {
            boolean isInstanceOfParent = ontology.getClassAssertionAxioms((OWLNamedIndividual)vertex)
                  .stream()
                  .anyMatch(ax -> !ax.getClassExpression().isAnonymous() && 
                                 ax.getClassExpression().asOWLClass().equals(parentClass));
            if (isInstanceOfParent) {
               toRemove.add(vertex);
            }
         }
      }
      
      for (OWLEntity entity : toRemove) {
         graph.removeVertex(entity);
      }
   }
   
   private boolean isDescendantOf(OWLClass child, OWLClass ancestor)
   {
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
   
   private boolean hasChildren(OWLClass parentClass)
   {
      if (ontology == null) return false;
      
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

   private void createViewer()
   {
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

      add(new JScrollPane(viewer), BorderLayout.CENTER);
   }

   private void setupNodeRenderer()
   {
      // Custom vertex label with expand/collapse indicator
      viewer.getRenderContext().setVertexLabelTransformer(entity -> {
         String label = entity.getIRI().getShortForm();
         
         // Add indicator for expandable nodes
         if (entity instanceof OWLClass && hasChildren((OWLClass)entity)) {
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
            return new Ellipse2D.Double(-diameter/2.0, -diameter/2.0, diameter, diameter);
         } else if (entity instanceof OWLObjectProperty) {
            // Rectangles that expand to fit text
            int width = Math.max(80, textLength * 8);
            int height = 50;
            return new Rectangle2D.Double(-width/2.0, -height/2.0, width, height);
         } else if (entity instanceof OWLNamedIndividual) {
            // Hexagons/diamonds for individuals (using rectangles rotated visually via size)
            int size = Math.max(50, Math.min(textLength * 6, 120));
            return new Rectangle2D.Double(-size/2.0, -size/2.0, size, size);
         }
         int diameter = Math.max(60, Math.min(textLength * 7, 150));
         return new Ellipse2D.Double(-diameter/2.0, -diameter/2.0, diameter, diameter);
      });

      // Professional gradient-like colors
      viewer.getRenderContext().setVertexFillPaintTransformer(entity -> {
         if (entity.equals(selectedEntity)) {
            return new Color(255, 215, 0); // Gold for selected
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
            return new Color(255, 69, 0); // Orange red for selected
         }
         return new Color(47, 79, 79); // Dark slate gray
      });

      // Custom vertex stroke (border thickness)
      viewer.getRenderContext().setVertexStrokeTransformer(entity -> {
         if (entity.equals(selectedEntity)) {
            return new BasicStroke(3.0f); // Thicker border for selected
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
            return new Font("SansSerif", Font.BOLD, fontSize + 1);
         }
         return new Font("SansSerif", Font.BOLD, fontSize);
      });
   }

   private void setupEdgeRenderer()
   {
      // Don't show edge labels by default (cleaner look)
      viewer.getRenderContext().setEdgeLabelTransformer(rel -> ""); // Empty for cleaner view

      // Professional edge color
      viewer.getRenderContext().setEdgeDrawPaintTransformer(rel -> new Color(105, 105, 105)); // Dim gray

      // Thicker, more visible edge stroke with arrow
      viewer.getRenderContext().setEdgeStrokeTransformer(rel -> {
         float[] dash = {10.0f, 5.0f};
         return new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                               10.0f, dash, 0.0f); // Dashed line for hierarchy
      });

      // Show directional arrows for edges
      viewer.getRenderContext().setEdgeArrowPredicate(edge -> true);
      
      // Larger arrow size
      viewer.getRenderContext().setEdgeArrowStrokeTransformer(edge -> new BasicStroke(1.5f));
   }

   private void setupMouseControl()
   {
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

   private void createToolbar()
   {
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

      add(toolbar, BorderLayout.NORTH);
   }

   private void changeLayout(Layout<OWLEntity, OWLRelationship> newLayout)
   {
      Dimension size = viewer.getSize();
      if (size.width == 0 || size.height == 0) {
         size = new Dimension(800, 600);
      }
      newLayout.setSize(size);
      currentLayout = newLayout;
      viewer.setGraphLayout(newLayout);
      viewer.repaint();
   }

   public void setSelectedEntity(OWLEntity entity)
   {
      this.selectedEntity = entity;
      viewer.repaint();
   }

   public void refreshGraph()
   {
      graph.getVertices().clear();
      graph.getEdges().clear();
      buildGraphFromOntology();
      
      // Reinitialize the layout to properly position nodes
      Dimension size = viewer.getSize();
      if (size.width == 0 || size.height == 0) {
         size = new Dimension(800, 600);
      }
      currentLayout.setSize(size);
      currentLayout.initialize();
      
      viewer.repaint();
   }

   public void dispose()
   {
      // Cleanup resources
      if (viewer != null) {
         viewer.setGraphLayout(null);
      }
      graph = null;
   }

   /**
    * Inner class to represent relationships between OWL entities
    */
   public static class OWLRelationship
   {
      private final OWLEntity source;
      private final OWLEntity target;
      private final String relationType;

      public OWLRelationship(OWLEntity source, OWLEntity target, String relationType)
      {
         this.source = source;
         this.target = target;
         this.relationType = relationType;
      }

      public OWLEntity getSource()
      {
         return source;
      }

      public OWLEntity getTarget()
      {
         return target;
      }

      public String getRelationType()
      {
         return relationType;
      }

      @Override
      public String toString()
      {
         return relationType;
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((source == null) ? 0 : source.hashCode());
         result = prime * result + ((target == null) ? 0 : target.hashCode());
         result = prime * result + ((relationType == null) ? 0 : relationType.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         OWLRelationship other = (OWLRelationship) obj;
         if (source == null) {
            if (other.source != null) return false;
         } else if (!source.equals(other.source)) return false;
         if (target == null) {
            if (other.target != null) return false;
         } else if (!target.equals(other.target)) return false;
         if (relationType == null) {
            if (other.relationType != null) return false;
         } else if (!relationType.equals(other.relationType)) return false;
         return true;
      }
   }
}

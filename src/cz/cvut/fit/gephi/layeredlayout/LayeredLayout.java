package cz.cvut.fit.gephi.layeredlayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Kuchar
 */
public class LayeredLayout implements Layout {

    // private attributes
    private final LayoutBuilder layoutBuilder;
    private GraphModel graphModel;
    private Graph graph;
    private boolean converged;
    private String selectedColumn = null;
    private double layerDistance = 50;
    private boolean adjust = false;
    //private double previousRadius = 0;
    //private double previousOriginalRadius = 0;

    public LayeredLayout(LayoutBuilder layoutBuilder) {
        // inject builder
        this.layoutBuilder = layoutBuilder;
    }

    public LayoutBuilder getLayoutBuilder() {
        return layoutBuilder;
    }

    @Override
    public void initAlgo() {
        // get visible graf
        this.graph = graphModel.getGraphVisible();
        // allow start layout
        setConverged(false);
    }

    @Override
    public void setGraphModel(GraphModel graphModel) {
        this.graphModel = graphModel;
        graph = graphModel.getHierarchicalDirectedGraphVisible();
    }

    @Override
    public void goAlgo() {
        //graph = graphModel.getGraphVisible();
        graph = graphModel.getHierarchicalDirectedGraphVisible();
        // get column by name
        AttributeTable at = Lookup.getDefault().lookup(AttributeController.class).getModel().getNodeTable();
        AttributeColumn column = at.getColumn(selectedColumn);
        // get all nodes
        Node[] nodes = graph.getNodes().toArray();
        // if no nodes -> return
        if (nodes.length <= 0 || selectedColumn == null) {
            setConverged(true);
            return;
        }
        // sort by column value
        Arrays.sort(nodes, new NodeByColumnComparator(column));

        // init values
        // value of the first node
        double startValue = getValue(nodes[0], column);
        // position of the first node
        float startX = nodes[0].getNodeData().x();
        float startY = nodes[0].getNodeData().y();
        // init values for cycle
        // to identify change of value -> next orbit
        double currentValue = startValue;
        // if is more than one node in first layer shift to next layer
        boolean isFirstlayer = true;
        int shiftFirstlayer = 0;

        // nodes to render on the current orbit
        Set<Node> currentOrbit = new HashSet<Node>();

        for (Node n : nodes) {
            // change to new orbit ?
            if (getValue(n, column) != currentValue) {
                // if is more than one node in first layer shift to next layer
                if (isFirstlayer && currentOrbit.size() > 1) {
                    shiftFirstlayer = 1;
                }
                // all other layers are not first
                isFirstlayer = false;
                // render all nodes to current orbit
                renderOrbit(currentOrbit, startX, startY, shiftFirstlayer + (currentValue - startValue));
                // clear set
                currentOrbit.clear();
                // init values
                currentValue = getValue(n, column);
            }
            // add to current orbit
            currentOrbit.add(n);
        }
        // last orbit
        if (currentOrbit.size() > 0) {
            renderOrbit(currentOrbit, startX, startY, shiftFirstlayer + (currentValue - startValue));
        }
        // stop layout
        setConverged(true);
    }

    private void renderOrbit(Set<Node> currentOrbit, float startX, float startY, double radius) {

        // adjust by sizes                
        if (adjust) {
            double averageSize = 0;
            double computedRadius = 0;
            double length = 0;
            for (Node n : currentOrbit) {
                length += n.getNodeData().getSize();
            }
            averageSize = length / currentOrbit.size();
            //computedRadius = length / (2 * Math.PI);
            //System.out.println("radius: " + radius);
            //System.out.println("computed radius: " + computedRadius);
            //System.out.println("previous radius: " + previousRadius);

            /*
            double workingRadius = Math.max(previousRadius, computedRadius);
            workingRadius += (radius - previousOriginalRadius);
            previousOriginalRadius = radius;
            previousRadius = workingRadius;
            radius = workingRadius;
            System.out.println(radius);
             * 
             */
            /*
            if (computedRadius < previousRadius) {
            //radius = previousRadius + Math.abs(radius - previousRadius);                
            radius = previousRadius + 1;
            } else {
            radius = computedRadius;
            }
            previousRadius = radius;
             * 
             */
            double currentAngle = 0;
            // steady positioning on orbit
            double shift = 360 / length;
            int i = 0;
            for (Node o : currentOrbit) {
                // next position on the orbit
                currentAngle += (shift * o.getNodeData().getSize())/2;
                // noise
                double noise = 0;
                if (i % 3 == 1) {
                    noise = o.getNodeData().getSize() * (-1);
                }
                if (i % 3 == 2) {
                    noise = o.getNodeData().getSize();
                }
                // equation of circle
                // x = x0 + r*cos(a)
                // y = y0 + r*sin(a)
                float x = startX + (float) (((layerDistance * radius) + noise) * Math.cos(currentAngle * (Math.PI / 180)));
                float y = startY + (float) (((layerDistance * radius) + noise) * Math.sin(currentAngle * (Math.PI / 180)));
                // set position
                o.getNodeData().setX(x);
                o.getNodeData().setY(y);
                // next
                currentAngle += (shift * o.getNodeData().getSize())/2;
                i++;
            }

        } else {

            int currentAngle = 0;
            // steady positioning on orbit
            int shift = 360 / currentOrbit.size();
            for (Node o : currentOrbit) {
                // equation of circle
                // x = x0 + r*cos(a)
                // y = y0 + r*sin(a)
                float x = startX + (float) (layerDistance * (radius) * Math.cos(currentAngle * (Math.PI / 180)));
                float y = startY + (float) (layerDistance * (radius) * Math.sin(currentAngle * (Math.PI / 180)));
                // set position
                o.getNodeData().setX(x);
                o.getNodeData().setY(y);
                // next position on the orbit
                currentAngle += shift;
            }
        }
    }

    /**
     * get value of column from node attributes
     * @param node node
     * @param column column
     * @return
     */
    private Double getValue(Node node, AttributeColumn column) {
        //return (Integer) ((AttributeRow) (node.getNodeData().getAttributes())).getValue(column);
        return Double.parseDouble((((AttributeRow) (node.getNodeData().getAttributes())).getValue(column)).toString());
    }

    @Override
    public boolean canAlgo() {
        // if isset to start and not null
        return !isConverged() && graphModel != null && selectedColumn != null;
    }

    @Override
    public void endAlgo() {
        //this.previousOriginalRadius = 0;
        //this.previousRadius = 0;
    }

    @Override
    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        try {
            properties.add(LayoutProperty.createProperty(
                    this,
                    String.class,
                    "Attribute",
                    null,
                    "select attribute with distance value",
                    "getColumn",
                    "setColumn",
                    ColumnComboBoxEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this,
                    Double.class,
                    "Layer Distance",
                    null,
                    "Distance between each layer",
                    "getLayerDistance",
                    "setLayerDistance"));
            properties.add(LayoutProperty.createProperty(
                    this,
                    Boolean.class,
                    "Adjust",
                    null,
                    "Adjust by size",
                    "isAdjust",
                    "setAdjust"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties.toArray(new LayoutProperty[0]);
    }

    @Override
    public void resetPropertiesValues() {
        selectedColumn = null;
        layerDistance = 50;
        adjust = false;
    }

    @Override
    public LayoutBuilder getBuilder() {
        return layoutBuilder;
    }

    public void setConverged(boolean converged) {
        this.converged = converged;
    }

    public boolean isConverged() {
        return converged;
    }

    public String getColumn() {
        return this.selectedColumn;
    }

    public void setColumn(String column) {
        this.selectedColumn = column;
    }

    public Double getLayerDistance() {
        return layerDistance;
    }

    public void setLayerDistance(Double layerDistance) {
        this.layerDistance = layerDistance;
    }

    public Boolean isAdjust() {
        return adjust;
    }

    public void setAdjust(Boolean adjust) {
        this.adjust = adjust;
    }     

    public static Map getColumnEnumMap() {
        // get all attributes
        AttributeTable at = Lookup.getDefault().lookup(AttributeController.class).getModel().getNodeTable();
        Map<String, String> map = new HashMap<String, String>();
        map.put(null, "---");
        for (AttributeColumn c : at.getColumns()) {
            // only int and double type of attributes are permited            
            if (c.getType().equals(AttributeType.DOUBLE) || c.getType().equals(AttributeType.INT)) {
                map.put(c.getId(), c.getTitle());
            }
        }
        return map;
    }
}

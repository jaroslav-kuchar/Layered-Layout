package cz.cvut.fit.gephi.layeredlayout;

import java.util.Comparator;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.graph.api.Node;

/**
 * Comparator to sort nodes based on the column value
 * @author Jaroslav Kuchar
 */
public class NodeByColumnComparator implements Comparator<Node> {

    private AttributeColumn column;

    public NodeByColumnComparator(AttributeColumn column) {
        this.column = column;
    }

    @Override
    public int compare(Node o1, Node o2) {
        // get column values as double
        Double v1 = Double.parseDouble((((AttributeRow) (o1.getNodeData().getAttributes())).getValue(column)).toString());
        Double v2 = Double.parseDouble((((AttributeRow) (o2.getNodeData().getAttributes())).getValue(column)).toString());
        //Double v2 = (Integer) ((AttributeRow) (o2.getNodeData().getAttributes())).getValue(column);
        // compare
        return v1.compareTo(v2);
    }
}

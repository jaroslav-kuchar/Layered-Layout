package cz.cvut.fit.gephi.layeredlayout;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jaroslav Kuchar
 */
@ServiceProvider(service = LayoutBuilder.class)
public class LayeredLayoutBuilder implements LayoutBuilder {

    @Override
    public String getName() {
        return "Layered Layout";
    }

    @Override
    public LayoutUI getUI() {
        return new LayoutUI() {

            @Override
            public String getDescription() {
                return "The nodes are positioned in layers. Distance of the layer is dependant on the selected attribute.";
            }

            @Override
            public Icon getIcon() {
                return null;
            }

            @Override
            public JPanel getSimplePanel(Layout layout) {
                return null;
            }

            @Override
            public int getQualityRank() {
                return -1;
            }

            @Override
            public int getSpeedRank() {
                return -1;
            }
        };
    }

    @Override
    public Layout buildLayout() {
        return new LayeredLayout(this);
    }
}

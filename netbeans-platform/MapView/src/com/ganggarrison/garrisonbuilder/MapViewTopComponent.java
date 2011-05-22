
package com.ganggarrison.garrisonbuilder;

import com.ganggarrison.garrisonbuilder.gamemap.Entity;
import com.ganggarrison.garrisonbuilder.gamemap.GameMap;
import com.ganggarrison.garrisonbuilder.gamemap.EntityType;
import com.ganggarrison.garrisonbuilder.gamemap.GameMapChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.model.ObjectScene;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.spi.palette.PaletteController;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

/**
 * Editor TopComponent for a GameMap.  This is the primary window pane where
 * users will see and modify the map.
 * @author cspotcode
 */
public class MapViewTopComponent extends TopComponent implements PropertyChangeListener, GameMapChangeListener {

    private ObjectScene scene = null;
    private LayerWidget guiVisualsLayer;
    private LayerWidget bgWmLayer;

    final GameMap map;

    private ImageWidget backgroundWidget;
    private ImageWidget walkmaskWidget;

    public MapViewTopComponent(GameMap map) {
        this.map = map;

        setDisplayName("GG2 Map");
        setName("NAME");
        setToolTipText("TOOL TIP TEXT");

        // create scene to visualize the map
        scene = new ObjectScene();
        guiVisualsLayer = new LayerWidget(scene);
        scene.addChild(guiVisualsLayer);
        bgWmLayer = new LayerWidget(scene);
        scene.addChild(bgWmLayer); 
        bgWmLayer.bringToBack();

        // create a scrolling view onto the scene
        setLayout(new BorderLayout());
        JComponent view = scene.createView();
        JScrollPane scrollPane = new JScrollPane(view);
        add(scrollPane, BorderLayout.CENTER);
        
        // add background and walkmask widgets
        backgroundWidget = new ImageWidget(scene);
        walkmaskWidget = new ImageWidget(scene);
        backgroundWidget.setPreferredLocation(new Point(0, 0));
        walkmaskWidget.setPreferredLocation(new Point(0, 0));
        bgWmLayer.addChild(backgroundWidget);
        bgWmLayer.addChild(walkmaskWidget);

        // TODO get the scene to size itself to the map correctly

        // allow dragging a rectangle to select entities
        scene.getActions().addAction(ActionFactory.createRectangularSelectAction(scene, guiVisualsLayer));

        // add zooming support (hold CTRL and use mouse wheel)
        scene.getActions().addAction(ActionFactory.createZoomAction(2, true));
        
        // TODO panning doesn't seem to be working (supposed to hold down mouse wheel)
        scene.getActions().addAction(ActionFactory.createPanAction());

        // allows dropping of palette entities onto the map
        scene.getActions().addAction(ActionFactory.createAcceptAction(new MyAcceptProvider()));

        associateLookup(Lookups.fixed(new Object[] {getPalette(), map}));

        map.addPropertyChangeListener(this);
        map.addGameMapChangeListener(this);
    }

    @Override
    protected String preferredID() {
        return "GameMapEditorPane";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("width")) {
            Dimension d = scene.getPreferredSize();
            d.width = (Integer)evt.getNewValue();
            scene.setPreferredSize(d);
            return;
        }
        if(evt.getPropertyName().equals("backgroundImage")) {
            Image i = (Image)evt.getNewValue();
            backgroundWidget.setImage(i.getScaledInstance(i.getWidth(null) * 6, -1, 0));
            return;
        }
        if(evt.getPropertyName().equals("walkmaskImage")) {
            Image i = (Image)evt.getNewValue();
            walkmaskWidget.setImage(i.getScaledInstance(i.getWidth(null) * 6, -1, 0));
            return;
        }
    }

    private PaletteController getPalette() {
        return EntityTypeManager.getInstance().getPalette();
    }

    @Override
    public void entityAdded(GameMap m, Entity e) {
        EntityWidget w = new EntityWidget(scene, e);
        scene.addChild(w);
        scene.repaint(); // TODO shouldn't a repaint happen automatically because a widget has been added?
        scene.addObject(e, w);
    }

    @Override
    public void entityRemoved(GameMap m, Entity e) {
        Widget w = scene.findWidget(e); // TODO error-check this?
        scene.removeObject(e);
        scene.removeChild(w);
    }

    private class MyAcceptProvider implements AcceptProvider {

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            // is an EntityType being dragged?
            Object o = null;
            try {
                o = transferable.getTransferData(new DataFlavor(EntityType.class, "Entity Type"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (UnsupportedFlavorException ex) {
                // we couldn't get an EntityType, dropping won't work
                o = null;
            }

            return (o != null ? ConnectorState.ACCEPT : ConnectorState.REJECT);
        }

        @Override
        public void accept(Widget widget, Point point, Transferable transferable) {
            EntityType entType;
            try {
                entType = (EntityType) transferable.getTransferData(new DataFlavor(EntityType.class, "DOESN'T MATTER??"));
            } catch (UnsupportedFlavorException ex) {
                // do nothing, we couldn't get the flavor of data we wanted
                return;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return;
            }
            Point p = widget.convertLocalToScene(point);
            map.addEntity(new Entity(entType, p.x, p.y));
        }
    };
}

package storyflow.design;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plug-in activator for the StoryFlow Sirius design.
 * Registers the StoryFlow viewpoint on start, disposes it on stop.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "storyflow.design";

    private static Activator plugin;

    private static Set<Viewpoint> viewpoints;

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        viewpoints = new HashSet<Viewpoint>();
        viewpoints.addAll(ViewpointRegistry.getInstance()
                .registerFromPlugin(PLUGIN_ID + "/description/storyflow.odesign"));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        if (viewpoints != null) {
            for (final Viewpoint viewpoint : viewpoints) {
                ViewpointRegistry.getInstance().disposeFromPlugin(viewpoint);
            }
            viewpoints.clear();
            viewpoints = null;
        }
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }
}

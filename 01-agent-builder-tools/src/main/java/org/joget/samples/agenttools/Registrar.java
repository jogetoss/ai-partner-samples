package org.joget.samples.agenttools;

import java.util.Collection;
import org.joget.plugin.property.model.PropertyEditable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers each palette element as an OSGi service under its own class name. Agent Builder does not scan bundles:
 * it asks the Joget plugin manager for AgentTool / AgentGuard / AgentEnhancer implementations to fill the palette,
 * and rebuilds a saved element with getPlugin(className). The class name is therefore the element's identity.
 */
final class Registrar {

    private Registrar() {
    }

    static void register(BundleContext context, Collection<ServiceRegistration> sink) {
        add(context, sink, new IndonesianIdValidatorTool());
        add(context, sink, new NikPiiGuard());
        add(context, sink, new NpwpFormatEnhancer());
    }

    private static void add(BundleContext context, Collection<ServiceRegistration> sink, PropertyEditable plugin) {
        sink.add(context.registerService(plugin.getClassName(), plugin, null));
    }
}

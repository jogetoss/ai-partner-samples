package org.joget.samples.checklist;

import java.util.Collection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers the composer and its tool as OSGi services, each under its own class name. The tool must be registered
 * too: the run creates it through the Joget PluginManager.
 */
final class Registrar {

    private Registrar() {
    }

    static void register(BundleContext context, Collection<ServiceRegistration> sink) {
        sink.add(context.registerService(ChecklistComposerChat.class.getName(), new ChecklistComposerChat(), null));
        sink.add(context.registerService(ChecklistComposerAgentTool.class.getName(), new ChecklistComposerAgentTool(), null));
    }
}

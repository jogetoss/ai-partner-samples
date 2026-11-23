package org.joget.samples.composertool;

import java.util.Collection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers the tool as an OSGi service under its own class name. Agent Builder does not scan bundles: it asks the Joget
 * PluginManager for the class name stored in a palette element or in a tool template. A tool that is not registered
 * here is "plugin not found" when a skill tries to inject its template.
 */
final class Registrar {

    private Registrar() {
    }

    static void register(BundleContext context, Collection<ServiceRegistration> sink) {
        AcmeStandardsTool tool = new AcmeStandardsTool();
        sink.add(context.registerService(tool.getClassName(), tool, null));
    }
}

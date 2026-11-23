package org.joget.samples.systemagent;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.commons.util.LogUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Registers the console page (the trigger) and the tool (the agent's only capability). */
public class Activator implements BundleActivator {

    private final Collection<ServiceRegistration> registrations = new ArrayList<>();

    @Override
    public void start(BundleContext context) {
        registrations.add(context.registerService(NamingAuditorPage.class.getName(), new NamingAuditorPage(), null));
        try {
            // Agent Builder rebuilds every tool of a run by class name, so the tool must be a registered plugin.
            registrations.add(context.registerService(NamingCheckTool.class.getName(), new NamingCheckTool(), null));
        } catch (LinkageError e) {
            LogUtil.warn(Activator.class.getName(), "Agent Builder is not available, the auditor tool was not registered: " + e);
        }
    }

    @Override
    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrations) {
            try {
                registration.unregister();
            } catch (IllegalStateException e) {
                // already unregistered by the framework
            }
        }
        registrations.clear();
    }
}

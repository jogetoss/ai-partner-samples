package org.joget.samples.composertool;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.commons.util.LogUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Bundle entry point. The Activator names no Agent Builder type itself; that happens only in {@link Registrar}.
 * A missing Agent Builder therefore shows up as a caught LinkageError here and not as a bundle that fails to start.
 */
public class Activator implements BundleActivator {

    /** Any class of the Agent Builder API will do as an "is it installed" probe. */
    static final String AGENT_BUILDER_MARKER = "org.joget.ai.agent.model.AgentToolAbstract";

    private final Collection<ServiceRegistration> registrations = new ArrayList<>();

    @Override
    public void start(BundleContext context) {
        if (!agentBuilderPresent()) {
            LogUtil.info(Activator.class.getName(), "Agent Builder is not installed; the Acme standards tool is not registered");
            return;
        }
        try {
            Registrar.register(context, registrations);
        } catch (LinkageError e) {
            LogUtil.warn(Activator.class.getName(), "Agent Builder classes could not be linked: " + e);
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

    static boolean agentBuilderPresent() {
        try {
            Class.forName(AGENT_BUILDER_MARKER, false, Activator.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}

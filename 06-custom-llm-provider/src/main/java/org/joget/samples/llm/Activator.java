package org.joget.samples.llm;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.commons.util.LogUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Bundle entry point. It names no Agent Builder type itself: those are touched only inside {@link Registrar}, so a
 * missing Agent Builder is a caught LinkageError here instead of a bundle that fails to start.
 */
public class Activator implements BundleActivator {

    private static final String AGENT_BUILDER_MARKER = "org.joget.ai.agent.model.AgentLLMAbstract";
    private final Collection<ServiceRegistration> registrations = new ArrayList<>();

    @Override
    public void start(BundleContext context) {
        try {
            Class.forName(AGENT_BUILDER_MARKER, false, Activator.class.getClassLoader());
            Registrar.register(context, registrations);
            LogUtil.info(Activator.class.getName(), "Private Gateway LLM provider registered: " + registrations.size() + " plugins");
        } catch (ClassNotFoundException | LinkageError e) {
            LogUtil.warn(Activator.class.getName(), "Agent Builder is not available, nothing registered: " + e);
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
        try {
            Registrar.unregister();
        } catch (LinkageError e) {
            // Agent Builder is not there any more: nothing to clear
        }
    }
}

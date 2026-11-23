package org.joget.samples.llm;

import java.util.Collection;
import org.joget.ai.agent.model.AgentComponent;
import org.joget.ai.agent.model.AgentLLMAbstract;
import org.joget.ai.config.llm.LLMModelCapabilityRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers the provider and the LLM plugin as OSGi services under their own class names, and the parameter rules
 * in the capability registry. Agent Builder registers the plugin interfaces; this bundle registers only
 * implementations, so install it after Agent Builder.
 */
final class Registrar {

    private Registrar() {
    }

    static void register(BundleContext context, Collection<ServiceRegistration> sink) {
        sink.add(context.registerService(GatewayProvider.class.getName(), new GatewayProvider(), null));
        sink.add(context.registerService(GatewayAgentLLM.class.getName(), new GatewayAgentLLM(), null));

        // Composers and system agents look the LLM class up here first, and match by name second.
        AgentComponent.register(GatewayNames.PROVIDER, GatewayAgentLLM.class.getName(), AgentLLMAbstract.class);

        // Which parameters each model accepts, and the extra parameter of this provider.
        LLMModelCapabilityRegistry.register(new GatewayCapabilityProvider());
    }

    static void unregister() {
        LLMModelCapabilityRegistry.unregister(GatewayNames.PROVIDER);
    }
}

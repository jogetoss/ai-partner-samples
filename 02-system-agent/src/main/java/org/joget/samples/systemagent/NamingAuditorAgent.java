package org.joget.samples.systemagent;

import org.joget.ai.agent.model.SystemAgentAbstract;

/**
 * The whole "agent" is its identity. The model, the prompts and the tools travel in the request that
 * {@link NamingAuditor} builds, and the run goes through the same engine as a canvas agent.
 *
 * getAgentName is what Agent Execution Audit Trail shows in the Agent Name column. getAgentId is stored in the
 * ai_system_agent table; it is a class literal because the default, getClass().getName(), differs when Joget
 * Cloud wraps the instance in a proxy. getLoggedName (not overridden) labels the AI usage dashboard.
 */
public class NamingAuditorAgent extends SystemAgentAbstract {

    public static final String AGENT_ID = NamingAuditorAgent.class.getName();
    public static final String AGENT_NAME = "Form Naming Auditor";

    @Override
    protected String getAgentId() {
        return AGENT_ID;
    }

    @Override
    protected String getAgentName() {
        return AGENT_NAME;
    }
}

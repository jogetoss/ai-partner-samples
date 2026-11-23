---
name: agent-builder-system-agents
description: How to build a system agent for Agent Builder (SystemAgentAbstract) and the run-time rules around success checks, identity, threads and registration. Use when writing, reviewing or debugging a system agent.
---

# System agents

Working code: `02-system-agent` in this repository.

## What it is
A system agent is a Java class that extends `org.joget.ai.agent.model.SystemAgentAbstract`. The class is only identity
(`getAgentId`, `getAgentName`, optional `getLoggedName`). The LLM, prompts and tools travel in the
`SystemAgentRequest` that your code builds, and `run(request, properties)` goes through the same engine as an agent
drawn in the builder.

## Identity and attribution
- Use a class literal for `getAgentId()`, not `getClass().getName()` (a proxy class would give a different name).
- `getAgentName()` is the name shown in Agent Execution Audit Trail. `getLoggedName()` labels the AI Usage Dashboard; by
  default it is the agent's own name.
- Pass `username`, `fullName`, `appId` and optionally `triggerSource` in the properties map, read on the request
  thread. Without them the history row has no user.

## Success
- `run()` returns normally even when the AI provider fails. Check `Response.isFailed()`, a null or empty content and
  an "API Error" prefix. A required tool that records the result is the stronger signal.
- `TaskResult.isSuccess()` of `runAll` only means the call returned; apply the same checks to its response.

## Threads and state
- Parallel tasks and parallel tool calls run on other threads: ThreadLocals are not carried. Key per-run state by the
  run id, or return `isParallelSafe() = false` from the tool.
- Do not keep values in the execution context for later tasks.

## Registration
- Register every tool as an OSGi service under its class name in your Activator. Agent Builder re-creates each tool of
  a run by class name; an unregistered tool fails the run ("Tool class could not be loaded").
- The first tool with a given function name wins, without a message; prefix yours.

## Triggers
There is no built-in scheduler or screen for a system agent. Call `run` from a Settings console page (as the sample
does), a web service plugin, a process tool or your own scheduler.

## Packages you may use
Exported by Agent Builder and safe to call: `org.joget.ai.agent.model`, `org.joget.ai.agent.core`
(`AgentFactory`, `AgentComponent`), `org.joget.ai.config.llm` (`LLMConfigFactory`), `org.joget.ai.config.config`
(`LLMConfig`). Not exported: `org.joget.ai.agent.utils`; code that uses it compiles and then fails with
`NoClassDefFoundError`.

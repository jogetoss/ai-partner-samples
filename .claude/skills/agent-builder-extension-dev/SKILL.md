---
name: agent-builder-extension-dev
description: How to extend the Agent Builder palette with custom tools, guards, enhancers, prompts and task decisions as a Joget plugin, and how to reach the AI Composers with a custom tool. Use when writing or debugging an Agent Builder extension bundle.
---

# Agent Builder extension cheat sheet

Working code for everything below is in this repository: `01-agent-builder-tools` (tool, guard, enhancer),
`02-system-agent` (system agent), `03-composer-custom-tool` (tool for the composers) and `06-custom-llm-provider`
(LLM provider and LLM plugin).

## What an external bundle can extend
| Element | Extend | Hook |
|---|---|---|
| Tool | `AgentToolAbstract` | `Functions getFunctions()` + `String execute(AgentLLM, LLMConfig, ToolExecution)` |
| Enhancer | `AgentEnhancerAbstract` | `enhance(...)`, must call `response.setContent(...)` (the return value is not used) |
| Guard | `AgentGuardAbstract` | `filterInput` / `filterOutput`, use `resolveViolation(clean, reason)` |
| Prompt | `AgentPromptAbstract` | `generate(...)` adds messages before the model call |
| Task decision | `AgentDecisionAbstract` | `decide(...)` returns `DecisionResult` (`continueExecution`, `abort`, `goTo`) |
| LLM plugin | `AgentLLMAbstract` | `getResponse(LLMConfig, Messages, Functions)`; send the request yourself (see below) |
| LLM provider | `LLMProviderAbstract` (`org.joget.ai.config.llm`) | `getProviderName()`, `createConfig(...)`, `getPropertyOptions()`: a Model Class in AI Central Config |

All of these except the LLM provider are in the package `org.joget.ai.agent.model`.

Not reachable from an external bundle: display tools (`org.joget.ai.agent.model.display`), anything in
`org.joget.ai.agent.utils` or `org.joget.ai.agent.mcp`, and decision providers (`org.joget.ai.decision`). These packages
are in the API jar, so code that uses them compiles, but Agent Builder does not export them, so it fails at run time
with `NoClassDefFoundError`.

## LLM plugins: no Apache HttpClient types
Agent Builder carries its own Apache HttpClient and does not export it. A class in your bundle that overrides
`prepareRequestHeader(HttpRequestBase)` or `parseResponse(Response, CloseableHttpResponse)` of `AgentLLMAbstract`, or
names any other class of that library, fails at the first call with `NoClassDefFoundError`. Send the request yourself
with `java.net.http.HttpClient`, call `preparePayload(...)` for the body and `persistLLMCall(...)` for the usage row.
Do not name a credential field of a provider `apiKey`: the AI Service form has its own field with that name.

## Registration
There is no scanning. Register each element as an OSGi service under its class name:
`context.registerService(plugin.getClassName(), plugin, null)`. The class name is the identity stored in saved agents;
renaming the class breaks them.

## Rules that cause real bugs
- A tool that throws ends the task. Return `{"error": "..."}` with a sentence the model can act on.
- Two tools with the same function name: the first one wins, without a message. Prefix your function names.
- Do not define `personalizedName` or `isEnabled` properties; the builder adds them.
- Plugin instances are shared: read properties into local variables at the top of `execute`, keep no per-call state in fields.
- Policy (allow list, masking, region filter) belongs in palette properties enforced in `execute`, not in the prompt.
- A configured class that cannot be loaded, or has the wrong type, fails the task with "`<Type>` class could not be loaded".
- A guard that masks must change the message or the response itself; an enhancer must call `response.setContent`.
- Return `false` from `isParallelSafe()` if the tool uses ThreadLocals.
- Keep tool results compact.

## Property options
`getPropertyOptions()` returns normal Joget property-editor JSON. Load it with
`AppUtil.readPluginResource(className, "/properties/<Name>.json", null, true, "messages/<bundle>")`.
Guards need the properties `onViolation` (`block` / `sanitize` / `redirect`) and `redirectTaskId`.

## Reaching the AI Composers with your tool
Composers have fixed tool sets. To offer yours: create a Tool Template (Settings > AI Central Config > Tool Templates,
Tool Type = your class), then list the template name under `tools:` in a skill's `SKILL.md` and scope the skill with
`composers:`. The tool is added after the model calls `invoke_skill`, for that run only, and only if the function name
is free. So write the skill description as a trigger sentence.

## Unit tests without Joget
`plugin.setProperty(key, value)` per key (not `setProperties`, which needs the Spring context),
`new ToolExecution(name, args)`, `new Response(text)`.

## Build
Install the API jar and the Joget platform jars once (`00-setup`), then `mvn clean package "-Djoget.version=9.1.2"`
(your own Joget version) in the sample folder and upload the jar in Settings > Manage Plugins > Upload. See also the skill `joget-plugin-dx9-cloud-rules`.

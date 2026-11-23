# Custom Composer Tools and Building Your Own Composer

This page is for plugin developers. Part 1 gives the built-in AI Composers and the Orchestrator a tool of your own (the common case). Part 2 builds a complete composer for a new artifact type. Part 3 explains what is possible for Composer MCP clients. The complete example of Part 2 and Part 3 are on [Custom Composer Tools and Building Your Own Composer (continued)](28b-custom-composer-tools-and-composers-continued.md). All code compiles against the Agent Builder API jar of the examples repository (Part 2 against a `jakarta` copy of it that the build makes) and uses only classes that the plugin exports. The projects are `03-composer-custom-tool` (Part 1) and `07-custom-composer` (Part 2) of the examples repository.

## Before you start

If the composers only need your naming or wording rules, write a skill without code ([Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md)).

Read first: [Extending Agent Builder: Overview and Development Setup](23-extending-overview.md) (Maven, OSGi recipe, upload), [Writing Custom Tools](24-custom-tools.md) (the tool contract) and [The Examples Repository](29-examples-repository.md). This page uses `03-composer-custom-tool` (tool bundle, tool template, script), `04-custom-skills` and `00-setup`, which has the Agent Builder API jar and the scripts `install-api-jar` and `install-joget-jars`. Run both before your first build, and `install-api-jar` again after each new Agent Builder build.

### What your bundle can use

Compile against `org.joget.ai.agent:agent-builder-api` with `provided` scope and all transitive dependencies excluded. At run time the classes come from the installed Agent Builder plugin. A class is available to your bundle only when the plugin exports its package. The exported packages that this page uses:

- `org.joget.ai.agent.lib.composer`: `AbstractComposerChat`.
- `org.joget.ai.agent.model`: `AgentToolAbstract`, `Function`, `Functions`, `ToolExecution`, `SystemAgentAbstract`, `SystemAgentRequest`, `Response`, `AgentComponent`, `AgentAbortedException`.
- `org.joget.ai.agent.core`: `AgentFactory`. `org.joget.ai.config.llm`, `.config`, `.utils`: `LLMConfigFactory`, `LLMConfig`, `CredentialUtil`, `SkillUtil`, `SkillStore`, `ToolTemplateUtil`.
- `org.joget.ai.agent`: use the class name `org.joget.ai.agent.AgentBuilder` as a string.

The plugin also exports `org.joget.ai.agent.lib`, `org.joget.ai.agent.embedding`, `org.joget.ai.assistant`, `org.joget.ai.config`, `org.joget.ai.config.feature`, `org.joget.ai.config.model`, `org.joget.ai.config.dao` and `org.joget.ai.config.exception`. It exports nothing else.

Some packages are in the API jar but are not exported: `org.joget.ai.agent.utils` (`WebsocketUtil`, `PluginProxyUtil`, `ComposerEnablementUtil`, the abort helpers), `org.joget.ai.core`, `org.joget.ai.agent.mcp`, `org.joget.ai.agent.jev`, `org.joget.ai.agent.model.display`, `org.joget.ai.config.voice`, `org.joget.ai.decision` and `org.joget.ai.typesafe`. Code that names their classes compiles but fails at run time with `NoClassDefFoundError`. The service classes of the plugin (package `org.joget.ai.agent.service`) are neither in the API jar nor exported, so code that names them does not compile. Part 2 gives the supported replacements. Do not reach these classes by reflection.

A composer class (Part 2) needs one extra build step on Joget DX 9, because `AbstractComposerChat` has servlet and websocket types in its method signatures. The step is described with the example on [Custom Composer Tools and Building Your Own Composer (continued)](28b-custom-composer-tools-and-composers-continued.md). Tools (Part 1) need no extra step.

## Part 1: How the composers get extra tools

A composer has a fixed tool set; you cannot add to it directly. Four pieces work together:

1. **The tool**: an `AgentToolAbstract` registered as a plugin.
2. **The tool template** (**Settings > AI Central Config > Tool Templates**): names the tool class and stores its configuration.
3. **The skill** (**Settings > AI Central Config > Skills**): lists the template in **Associated Tool Templates** and the composers that may use it in **Available For**.
4. **The run**: when an active skill is available for a composer, the composer is offered the function `invoke_skill` and its system prompt lists the skills with their descriptions. When the model calls `invoke_skill` for your skill, Agent Builder takes the template names from the skill, creates each tool through the plugin manager, sets the saved properties, and adds the tool's functions to the live function list of that run, so the model sees them from its next step.

What follows from this:

- The tool exists for the **current run only**. Every chat message is a new run, so the skill must tell the model to call the tool each time.
- The skill **Description** decides whether the model invokes the skill.
- Injection ignores the template's **Status** and **Available For**. The skill's **Available For** and **Status** decide which composers are told about the skill. The template's **Available For** only controls its place in the Agent Builder palette.
- If a function of the same name is already in the run, yours is skipped silently. The first tool wins.
- The route works for every built-in composer, for the Orchestrator and for your own composer (Part 2).

## Part 1 walkthrough: write, build and deploy the tool

The example is `lookup_acme_standard` from `03-composer-custom-tool`: it returns the company's cost centres and request status values as `{value,label}` pairs, so generated forms carry real data. The classes below are source files of the repository bundle. It also has a JSON data file with six topics, a class that reads it, and unit tests.

### Step 1: The project

Use the `pom.xml` of `03-composer-custom-tool/bundle`. What matters in it:

- Packaging `bundle` (maven-bundle-plugin 5.1.9). `joget.version` is `9.1.2` and `agent-builder.version` is `8.2.5-ALPHA`. `wflow-core`, `wflow-commons`, `wflow-plugin-base` and `agent-builder-api` are `provided`, each with `<exclusion>*:*</exclusion>`; there is no `<repositories>` section. Never embed the API: a second copy of its classes breaks `instanceof`.
- `Bundle-SymbolicName` `org.joget.samples.composertool`, `Private-Package` `org.joget.samples.composertool.*`, `Export-Package` `!*`, `Bundle-Activator` `org.joget.samples.composertool.Activator`.
- `Import-Package` `!*,javax.servlet;version="0",javax.servlet.http;version="0",org.osgi.framework;version="1.3.0"` (naming `javax.servlet` makes Joget DX 9 convert the jar to Jakarta on upload) and `DynamicImport-Package` `*` (Agent Builder classes are resolved when first used).

### Step 2: The tool class

```java
package org.joget.samples.composertool;

import java.util.List;
import org.joget.ai.agent.model.AgentLLM;
import org.joget.ai.agent.model.AgentToolAbstract;
import org.joget.ai.agent.model.Function;
import org.joget.ai.agent.model.Functions;
import org.joget.ai.agent.model.LLMConfig;
import org.joget.ai.agent.model.ToolExecution;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tool the composers can call while they generate an artifact: it returns the example option lists and standards of the fictional company Acme, so
 * the generated form carries the company's cost centres, departments and leave types instead of invented ones.
 *
 * <p>How it is used. The tool is not on any composer by default. A Tool Template (Central Config) wraps this class, and a
 * skill lists that template in its {@code tools:} frontmatter. When the composer calls {@code invoke_skill} for that skill,
 * Agent Builder instantiates the template and adds {@code lookup_acme_standard} to the live function list for the rest
 * of that run.
 *
 * <p>Contract used: {@link #getFunctions()} publishes one function; {@link #execute} receives the model's arguments and
 * returns a String that becomes the tool message. Failures are returned as {"error": ...} and never thrown, because a
 * throwing tool aborts the whole composer run.
 */
public class AcmeStandardsTool extends AgentToolAbstract {

    public static final String DEFAULT_FUNCTION = "lookup_acme_standard";

    @Override
    public Functions getFunctions() {
        // The template may rename the function or reword its description; both default to the values below.
        String name = getPropertyString("name");
        String description = getPropertyString("description");
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_FUNCTION;
        }
        if (description == null || description.trim().isEmpty()) {
            description = "Look up an Acme company standard by topic and get it back as JSON. Use it BEFORE you add a "
                    + "dropdown, radio or checkbox for cost centres, departments, leave types or request status, and when you "
                    + "need the approval matrix or the date, currency and request number formats. Option lists come back as "
                    + "{value,label} pairs: put value in the stored value and label in the option label exactly as returned.";
        }
        Function f = new Function(name.trim(), description);
        String[] topics = StandardsCatalogue.load(null).topics().toArray(new String[0]);
        f.addParameter("topic", Function.ParameterType.STRING, "The standard to look up", topics, true);
        Functions fs = new Functions();
        fs.add(f);
        return fs;
    }

    @Override
    public String execute(AgentLLM llm, LLMConfig config, ToolExecution call) {
        JSONObject args = call == null || call.getArguments() == null ? new JSONObject() : call.getArguments();
        try {
            StandardsCatalogue catalogue = StandardsCatalogue.load(getPropertyString("overrideJson"));
            String topic = args.optString("topic", "").trim();
            JSONObject found = topic.isEmpty() ? null : catalogue.topic(topic);
            if (found == null) {
                List<String> available = catalogue.topics();
                return error("Unknown topic '" + topic + "'. Available topics: " + String.join(", ", available));
            }
            found.put("topic", topic);
            return found.toString();
        } catch (JSONException e) {
            // Most likely a typo in the template's Override box.
            LogUtil.warn(AcmeStandardsTool.class.getName(), DEFAULT_FUNCTION + ": invalid override JSON: " + e.getMessage());
            return error("The override JSON in the tool template is not valid: " + e.getMessage());
        } catch (RuntimeException e) {
            LogUtil.warn(AcmeStandardsTool.class.getName(), DEFAULT_FUNCTION + " failed: " + e);
            return error("Lookup failed: " + e.getMessage());
        }
    }

    private static String error(String message) {
        return new JSONObject().put("error", message).toString();
    }

    // ---- ExtDefaultPlugin / PropertyEditable / AgentBuilderPaletteElement ----

    @Override
    public String getName() {
        return "AcmeStandardsTool";
    }

    @Override
    public String getVersion() {
        return Ext.VERSION;
    }

    @Override
    public String getLabel() {
        return "Acme Standards Lookup";
    }

    @Override
    public String getDescription() {
        return "Returns Acme option lists and standards as JSON for the composers";
    }

    @Override
    public String getClassName() {
        return AcmeStandardsTool.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return Ext.propertyOptions(getClassName(), "AcmeStandardsTool");
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-book\"></i>";
    }

    /** Card shown on the Agent Builder canvas and in Central Config. */
    @Override
    public String getInfoTemplate() {
        return "<div><strong>lookup_acme_standard(topic)</strong><br/>Returns Acme option lists and standards</div>";
    }
}
```

- `getFunctions()` describes the function; the template can rename it and reword its description. `topic` is an enum of the topic names. The data comes from `StandardsCatalogue`: the file `standards/acme-standards.json` in the jar, overlaid topic by topic with the template property `overrideJson`. `Ext` loads the property form and reads the version.
- `execute(...)` returns a **string**, which becomes the tool message. Return `{"error": "..."}` for a problem: a thrown exception ends the whole composer run, a returned error lets the model correct itself.
- `getClassName()` must equal the name used in `registerService`. Do not override `isHidden()`: a hidden tool is missing from the **Tool Type** list.

### Step 3: Register the tool

```java
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
```

`Activator` is the Activator of [Extending Agent Builder: Overview and Development Setup](23-extending-overview.md): it checks that Agent Builder is present and then calls `Registrar.register`. An unregistered tool fails with "Plugin not found" when a skill injects its template.

### Step 4: The property form and the texts

They are `src/main/resources/properties/AcmeStandardsTool.json` and `src/main/resources/messages/composerTool.properties`. The JSON is the form shown as **Tool Configuration**: `name` (**Function name**, text field, default `lookup_acme_standard`, validated with `^[a-zA-Z0-9_-]{1,64}$`), `description` (**Function description**, text area) and `overrideJson` (**Override standards (JSON)**, code editor). Labels are tokens such as `@@composerTool.name@@`, which `getPropertyOptions()` translates from the properties file.

### Step 5: Build and upload

1. In `03-composer-custom-tool/bundle` run `mvn clean package "-Djoget.version=9.1.2"` (your own Joget version). 13 unit tests run. The jar is `target/composer-tool-1.0.0.jar`.
2. Open **Settings > Manage Plugins > Upload** and upload it. No restart is needed. Change `<version>` for hand-overs (a re-upload of a SNAPSHOT can serve cached resources) and upload again after each Agent Builder update.
3. The log shows `Bundle org.joget.samples.composertool started`.

## Part 1 walkthrough: tool template, skill and proof

### Step 6: Create the tool template

Open **Settings > AI Central Config**, select **Tool Templates**, click **Add Tool Template**, fill in:

| Field | Value | Notes |
|---|---|---|
| **Display Name** | `Acme Standards Lookup` | |
| **Template Name (slug)** | `lookup_acme_standard` | The field fills itself from the display name (here `acme-standards-lookup`): replace it with this text. Unique. The skill's `tools:` list uses this exact text. |
| **Tool Type** | `Acme Standards Lookup` | Lists every registered, non-hidden tool class. |
| **Tool Configuration** | **Function name** `lookup_acme_standard` | The form of your `getPropertyOptions()` |
| **Available For** | Optional | Agent Builder palette only |
| **Status** | **Active** | Only active templates are offered to skills |

Click **Create**.

### Step 7: Write and install the skill

The file is `04-custom-skills/acme-standards-lookup/SKILL.md`. Its front matter keys:

- `id`: a UUID; keep it identical across environments. `name`: `acme-standards-lookup`, what the model passes to `invoke_skill`.
- `description`: one quoted line starting "Use when ...". The model decides from this line alone whether to open the skill.
- `status`: `active`. `draft` hides the skill from all composers; `deprecated` hides it from discovery but it still runs by name.
- `composers`: full class names: `org.joget.ai.agent.lib.composer.` plus `FormComposerChat`, `DatalistComposerChat`, `UiComposerChat`, `ProcessComposerChat`, `ThemeBuilderComposerChat` or `OrchestratorChat`; for your own composer, its full class name.
- `tools`: `lookup_acme_standard`, the **Template Name** (not the class name).

The model first reads an index (the text before the first section plus the first sentence of each section), then one section at a time: start every section with a "when X, do Y" sentence. A `## ` heading must start a line and never sit inside a code block. The body of the file, shortened:

> ```markdown
> # acme-standards-lookup
> **Nav:** WhenToCall - UsingTheResult
>
> Reading this skill adds the function lookup_acme_standard(topic) to your tools for this run.
> Call it BEFORE you add any option list; never invent cost centres or status values.
>
> ## WhenToCall
>
> When the request needs a list of choices, call lookup_acme_standard with topic `cost_centres` or `request_status`.
>
> ## UsingTheResult
>
> When the tool returns an option list, copy it into the field exactly as returned: `value` is the stored value,
> `label` is the label, same order. If it returns an `error`, say so; do not make up values.
> ```

Install it in one of two ways:

1. **Central Config:** **Skills** tab, **Add Skill**; fill **Skill Name**, **Description**, **Instruction** (the body), **Available For**, **Status** (**Active**) and **Associated Tool Templates**.
2. **As a file:** copy the folder to `<JOGET_HOME>/wflow/app_ai_skills/acme-standards-lookup/SKILL.md`. The file must start with `---` and have no byte-order mark. It is re-read when its time stamp changes. In a cluster the folder must be on shared storage.

### Step 8: Prove the composer calls the tool

1. In the Form Builder open the composer and send: `Create a purchase request form with requester name (required), cost centre (dropdown), item description (textarea) and status.`
2. Click **Show steps**: `invoke_skill`, your tool, then the edits. The cost centre options equal the tool's values, in order. The log shows `invoke_skill for <composer class>: 1 skill(s) offered [acme-standards-lookup]`.
3. Set the skill to **Draft** and repeat: the composer invents its own options.
4. In the Orchestrator ask: `Question only, do not build anything: which cost centres exist in our standards?` It calls `invoke_skill`, then the tool.

## Part 1: Design rules, limits and troubleshooting

### Design rules

- Return data, not instructions: the skill (read once per run) says how to use it, while a tool description is sent on every model call. Keep results small (`value`, `label`); they stay in the conversation. Return errors as text that lists the valid choices. Make closed sets enums.
- Keep the tool stateless: the plugin instance can be shared by all users. A tool call receives no app id and no browser state; if the answer depends on the app, add a parameter and say in the skill where its value comes from.
- Use a function name that no built-in function uses, and one skill per topic.

### Limits

- Tools only, not guards, enhancers, prompts or decisions (use skills or **Central Instructions** for text rules), and for one run only.
- Use one template per tool class: Joget can hand the same plugin instance to every template of a class, so one template's properties may overwrite another's.
- All tool templates are one Joget setting; on MySQL and MariaDB it holds 64 KB and a larger change is refused. Treat template values as readable by everyone who can open AI Central Config.
- Every tool call is one more model round trip.

### Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Class missing in **Tool Type** | Bundle not active; class not registered; `isHidden()` true; not an `AgentToolAbstract` | Check **Manage Plugins** and the log; register the class; remove the override |
| Log: `Could not inject tool template 'x': Plugin not found for class: y`, or `Tool template not found: x` | The saved class differs from the registered name, the bundle is stopped, or the skill's `tools:` entry is not a **Template Name** | Use `YourTool.class.getName()` in both places; re-select **Tool Type**; use the slug in `tools:` |
| Log: `No skill is available to <composer>` | Skill not **Active**, composer not in **Available For**, or `SKILL.md` does not start with `---` | Fix the skill; the warning lists what it found |
| Skill never invoked; or invoked but the tool not called; or tool missing in the next message; or function never offered | The **Description** does not match the user's words; the skill text does not say when to call; its function name differs from **Function name**; a new message is a new run; the function name already exists in the run | Start the description with "Use when"; use the exact name and "call it before ... every time"; rename the function |

**Class names.** Three places must give the same text: `getClassName()`, the name passed to `registerService`, and the **Tool Type** saved in the template. The **Tool Type** list saves the runtime class name with everything from `$$` onward removed, so never use `$$` in a class name. A composer finds the tool through the plugin manager by the saved name: one wrong character gives "Plugin not found". The Agent Builder palette (agents, not composers) compares the saved name with the runtime class name of the plugin instance exactly; if a template works in the composers but is missing there, compare the three names first.

## Part 2: When to build your own composer

A composer is a chat panel on a builder page plus a service that lets a model change one JSON artifact through functions you define. Build one only for a new kind of artifact with its own builder page (checklists, approval matrices, document templates). For rules and data use Part 1; a second panel on an existing builder page is not supported.

You also need a **custom builder** for the artifact: a Joget plugin (extends `org.joget.apps.app.model.CustomBuilderAbstract`) served at `/web/console/app/<appId>/<version>/cbuilder/<builder name>/design/<artifact id>`, which stores the artifact as a builder definition with your type key. Writing it is outside this page; see https://dev.joget.org/community. The panel expects the page to provide the JavaScript object `CustomBuilder` with `getJson()`, `loadJson(json, ...)` and `paletteElements`; otherwise override `getBridgeJs()` to define `window.ComposerBridge` with `getJson()`, `loadJson(json)` and `getPaletteElements()`.

The panel (HTML, CSS, JavaScript, texts, voice) belongs to Agent Builder: you reuse it by returning Agent Builder's class and message path from two methods. You write three things: the **composer class** (extends `AbstractComposerChat`: identity, pages, message routing, Orchestrator contract, saving), the **service** (builds the model request, runs it, reports events) and the **artifact tool** (the functions the model calls).

The browser sends the **whole artifact JSON** with every message; the server keeps no chat state. The model never writes the artifact: it calls your functions, each changes a JSON held in a `ThreadLocal`, and the service returns the result with the **Confirm** and **Revert** buttons.

## Part 2: The composer class and the conversation

`AbstractComposerChat` is a Joget UI injector and a WebSocket plugin at `/web/socket/plugin/<class name>`. Name your class so it **ends in `ComposerChat`**: the panel derives its label and storage key from that suffix.

Methods to implement:

| Method | Return |
|---|---|
| `getName()` (public) | Shown in Central Config **Available For**, AI Session Monitor, logs. Convention: `AI Composer - Checklist Builder Agent` |
| `getVersion()`, `getDescription()` (public) | Text |
| `getInjectUrlPatterns()` (public) | Console URL patterns that get the panel |
| `getComposerUuid()` (public) | A UUID you generate once |
| `getPlatformVersion()` (protected) | `"9"` (Joget DX 9) |
| `getMessagePath()` (protected) | `agent/messages/agentPlugins` |
| `getAgentBuilderClassName()` (protected) | `org.joget.ai.agent.AgentBuilder` |
| `handleMessage(String, Session)` (protected) | Your routing |

Do not implement `getLabel()` or `getPropertyOptions()`, and do not override `onMessage`, `isComposerFeatureEnabled` or `realClassName` (final). Never return your own message path from `getMessagePath()`: the panel would look for its keys in your file and show raw `@@composer.*@@` tokens.

Always override `getAgentName()`: the default reads a text key that exists only for the built-in composers, so the header shows a raw token. Often override `getBuilderLabel()`, `getInputPlaceholder()`, `showSelectedElementBar()` (`false` without element selection) and `getBridgeJs()`.

The browser sends JSON; `appId`, `appVersion` and `selectedAiService` are added to every message:

| Browser sends | Fields | Your reply |
|---|---|---|
| `init` | `sessionId`, `reconnect` | A `status` frame, then (if `reconnect` is false) a greeting. With no active AI service, an `error` frame with the error code instead |
| `agentic_edit` | `message`, `json`, `selectedElement`, `paletteElements`, `images`, `history` (last six messages, `{role, content}`) | The frames below |
| `abort_run` | none | Stop the run |

The panel **waits for the reply to `init` before it sends the first `agentic_edit`**. If you do not answer with `status`, `chat_response` or `error`, the first message never leaves the browser.

You send:

| `type` | Fields | Meaning |
|---|---|---|
| `status` | `message` | Reply to `init` |
| `chat_response` | `content`, `welcome: true` | The greeting: `AbstractComposerChat.createWelcomeMessage(text)` |
| `error` | `message`, `errorCode` | A refusal. With `AbstractComposerChat.ERROR_CODE_AI_SERVICES_NOT_CONFIGURED` the panel shows its "add an AI service" card. `ensureAiServiceConfigured(session, type)` does this for you |
| `agentic_trace` | `runId`, `payload` | One step; feeds **Show steps** |
| `agentic_partial_update` | `json`, `step` | Live preview; **Revert** restores the state before the run |
| `agentic_complete` | `message`, `json`, `actions` | Result. `actions: ["apply","cancel"]` draws **Confirm** and **Revert**; leave it out when nothing changed |
| `agentic_aborted`, `agentic_error` | `message` | Stopped, failed |

Your own texts live in your bundle (`messages/checklistComposer.properties`) and are read with `AppPluginUtil.getMessage(key, composerClassName, "messages/checklistComposer")`, so they follow Joget's normal plugin message rules. The panel's own texts come from Agent Builder.

## Part 2: The service, the tool and skills

`executeHeadless(incoming, sink)` builds the request, runs the model and reports to a `Consumer<JSONObject>`. The WebSocket path and the Orchestrator both call it. Events: `run_started` (`runId`), `agentic_trace`, `agentic_partial_update`, `agentic_complete` (`message`, `json`, optional `changed`), `agentic_aborted`, `agentic_error`. Steps:

1. **Model:** `LLMConfigFactory.createByServiceOrActive(incoming.selectedAiService)`, then `AgentFactory.createLLM(pluginManager, config.getProviderName(), properties)` with `apiKey`, `model`, `apiUrl`, `maxTokens`.
2. **Prompts:** a `PersonaAgentPrompt` (`role`, `backstory`) as system prompt, ending with `SkillUtil.buildSkillsPromptHint(AbstractComposerChat.getActiveComposerClass())`; one `TextPromptAgentPrompt` (`msgRole` `USER` or `ASSISTANT`, `prompt`) per entry of `incoming.history` (the example omits this for brevity; add it for chat memory); then the instruction with the current JSON. For a live preview, send `agentic_partial_update` (`json`, `step`) from the trace listener after each successful `Tool` event.
3. **Tools:** your tool first, then `AgentComponent.SkillComposerTool`, which adds `invoke_skill` only when a skill is available for the active composer. Register your tool with `AgentComponent.register(name, className, AgentToolAbstract.class)` and create it with `AgentFactory.create`.
4. **Run:** `new SystemAgentRequest(llm, systemPrompts, taskPrompts, tools, enhancers, false)`, `setRunId`, then the blocking `agent.run(request, properties)`. `agent` is a small `SystemAgentAbstract` subclass that gives the run a stable id and name.
5. **After the run:** if the stop flag is set, send `agentic_aborted`; if the tool produced a result, `agentic_complete` with it; otherwise (the model answered in words) `agentic_complete` with `changed: false` and the original JSON. In `finally`, clear every `ThreadLocal` and unsubscribe the trace listener.

Agent Builder adds central instructions as the first system message if the composer context is bound to the running thread (see threading).

**The artifact tool.** One verb per function (`set_checklist`, `add_item`); an initialiser first; constraints in the parameter text ("snake_case id, at most 20 characters"); small status results, never the artifact; errors as text. Return `Boolean.TRUE` from `isHidden()` (out of the palette and the **Tool Type** list) and `false` from `isParallelSafe()`: the state is in `ThreadLocal`s, and a worker thread would see empty ones and lose the work without an error.

**Skills for your composer.** A skill reaches it when its `composers` list contains your **full class name**; the short words ("Form", "List") work only for built-in composers. Your composer appears in **Available For** as soon as it is registered, and Central Config refuses a skill that names an uninstalled composer. Built-in skills are seeded only for the built-in composers. Yours come from an administrator, from a `SKILL.md` you ship, or from your bundle at start. To write a skill from your bundle, call `SkillStore.saveSkill(JSONObject)` (package `org.joget.ai.config.utils`) from `Activator.start()` with the keys `id`, `name`, `description`, `status`, `composers` and `tools` (JSON arrays) and `instructions` (the body). Use a fixed `id` (otherwise every start adds a copy) and check `SkillStore.findById(id)` first: never overwrite a skill that administrators may have edited.

## Part 2: The Orchestrator, saving and Stop

A non-null `getOrchestratorFunctionName()` is the whole opt-in. At each request the Orchestrator takes every installed composer that has a function name, returns `true` from `isOrchestratorEnabled()` and is enabled by the administrator in AI Session Monitor (**AI Composer Availability**).

| Method | Default | Return |
|---|---|---|
| `getOrchestratorFunctionName()` | `null` | `edit_<noun>`, for example `edit_checklist`. The `edit_` prefix gives the artifact word for routing, labels and the completion report |
| `getOrchestratorFunctionDescription()` | empty | When to call it and what comes back |
| `getOrchestratorRules()` | `null` | Text added under `COMPOSER RULES:` in the Orchestrator prompt |
| `getOrchestratorBuilderType()` | `cbuilder` for every custom builder | Your builder type |

`getOrchestratorFunction()` (parameters `userMessage`, `appId`, `appVersion`, `json`, `existingArtifactId`, `imageIndices`) needs no change; `getServerSidePaletteElements()` returns class names and labels if your artifact has element types. The Orchestrator prompt is written for the built-in types, so `getOrchestratorRules()` is the only place to say where your type fits: name the function exactly; give its place relative to `edit_form`, `edit_datalist`, `edit_userview` and `edit_process`; say what it needs from earlier steps and when **not** to call it. Keep it short: every composer's rules are sent on every run. Example: `Call edit_approvalmatrix AFTER edit_form (it refers to field ids) and BEFORE edit_process, and only when the user mentions approval levels or limits.`

What the Orchestrator does with your composer:

- It calls `executeHeadless` on its own thread, bound to your composer, and waits **up to 300 seconds**. `incoming` has `message`, `history`, `appId`, `appVersion`, `json`, `selectedAiService`, `paletteElements` (if any) and `orchestrated: true`. When `orchestrated` is true, do not write the artifact yourself: the Orchestrator saves it.
- It reads the artifact's `id` and `name` from the top level of the returned JSON, then from `properties.id` and `properties.name`. **Return both**, or it cannot register or save the artifact.
- It may review the result with another model call and run your composer again with a refinement instruction and the current JSON (two rounds by default). Your composer must start from the given JSON, change it in place, and return `changed: false` when it changes nothing. Administrators can turn the review off with the Java system property `aiab.orchestrator.review=false` or limit it with `aiab.orchestrator.review.rounds`.

**Saving.** `persistArtifact(appId, appVersion, json)` returns `false` by default ("not saved"). The Orchestrator calls it only in **Create** mode with **Confirm** or **Autopilot**, and only when the JSON has an id. In **Ask** mode nothing is saved: the browser keeps the result for the builder page to apply. If you return `false`, the completion report lists the artifact as built but not saved. The example stores it with `BuilderDefinitionDao` under your type key.

**Stop.** Agent Builder's stop service is not exported. Keep your own map of active run ids (in the example, inside the tool class). On `abort_run` mark the run; the tool checks the mark before every function call and throws `AgentAbortedException`, which Agent Builder treats as "stop now". A model call that has begun finishes first. The map is per node, which is right: the run stays on the node that accepted the WebSocket.

## Part 2: Threading, cluster, Joget Cloud and testing

**Threading.** `onMessage` binds your composer class to the **dispatch thread** and clears it afterwards, but the run happens on another thread. Without the binding, `invoke_skill` and the central instructions silently vanish from the request. On the dispatch thread read `AbstractComposerChat.getActiveComposerClass(YourComposerChat.class)` and submit the work, on the base class's `executorService`, through `AbstractComposerChat.withComposerContext(className, runnable)`, which also carries the logged-in user. Do the same for any thread you start; the Orchestrator already binds your composer around `executeHeadless`. Never run the model on the dispatch thread, and clear every `ThreadLocal` in `finally`.

**Cluster and Cloud.** A WebSocket session and its run stay on the node that accepted it, so per-node state needs no sharing. Skills are files under `<JOGET_HOME>/wflow/app_ai_skills`, which must be shared storage in a cluster. On Joget Cloud a plugin instance can be wrapped in a proxy and `getClass().getName()` can carry a suffix: use class literals for ids and skill scopes, a plain string for `getAgentName()`, and never store the class name of a plugin instance. Keep your class name stable: skill scopes and the administrator's switch are keyed by it.

**Testing.** Test the tool with plain JUnit and no server: set `CURRENT`, call `tool.execute(null, null, new ToolExecution(name, args))`, assert the result and `RESULT_HOLDER`, and clear the `ThreadLocal`s after each test. Then follow "Install and check" on the continued page, and watch the model calls in Agent Execution Audit Trail: the review may repeat your composer.

The complete minimal example of a composer, and Part 3 about Composer MCP clients, are on [Custom Composer Tools and Building Your Own Composer (continued)](28b-custom-composer-tools-and-composers-continued.md).

## Where to go next

- [Custom Composer Tools and Building Your Own Composer (continued)](28b-custom-composer-tools-and-composers-continued.md)
- [Writing Custom Tools](24-custom-tools.md)
- [Skills, Central Instructions and Tool Templates](22-skills-and-tool-templates.md)
- [How the Composers Work](15-how-composers-work.md)
- [The Examples Repository](29-examples-repository.md)

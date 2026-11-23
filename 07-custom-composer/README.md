# 07-custom-composer: a composer of your own

One small plugin jar (`checklist-composer`, about 20 KB) that adds a new AI Composer to Agent Builder. The composer
edits a "checklist" artifact, `{id, name, items: [{id, label}]}`, through two functions that the model calls. It
extends the API class `AbstractComposerChat`, so it reuses the chat panel of the built-in composers, and it joins the
Orchestrator with the function `edit_checklist`.

Build a composer only for a new kind of artifact that has its own builder page. To give the built-in composers your
rules or your data, use a skill ([04-custom-skills](../04-custom-skills/README.md)) or a tool
([03-composer-custom-tool](../03-composer-custom-tool/README.md)).

> **Note:** This sample has no builder page. A complete solution also needs a custom builder plugin (a class that
> extends `org.joget.apps.app.model.CustomBuilderAbstract`) with the type `checklist`. Without it, the composer is
> registered, answers on its WebSocket address and is offered to the Orchestrator, but no page shows its chat panel.

## What the sample shows

| Class | Extension point | What it adds |
|---|---|---|
| `ChecklistComposerChat` | `AbstractComposerChat` | The composer: its name, the pages that get the panel, the message routing, the Orchestrator contract and saving |
| `ChecklistComposerService` | none | Builds the model request, runs it as a system agent and reports the events. The panel and the Orchestrator both use it |
| `ChecklistComposerAgentTool` | `AgentToolAbstract` | The functions `set_checklist` and `add_item`. They are the only way the model can change the artifact |
| `Activator`, `Registrar` | none | Start the bundle and register the composer and the tool |

What you learn from the code:

1. A composer class that extends `AbstractComposerChat` must be compiled with the `jakarta.servlet` and
   `jakarta.websocket` packages, because it overrides methods that take servlet and websocket types. The Agent
   Builder API jar is compiled with the `javax` packages. The build therefore first makes a `jakarta` copy of the API
   jar and compiles against that copy (see "How the build works").
2. The panel (HTML, CSS, JavaScript and texts) belongs to Agent Builder. The composer returns Agent Builder's class
   name and message path and brings only its own texts.
3. The model never writes the artifact. It calls functions; each function changes a JSON held in a `ThreadLocal`.
4. Work runs on a worker thread that carries the composer context (`AbstractComposerChat.withComposerContext`).
   Without it, skills and central instructions do not reach the run.
5. A function name from `getOrchestratorFunctionName()` is all the Orchestrator needs to offer the composer.

## Files

| Path | Content |
|---|---|
| `pom.xml` | Runs the two modules in order. Build here. |
| `api-jakarta/pom.xml` | Makes `agent-builder-api-jakarta-1.0-SNAPSHOT.jar`: the Agent Builder API jar with `javax.servlet` and `javax.websocket` renamed to `jakarta`. No source code. Compile time only. |
| `bundle/pom.xml` | Build file of the plugin. `joget.version` (default `9.1.2`) is a property. |
| `bundle/src/main/java/org/joget/samples/checklist/Activator.java` | Bundle entry point. Checks that Agent Builder is present, then calls `Registrar`. |
| `.../Registrar.java` | Registers the composer and the tool as OSGi services under their class names. |
| `.../ChecklistComposerChat.java` | The composer. |
| `.../ChecklistComposerService.java` | The run. |
| `.../ChecklistComposerAgentTool.java` | The tool. |
| `bundle/src/main/resources/messages/checklistComposer.properties` | The composer's own texts. |
| `bundle/src/test/java/.../ChecklistToolTest.java` | 5 unit tests of the tool. They run without Joget and without a model. |

## Prerequisites

- [00-setup](../00-setup/README.md) is done (API jar and Joget platform jars in your local Maven repository).
- Joget DX 9.1 or later (Enterprise) with Agent Builder installed, and an active AI service.
- JDK 17 and access to Maven Central. The first build downloads the Eclipse Transformer plugin.

## Build

Run the build in this folder, not in `bundle`:

```bash
mvn clean package "-Djoget.version=9.1.2"
```

Use the Joget version that `00-setup/install-joget-jars` printed. Expected result: `Tests run: 5, Failures: 0,
Errors: 0, Skipped: 0`, three lines that end in `SUCCESS` (`agent-builder-api-jakarta`, `checklist-composer`,
`checklist-composer-build`), `BUILD SUCCESS`, and the file `bundle/target/checklist-composer-1.0-SNAPSHOT.jar`.

### How the build works

1. The module `api-jakarta` runs the Eclipse Transformer (`org.eclipse.transformer:transformer-maven-plugin:1.0.0`)
   on `org.joget.ai.agent:agent-builder-api:8.2.5-ALPHA` with the standard `jakarta` rules. The result is
   `api-jakarta/target/agent-builder-api-jakarta-1.0-SNAPSHOT.jar`. Two classes change: `AbstractComposerChat` and
   one helper. Joget DX 9 uses the same tool when it installs the Agent Builder plugin, so the copy has the same
   method signatures as the classes that run on the server.
2. The module `bundle` compiles the composer against that copy and against `jakarta.servlet-api`,
   `jakarta.websocket-api` and `jakarta.websocket-client-api` (all `provided`). The source imports
   `jakarta.websocket.Session`.
3. The manifest of the plugin jar imports `jakarta.servlet`, so Joget installs the jar as it is.

Never install or embed the `jakarta` copy of the API. At run time the classes come from the installed Agent Builder
plugin.

If you compile a composer directly against `agent-builder-api-8.2.5-ALPHA.jar`, the build stops with
`package javax.websocket does not exist` and `ChecklistComposerChat is not abstract and does not override abstract
method getHtml(jakarta.servlet.http.HttpServletRequest)`.

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose `bundle/target/checklist-composer-1.0-SNAPSHOT.jar`
   in the **Upload Plugin** dialog and click **Upload**.
2. The list now contains **AI Composer - Checklist Builder Agent** (plugin type UI HTML Injector, WebSocket) and
   **Checklist Composer** (plugin type AI Agent Tool). The Joget log shows `Bundle org.joget.samples.checklist started`.

Optional script: `../00-setup/deploy-plugin.ps1 -Jar bundle\target\checklist-composer-1.0-SNAPSHOT.jar` (or
`deploy-plugin.sh`).

## Try it

1. Open **Monitor > AI Session Monitor**. The section **AI Composer Availability** lists
   **AI Composer - Checklist Builder Agent**, ticked.
2. Open **Settings > AI Central Config**, tab **Skills**. **Central Instructions** has a section
   **AI Composer - Checklist Builder Agent**.
3. Open the Orchestrator on the **Design App** page of any app. Its greeting now contains the line
   `Create or edit checklists`.
4. With a custom builder of type `checklist`: open one of its design pages, open the composer chat panel and send
   `Add three items for a new starter onboarding.` The panel shows the steps, a summary, and the buttons **Confirm**
   and **Revert**. **Confirm** loads the new JSON into the builder.

## Expected result

For the message in step 4 the composer returns an artifact like this (the wording differs from run to run):

```json
{
  "id": "new_starter_onboard",
  "name": "New Starter Onboarding",
  "items": [
    {"id": "setup_workstation", "label": "Set up workstation with required hardware and system access"},
    {"id": "complete_hr_docs", "label": "Complete and submit all HR documentation"},
    {"id": "attend_orientation", "label": "Attend company orientation session"}
  ]
}
```

## What was tested

On Joget DX 9.1.2 Enterprise with Agent Builder 8.2.5-ALPHA:

- The build above, and the 5 unit tests.
- Install by upload: the bundle starts, and steps 1 to 3 of "Try it" show the composer.
- A WebSocket conversation with the composer (`init`, then `agentic_edit` with an empty checklist): the composer
  answers with `status`, the greeting, the `agentic_trace` steps and `agentic_complete` with the new JSON and the
  actions `apply` and `cancel`.
- The chat panel in a browser, on a test page that supplied the builder container and a `ComposerBridge` object:
  the panel opens with the header **Checklist Composer**, sends the message, shows **Confirm** and **Revert**, and
  **Confirm** hands the JSON to the bridge.

Not tested: the panel on a real custom builder page (the sample has no builder), an Orchestrator run in **Create**
mode that calls `edit_checklist`, and `persistArtifact`. Test these with your own builder before you rely on them.

## Change it into your own

- **Another artifact:** change the functions in `ChecklistComposerAgentTool` (one verb per function, an initialiser
  first) and the role text in `ChecklistComposerService`. Keep `id` and `name` at the top level of the JSON: the
  Orchestrator reads them.
- **Your builder:** set `BUILDER_TYPE` and the URL pattern in `ChecklistComposerChat` to the type of your custom
  builder. Generate a new value for `getComposerUuid()`.
- **Class name:** keep the ending `ComposerChat`. Skill scopes and the administrator's switch are stored under the
  full class name, so do not rename the class later.
- **A newer Agent Builder:** install the new API jar with `00-setup/install-api-jar`, set `agent-builder.version` in
  `api-jakarta/pom.xml`, and build again.

## Limitations and things to know

- `AbstractComposerChat` is part of an alpha API and can change in a later Agent Builder build.
- Stop is cooperative: a model call that has begun finishes first.
- The tool is hidden from the Agent Builder palette (`isHidden()` returns `true`). It is only for this composer.
- The sample does not send the chat history to the model, so each message is a new conversation.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Could not find artifact org.joget.samples:agent-builder-api-jakarta` | The build was started in `bundle` | Build in `07-custom-composer` |
| `Plugin org.eclipse.transformer:transformer-maven-plugin:1.0.0 ... could not be resolved` | No access to Maven Central | Allow the download once, or add the plugin to your company repository |
| `does not override abstract method getHtml(jakarta.servlet.http.HttpServletRequest)` | The composer was compiled against the original API jar | Depend on `agent-builder-api-jakarta`, as `bundle/pom.xml` does |
| The composer is not in **AI Composer Availability** | Agent Builder was not installed or not started when the bundle started | Install Agent Builder, then upload the jar again |
| No chat panel on your builder page | The URL pattern does not match, the page has no builder container, or the composer is unticked in **AI Composer Availability** | Check `getInjectUrlPatterns()` and the page |
| Raw `@@composer...@@` text in the panel | `getMessagePath()` or `getAgentBuilderClassName()` points to your bundle | Return the Agent Builder values, as the sample does |

# 02-system-agent: Form Naming Auditor

A system agent is an agent that you write in Java instead of drawing it in the Agent Builder. It runs through the
same engine as a drawn agent, so it gets the same tracing and the same entry in Agent Execution Audit Trail. Your code
supplies the model, the prompts and the tools.

This sample (`form-naming-auditor`, about 21 KB) audits the ids of the forms and lists of a Joget app against a
naming rule and writes a report. Java finds the facts; the model only writes them up, in English or Bahasa Indonesia.

## What the sample shows

| Class | Role |
|---|---|
| `NamingAuditorAgent` | The agent. It extends `SystemAgentAbstract` and is identity only: `getAgentId()` and `getAgentName()`. |
| `NamingAuditor` | Builds the request (the active AI service from AI Central Config, a persona prompt, a task prompt, one tool) and calls `run`. This is the only class that touches the model. |
| `NamingCheckTool` | The tool `check_app_naming(appId, appVersion)`. It reads the forms and lists of an app through the Joget DAOs and returns the ids that break the rule, each with a suggested id. |
| `NamingRules` | The rule itself in plain Java: lower snake_case, starts with a letter, at most 40 characters, no double underscore. Replace it with your own standard. |
| `NamingAuditorPage` | A page in **Settings** that starts the agent. A system agent has no screen and no scheduler of its own; something must call it. |
| `Activator` | Registers the page and the tool. |

What you learn from the code:

1. The agent class is a name tag. The request carries the model, prompts and tools.
2. Every tool of a system agent must be registered as a plugin, because Agent Builder re-creates it by class name.
3. `run()` returns normally even when the AI provider fails, so the code checks the response.
4. How to add a page to **Settings** with `ConsolePagePluginAbstract` and its own JSON endpoints.

## Files

| Path | Content |
|---|---|
| `pom.xml` | Build file. It also selects the servlet namespace (`javax` or `jakarta`) from `joget.version`. |
| `src/main/java/org/joget/samples/systemagent/*.java` | The six classes above. |
| `src/main/resources/templates/page.html` | The HTML and JavaScript of the Settings page. |
| `src/test/java/.../AuditorTest.java` | 3 unit tests for the naming rule. They run without Joget. |
| `sample-output/report-example.md` | An example of a report the agent wrote. |
| `trigger-audit.js` | Optional script that runs the audit in a headless browser. |

## Prerequisites

- [00-setup](../00-setup/README.md) is done.
- Joget DX Enterprise with Agent Builder installed.
- An active AI service in **Settings > AI Central Config > AI Services**. The agent uses the service that is
  selected there; it has no model setting of its own.

## Build

```bash
mvn clean package "-Djoget.version=9.1.2"
```

Expected result: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, and the file
`target/form-naming-auditor-1.0-SNAPSHOT.jar`.

About the servlet namespace: `NamingAuditorPage` overrides a platform method that takes servlet classes. Joget DX 9
uses `jakarta.servlet`. The source file uses `javax.servlet`, and the build compiles a converted copy of that one file (in `target/generated-sources/servlet`) in
which `javax.servlet` is replaced by `jakarta.servlet`. You do not need to do anything; the pom explains the steps.

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose `target/form-naming-auditor-1.0-SNAPSHOT.jar` in the **Upload Plugin** dialog and
   click **Upload**.
2. The list now contains **Form Naming Auditor (system agent)** (file `form-naming-auditor-1.0-SNAPSHOT.jar`). It is
   also listed when you select **Console Page** or **AI Agent Tool** in **Filter by Type**. The
   Joget log shows `Bundle org.joget.samples.form-naming-auditor started`.
3. Reload the console. The **Settings** menu has a new entry **Form Naming Auditor**.

## Configure

Nothing in the plugin. Make sure one AI service is active in **Settings > AI Central Config > AI Services**.

## Try it

In the console:

1. Open **Settings > Form Naming Auditor** as an administrator.
2. Type an app id in **App** (the field suggests your published apps).
3. Choose **English** or **Bahasa Indonesia**.
4. Click **Run audit** and wait. A run usually takes 10 to 60 seconds.

Other ways to start it:

| Way | How |
|---|---|
| URL, as a logged-in administrator | `GET <JW_BASE>/web/console/plugin/FormNamingAuditor/api/audit?appId=<id>&lang=en` (or `lang=id`). The answer is JSON: `{"ok": true, "runId": "...", "report": "..."}`. |
| Your own Java code | `NamingAuditor.audit(appId, "en")`. Call it from a process tool, a scheduler or a web service plugin, for example for a nightly run. |
| Optional script | `node trigger-audit.js <appId> out` (needs `npm install` in the repository root). It saves `out/report.md` and two screenshots. |

## Expected result

- The status line shows `Done. Run id: RUN-...` and the report appears below it as Markdown text.
- For an app whose ids are all lower snake_case, the report says that all checks passed and gives the number of forms
  and lists checked.
- For an app with camelCase ids, the report has a one-line verdict, a table of violations (id, problem, suggested
  id) and three recommendations. `sample-output/report-example.md` is such a report for the Joget sample app
  `JogetDxShowcase` (75 violations in 65 forms and 44 lists).
- **Monitor > Agent Execution Audit Trail** has a new row with Agent Name `Form Naming Auditor`. Open it to see the
  prompts and the result of the tool `check_app_naming`. The tool result is the evidence; the report text is the
  model's summary of it.

The numbers come from Java code and are the same in every run. The wording of the report differs from run to run.

## How to adapt it

- **Your own rule:** change `NamingRules.problems(id)` and `NamingRules.suggest(id)`. The unit tests show the
  expected behaviour.
- **Check more things:** `NamingCheckTool.execute` reads forms and lists. Add userviews or processes in the same
  way, or write a second tool and add it to the list in `NamingAuditor.audit`. Register every tool in `Activator`.
- **Another agent:** copy `NamingAuditorAgent` (new id and name), write your own request builder like
  `NamingAuditor`, and change the persona and task prompts.
- **A fixed model instead of the active service:** `NamingAuditor.activeLlm` calls
  `LLMConfigFactory.createByServiceOrActive(null)`. Pass the name of an AI service instead of `null`.
- **Another trigger:** keep `NamingAuditor.audit` and call it from your own plugin. The page is only one possible
  caller.
- **History fields:** the properties `username`, `fullName`, `appId` and `triggerSource` that `NamingAuditor` passes
  to `run` are what Agent Execution Audit Trail shows for the run.

## Limitations and things to know

- `run()` returns normally even when the AI provider fails. The sample checks the response text
  (`isFailed()`, empty text, an `API Error` prefix). A tool that records the result is a stronger signal.
- The audit endpoint is a `GET` request to keep the sample short. A page that starts work that costs money should
  use `POST` with the console CSRF token.
- The violations come from the tool and are reliable. The verdict and the recommendations are free text from the
  model and can be wrong; for example, a report can recommend a naming style that differs from the rule. Tighten the
  persona prompt in `NamingAuditor` if you need stricter wording.
- There is no token streaming. The page waits for the complete answer.
- The tool reads the published version of the app, or the latest version when none is published, unless the model
  passes a version.
- Very large apps produce a long tool result. The sample sets `maxTokens` to 3000 for the report.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Error: No active AI service in AI Central Config: ...` | No AI service is active | Add or activate one in **Settings > AI Central Config > AI Services** |
| `Error: Agent Builder is not installed or cannot be linked: ...` (HTTP 409) | Agent Builder is missing or stopped | Install Agent Builder, then upload this jar again |
| `The AI service did not return a report.` | The provider call failed (wrong key, model name, network) | Open the run in **Monitor > Agent Execution Audit Trail** and read the error; check the credential |
| `Tool class could not be loaded` in the run | `NamingCheckTool` is not registered; the log shows `the auditor tool was not registered` | Install Agent Builder first, then upload this jar again |
| The report says the app was not found | Wrong app id | Use the app id, not the app name |
| **Form Naming Auditor** is not in the **Settings** menu | The user is not an administrator, or the page was not reloaded | Log in as an administrator and reload |
| The build fails in `NamingAuditorPage` with `method does not override or implement a method from a supertype` | The servlet namespace does not match the platform jars | Do not set `servlet.namespace` yourself; pass the real `joget.version` |

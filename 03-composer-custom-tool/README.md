# 03-composer-custom-tool: a custom tool for the AI Composers

The AI Composers (Form Composer, Process Composer, Orchestrator and the others) have fixed tool sets. This sample
shows the supported way to give them one more tool of your own.

The tool is `lookup_acme_standard(topic)`. It returns example option lists for a fictional company, Acme (sample data: cost centres, departments,
leave types, request status), the purchase approval matrix and the date, currency and request-number formats. With
it, a generated form carries the company's data instead of lists that the model invented.

## What the sample shows

The chain has four parts. Parts 1 and 2 are in this folder; part 3 is in [04-custom-skills](../04-custom-skills/README.md).

```text
1. Plugin jar          bundle/                     registers the class AcmeStandardsTool (an Agent Builder tool)
2. Tool template       AI Central Config           a saved, configured instance of that class, name lookup_acme_standard
3. Skill               acme-standards-lookup    lists the template under "tools:" and is scoped to composers
4. At run time         the composer reads the skill (invoke_skill) -> Agent Builder adds the template's
                       function to the run -> the model calls lookup_acme_standard
```

Rules that matter:

- The tool is added only after the model reads the skill, and only for that run. Each chat message is a new run.
  This is why the skill text says "call it BEFORE you add any option list".
- The name in the skill's `tools:` list must equal the template's **Template Name (slug)**.
- The function name the model sees is the tool's property **Function name**. Keep it equal to the template name.
- If another tool already has the same function name, the first one wins without a message.
- The skill must be in scope for the composer (`composers:` in the skill, **Available For** in the UI).

## Files

| Path | Content |
|---|---|
| `bundle/pom.xml` | Build file of the plugin jar. |
| `bundle/src/main/java/org/joget/samples/composertool/AcmeStandardsTool.java` | The tool. One function with one parameter, `topic`. |
| `.../StandardsCatalogue.java` | Where the data comes from: a JSON file in the jar, which the template can override. Replace this class to read your own system. |
| `.../Activator.java`, `.../Registrar.java` | Start the bundle and register the tool under its class name. |
| `.../Ext.java` | Shared helper: loads the property options and reads the version. |
| `bundle/src/main/resources/standards/acme-standards.json` | The example data. |
| `bundle/src/main/resources/properties/AcmeStandardsTool.json`, `messages/composerTool.properties` | The tool's configuration panel and its labels. |
| `bundle/src/test/java/...` | 13 unit tests. They run without Joget. `ReferenceSkillInSyncTest` checks that the skill `04-custom-skills/acme-standards-reference` holds the same data as the JSON file. |
| `tool-template/lookup_acme_standard.json` | The tool template as data, for the optional script. |
| `scripts/central-config.js` | Optional script: creates the template or a skill from a file, or lists them. |

## Prerequisites

- [00-setup](../00-setup/README.md) is done.
- Joget DX Enterprise with Agent Builder installed, and an active AI service in **Settings > AI Central Config**.
- You have the Admin role (for AI Central Config).

## Build

```bash
cd bundle
mvn clean package "-Djoget.version=9.1.2"
```

Expected result: `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, and the file
`bundle/target/composer-tool-1.0.0.jar` (about 15 KB).

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose `bundle/target/composer-tool-1.0.0.jar` in the **Upload Plugin** dialog and
   click **Upload**.
2. The list now contains **Acme Standards Tool** (file `composer-tool-1.0.0.jar`). It is also listed when you
   select **AI Agent Tool** in **Filter by Type**. The Joget log shows
   `Bundle org.joget.samples.composertool started`.

## Configure

### 1. Create the tool template

1. Open **Settings > AI Central Config**, tab **Tool Templates**.
2. Click **Add Tool Template**.
3. Fill in the form:

   | Field | Value |
   |---|---|
   | **Display Name** | `Acme Standards Lookup` |
   | **Template Name (slug)** | `lookup_acme_standard`. The field fills itself from the display name with `acme-standards-lookup`: replace that text. |
   | **Description** | `Returns Acme option lists, the purchase approval matrix and the formats.` |
   | **Tool Type** | **Acme Standards Lookup** |
   | **Tool Configuration > Function name** | `lookup_acme_standard` (the default) |
   | **Tool Configuration > Function description** | leave empty to use the built-in text |
   | **Tool Configuration > Override standards (JSON)** | leave empty |
   | **Available For** | **AI Composer - Form Builder Agent**, **AI Orchestrator Agent**, **AI Composer - Process Builder Agent** |
   | **Status** | **Active** |

4. Click **Create**.

Optional script instead of steps 1 to 4 (needs `npm install` in the repository root):

```bash
node scripts/central-config.js tool-template tool-template/lookup_acme_standard.json
node scripts/central-config.js list-tool-templates
```

### 2. Install the skills

Copy these two folders from `../04-custom-skills` into `<JOGET_HOME>/wflow/app_ai_skills/`:

- `acme-standards-lookup` (links the tool)
- `acme-naming-conventions` (field ids and labels; used in the expected result below)

No restart is needed. Other ways to install a skill are in [04-custom-skills](../04-custom-skills/README.md).

### 3. Check

Open **Settings > AI Central Config**, tab **Skills**. The card `acme-standards-lookup` shows the linked tool
template `lookup_acme_standard`.

## Try it

1. Open an app, and open a new or existing form in the Form Builder.
2. Open the composer chat panel (Ctrl+J).
3. Send:

   ```text
   Create a purchase request form with requester name (required), department (dropdown), cost centre (dropdown), item description (textarea), total amount and status.
   ```

4. Second test, in the Orchestrator (it answers without building anything):

   ```text
   Question only, do not build anything: according to our Acme standards, how many approval steps does a purchase request of Rp 20.000.000 need, and which cost centres exist?
   ```

## Expected result

For the form:

- The run trace (**Monitor > Agent Execution Audit Trail**, open the run) shows a call of `invoke_skill` for
  `acme-standards-lookup`, then three calls of **Acme Standards Lookup** with the topics `departments`,
  `cost_centres` and `request_status`.
- The cost centre dropdown contains exactly `CC-1000`, `CC-2000`, `CC-3000` and `CC-4100` with their labels.
- The department dropdown contains the five departments with English stored values and Bahasa Indonesia labels
  (`finance` = `Keuangan`, and so on).
- With `acme-naming-conventions` installed, the field ids start with `acme_` and the labels are in Bahasa
  Indonesia. `../04-custom-skills/expected-result/with-skills.fields.txt` shows a typical result.

For the Orchestrator question: it reads the skill, calls the tool for `approval_matrix` and `cost_centres`, and
answers 2 approval steps (line manager, then department head) and the four cost centres.

The model decides the order of its calls, so a trace can differ in detail. The option lists must be exact.

## How to adapt it

- **Change the data without code:** edit the tool template and paste JSON into **Override standards (JSON)**. It has
  the same shape as `acme-standards.json`: an object of topics. A topic you give replaces the bundled topic of the
  same name; you can also add new topics. Example:

  ```json
  {"departments": {"description": "Departments", "options": [{"value": "legal", "label": "Hukum"}]}}
  ```

- **Read your own system:** replace `StandardsCatalogue`. Keep the two methods `topics()` and `topic(name)` and fetch
  the data from your ERP, a Joget form table or a REST service.
- **Your own tool:** copy `AcmeStandardsTool`, change the function name (use your own prefix), the description
  and `execute`. Return errors as `{"error": "..."}`; a tool that throws ends the whole composer run.
- **Rename the function:** change **Function name** in the template, the template name, and the name in the skill's
  `tools:` list and text. All three must agree.
- **Keep the static copy in step:** if you change `acme-standards.json`, change
  `04-custom-skills/acme-standards-reference/SKILL.md` too. The unit test fails when they differ.

## Limitations and things to know

- The tool reaches a composer only through a tool template plus a skill. A composer never sees it by default.
- The tool lasts for one run. In a long chat the model must read the skill again in each message that needs it.
- The template field **Available For** is used when templates are listed for a composer. It does not limit a tool
  that a skill links.
- If a form id already exists in the app, the Form Composer does not overwrite that form; it creates a new id (for
  example `purchase_request_2`).
- Over Composer MCP, the `tools:` link of a skill has no effect, because the tool exists only inside Agent Builder
  runs. `04-custom-skills/acme-standards-reference` gives MCP clients the same data as text.
- `scripts/central-config.js` calls the web service behind the AI Central Config page. That service is not a public
  API and can change between Agent Builder versions.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| **Acme Standards Lookup** is not in the **Tool Type** list | The jar is not installed, or Agent Builder was not running when it started (log: `Agent Builder is not installed; the Acme standards tool is not registered`) | Install Agent Builder, then upload the jar again |
| The trace shows `invoke_skill`, but no lookup call | The name in the skill's `tools:` differs from the template name; or the template is not Active; or another tool already uses the function name | Make the names equal; set the template to Active; choose a unique function name |
| The composer never reads the skill | The skill is not scoped to that composer, or its status is Draft | Tick the composer under **Available For**; set **Status** to Active |
| The tool answers `The override JSON in the tool template is not valid` | A typing error in **Override standards (JSON)** | Correct the JSON |
| The tool answers `Unknown topic '...'. Available topics: ...` | The model asked for a topic that does not exist | Nothing; the model retries with a valid topic. Add the topic if you need it |
| The dropdown values are invented | The model built the form without reading the skill | Make the skill description a clear trigger ("Use when a form needs a dropdown ..."), or name the skill in your prompt |

# 01-agent-builder-tools: a custom Tool, Guard and Enhancer

One small plugin jar (`indonesian-id-tools`, about 22 KB) that adds three elements to the Agent Builder palette. It
needs no external system: the tool uses built-in rules for Indonesian identity numbers.

- NIK (Nomor Induk Kependudukan) is the 16-digit national identity number. It contains a province code, a birth date
  and the gender.
- NPWP (Nomor Pokok Wajib Pajak) is the tax number. It has 15 digits (the old format) or 16 digits. A 16-digit NPWP
  of an individual is the holder's NIK. A 16-digit NPWP of a company or a non-resident is `0` followed by the old
  15 digits. The tool checks a 16-digit NPWP that starts with `0` as those 15 digits, and any other 16-digit NPWP
  as a NIK.

## What the sample shows

| Palette section | Label in the palette | Class | What it does |
|---|---|---|---|
| Tools | Indonesian ID Validator | `IndonesianIdValidatorTool` | Gives the model the function `validate_indonesian_id(idType, number)`. It checks a NIK or an NPWP and decodes province, birth date, gender, tax office code. |
| Guards | NIK PII Guard | `NikPiiGuard` | Blocks the task, masks the number (`317401******0001`) or jumps to another task when a real-looking NIK is in the request or in the answer. |
| Enhancers | NPWP Format and Notice | `NpwpFormatEnhancer` | Rewrites a bare 15-digit NPWP as `09.254.294.4-407.000` and appends a notice to the answer. |

What you learn from the code:

1. A palette element is a normal Joget plugin, registered under its own class name. There is no scanning.
2. A tool is a function description plus a method that returns a String. Errors are returned as JSON, never thrown.
3. Policy (which ID types, masking) is a palette property that your Java code enforces, so the model cannot talk its
   way around it.
4. A guard must change the message or the response itself to mask something. An enhancer must call
   `response.setContent(...)`; the value it returns is not used.
5. The jar compiles against the API jar with scope `provided` and resolves Agent Builder packages at run time, so the
   bundle starts even when Agent Builder is missing.

## Files

| Path | Content |
|---|---|
| `pom.xml` | Build file. `joget.version` (default `9.1.2`) and `agent-builder.version` (`8.2.5-ALPHA`) are properties. |
| `src/main/java/org/joget/samples/agenttools/Activator.java` | Bundle entry point. Checks that Agent Builder is present, then calls `Registrar`. |
| `.../Registrar.java` | Registers the three elements as OSGi services under their class names. |
| `.../IndonesianIdValidatorTool.java` | The tool. |
| `.../NikPiiGuard.java` | The guard. |
| `.../NpwpFormatEnhancer.java` | The enhancer. |
| `.../IndonesianIds.java` | The rules for NIK and NPWP in plain Java. |
| `.../Ext.java` | Shared helper: loads the property options and reads the version. |
| `src/main/resources/properties/*.json` | The configuration panel of each element. |
| `src/main/resources/messages/idTools.properties` | Labels for those panels. |
| `src/test/java/.../IdToolsTest.java` | 11 unit tests. They run without Joget. |
| `demo-app/acme_tools.jwa` | A Joget app with three ready agents that use the elements. |
| `demo-app/agents/*.json` | The same three agent definitions as readable JSON. |
| `demo-app/build-demo-app.js`, `demo-app/run-agent.js` | Optional scripts. |
| `images/agent-canvas.png` | How the first demo agent looks in the builder. |

## Prerequisites

- [00-setup](../00-setup/README.md) is done (API jar and Joget platform jars in your local Maven repository).
- Joget DX Enterprise with Agent Builder installed.
- To try the agents: an AI credential in **Settings > AI Central Config > AI Credentials**. The demo app expects an
  Anthropic credential with the name `Anthropic`.

## Build

```bash
mvn clean package "-Djoget.version=9.1.2"
```

Use the Joget version that `00-setup/install-joget-jars` printed. Expected result: `Tests run: 11, Failures: 0,
Errors: 0, Skipped: 0`, `BUILD SUCCESS`, and the file `target/indonesian-id-tools-1.0-SNAPSHOT.jar`.

## Install

1. Open **Settings > Manage Plugins**, click **Upload**, choose `target/indonesian-id-tools-1.0-SNAPSHOT.jar` in the **Upload Plugin** dialog and
   click **Upload**.
2. The list now contains **Indonesian ID Tools for Agent Builder** (file `indonesian-id-tools-1.0-SNAPSHOT.jar`). It
   is also listed when you select **AI Agent Tool**, **AI Agent Guard** or **AI Agent Enhancer** in **Filter by
   Type**. The Joget log
   shows `Indonesian ID tools registered: 3 plugins` and `Bundle org.joget.samples.indonesian-id-tools started`.

Optional script: `../00-setup/deploy-plugin.ps1 -Jar target\indonesian-id-tools-1.0-SNAPSHOT.jar` (or `deploy-plugin.sh`).

## Configure

### The demo app

1. **Design Apps > Import App**, choose `demo-app/acme_tools.jwa`. The app is `Acme Tools Demo` (id `acme_tools`)
   with three agents.
2. The agents use an Anthropic model (`claude-sonnet-4-6`) through the AI credential named `Anthropic`. Do one of
   these:
   - Create an Anthropic credential with exactly the name `Anthropic` in **Settings > AI Central Config >
     AI Credentials**.
   - Or open each agent in the builder, click the **LLM SERVICE** card, and choose your own credential and model.
     You can also drag another LLM service from the palette.
   - Or regenerate the app for your credential name: `AI_SERVICE="My Credential" node demo-app/build-demo-app.js`
     (needs `npm install` in the repository root), then import the new `.jwa`.

No API key is stored in the `.jwa` or in the JSON files.

### The palette properties

| Element | Property | Values | Default |
|---|---|---|---|
| Indonesian ID Validator | **Allowed ID types** | NIK and NPWP, NIK only, NPWP only | NIK and NPWP |
| | **Validation mode** | Format only: length and digits. Strict: also the province code and birth date of a NIK, and the tax office code of an NPWP. | Strict |
| | **Return full number** | Checked: the tool returns the full number. Not checked: the number is masked. | not checked |
| | **Extra province codes** | Grid of **Code (2 digits)** and **Province name**: additions to the built-in province table | empty |
| NIK PII Guard | **Direction** | Request and answer, Request only, Answer only | Request and answer |
| | **On violation** | Block the task, Mask the number and continue, Jump to another task | Mask the number and continue |
| | **Redirect task id** | The id of the task to jump to (only for Jump to another task) | empty |
| NPWP Format and Notice | **Notice text** | Text appended when the answer mentions NIK or NPWP. Empty switches the notice off. | `Catatan: pengecekan hanya memeriksa format, bukan data resmi DJP atau Dukcapil.` |

## Try it

The demo agents:

| Agent | Elements | Purpose |
|---|---|---|
| Acme 1 ID Check (tool + guard + enhancer) | Tool (strict, full number, extra province `99`), Guard (Answer only, mask), Enhancer | The full chain |
| Acme 2 ID Check (tool only) | Tool only | Shows the answer without guard and enhancer |
| Acme 3 PII Input Block (guard only) | Guard (Request only, block) | Shows a blocked request |

Steps:

1. Open the app `acme_tools` in the App Composer, then open the agent **Acme 1 ID Check (tool + guard +
   enhancer)**. The task **Answer** shows a GUARD card, two PROMPT cards, a TOOL card and an ENHANCER card, as in
   `images/agent-canvas.png`.
2. Type `Indonesian` in **Search Palette**: **Indonesian ID Validator** is listed under **Tools**. Search `NIK`:
   **NIK PII Guard** is under **Guards**. Search `NPWP`: **NPWP Format and Notice** is under **Enhancers**.
3. Click the TOOL card to see its four properties.
4. Open the **Preview** tab and send this message:

   ```text
   Tolong cek NIK 3174011501900001 dan NPWP 092542944407000
   ```

5. Send the same message to agent 2, then the messages in the table below.

The numbers in the examples are made up; they only have a valid structure.

## Expected result

| Agent | Message | Expected |
|---|---|---|
| 2 (tool only) | `Tolong cek NIK 3174011501900001 dan NPWP 092542944407000` | The answer shows the NIK in full, `3174011501900001`, with province DKI Jakarta, birth date 1990-01-15, male. No notice at the end. |
| 1 (tool + guard + enhancer) | the same | The tool result in the trace holds the full number. The answer shows the masked NIK `317401******0001`, the NPWP as `09.254.294.4-407.000`, and the last line is the notice `Catatan: pengecekan hanya memeriksa format, bukan data resmi DJP atau Dukcapil.` |
| 1 | `NIK 9974011501900001 valid? dan NPWP 12345` | The NIK is valid with province `Provinsi Contoh (demo)` (from the **Extra province codes** grid). The NPWP is invalid: it needs 15 digits. |
| 3 (guard only) | `Cek KTP saya 3174011501900001 ya` | The task fails with `Content blocked by guard: 1 NIK number(s) in the request`. The model is not called. |

The wording of the model's answer differs from run to run. The numbers, the masking, the NPWP layout and the notice
come from Java code and do not change.

To see what happened inside a run, open **Monitor > Agent Execution Audit Trail** and open the run.

Optional script (needs `npm install` in the repository root; it calls your AI provider):

```bash
node demo-app/run-agent.js acme_id_check "Tolong cek NIK 3174011501900001 dan NPWP 092542944407000"
AI_SERVICE="My Credential" node demo-app/run-agent.js acme_pii_input_block "Cek KTP saya 3174011501900001 ya"
```

## How to adapt it

- **Another tool:** copy `IndonesianIdValidatorTool`. Change `getFunctions()` (name, description, parameters: this
  is all the model sees), `execute()` and the properties JSON. Give the function a name with your own prefix: when
  two tools have the same function name, the first one wins without a message.
- **Another guard:** copy `NikPiiGuard`. Keep the properties `onViolation` and `redirectTaskId`, and keep
  `resolveViolation(cleanText, reason)`: it turns the builder's choice into the result.
- **Another enhancer:** copy `NpwpFormatEnhancer`. Always end with `response.setContent(result)`.
- **Register it:** add one line in `Registrar.register(...)`.
- **Rename:** change `artifactId`, `Bundle-SymbolicName`, `Private-Package`, `Bundle-Activator`, `Joget-Name` in
  `pom.xml`, and the Java package. Saved agents refer to an element by its class name, so do not rename a class after
  agents use it.
- **Unit tests:** set properties one by one with `setProperty(key, value)`. `setProperties(...)` needs the running
  Joget.

## Limitations and things to know

- The checks are structural only. Nothing asks the tax office or the civil registry.
- On the request side, use **Block the task** or **Jump to another task**. Masking the request and continuing is
  not guaranteed to reach the model in masked form. The demo uses block on the request side and mask on the answer
  side.
- A blocked task ends as failed, with the reason as the response. There is no friendlier message.
- Display tools and decision providers cannot be written in an external bundle: Agent Builder does not export those
  packages.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| The elements are not in the palette | Agent Builder was not running when this bundle started; the log shows `Agent Builder is not available, nothing registered` | Install Agent Builder, then upload this jar again |
| A canvas card shows a missing plugin | The jar is not installed, or a class was renamed | Install the jar; keep class names stable |
| The run fails with an error about the LLM or the credential | No AI credential with the name the agent uses | See "The demo app" above |
| `Tool class could not be loaded` | The element is in the agent but its plugin is not registered | Upload the jar again; check the Joget log for the bundle |
| The answer has no notice | **Notice text** is empty, or the answer does not contain the word NIK or NPWP | Set the text; check the answer |

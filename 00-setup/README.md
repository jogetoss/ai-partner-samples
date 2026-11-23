# 00-setup: prepare your machine to build the samples

The Java samples (01, 02, 03 and 06) compile against two things that are not on a public Maven repository:

1. The **Agent Builder API jar** (`agent-builder-api`). It is delivered in this folder.
2. Five **Joget platform jars** (`wflow-core`, `wflow-commons`, `wflow-plugin-base`, `wflow-wfengine`,
   `wflow-directory`). They are in every Joget installation.

This folder installs both into your local Maven repository. After that, every sample builds with one Maven command.
All other libraries come from Maven Central.

## Files

| File | Purpose |
|---|---|
| `lib/agent-builder-api-8.2.5-ALPHA.jar` | The Agent Builder API: the classes an extension extends or calls. It is the API jar contained in `agent-builder-plugin-8.2.5-ALPHA.jar`. |
| `lib/agent-builder-api-8.2.5-ALPHA.pom` | A Maven pom for that jar without dependencies, so Maven resolves nothing else. |
| `install-api-jar.ps1`, `install-api-jar.sh` | Install the API jar into the local Maven repository. |
| `install-joget-jars.ps1`, `install-joget-jars.sh` | Install the Joget platform jars of your own Joget installation into the local Maven repository. |
| `deploy-plugin.ps1`, `deploy-plugin.sh`, `deploy-upload.js` | Optional: install a built jar on a local Joget with one command. |
| `check-install.js` | Optional: list which plugins of this repository are installed on a Joget. |

## Prerequisites

- JDK 17 and Apache Maven 3.8 or later on the `PATH` (`java -version`, `mvn -v`).
- A Joget DX Enterprise installation on the same machine (for step 2), or another source for the Joget platform jars.
- Internet access to Maven Central for the first build.

## Step 1: install the Agent Builder API jar

Windows (PowerShell):

```powershell
.\install-api-jar.ps1
```

Linux, macOS or Git Bash:

```bash
./install-api-jar.sh
```

Expected result:

```text
Installed org.joget.ai.agent:agent-builder-api:8.2.5-ALPHA
```

The scripts run this Maven command. You can also run it yourself:

```bash
mvn install:install-file -Dfile=lib/agent-builder-api-8.2.5-ALPHA.jar -DpomFile=lib/agent-builder-api-8.2.5-ALPHA.pom
```

The samples use these coordinates, with scope `provided`:

```xml
<dependency>
    <groupId>org.joget.ai.agent</groupId>
    <artifactId>agent-builder-api</artifactId>
    <version>8.2.5-ALPHA</version>
    <scope>provided</scope>
</dependency>
```

Never put the API jar inside your own plugin jar. At run time the classes come from the installed Agent Builder
plugin; a second copy makes Agent Builder reject your plugin.

## Step 2: install the Joget platform jars

The Joget Maven repository (`dev.joget.org/archiva`) asks for a login. Without that login Maven cannot download
`wflow-core` and the other platform jars. You have three ways to get them into your local Maven repository:

| Way | When to use it |
|---|---|
| A. `install-joget-jars` (this folder) | You have a Joget installation. This is the quickest way. |
| B. Build Joget from source with `mvn install` | You already build Joget yourself. The artifacts are then in your local repository and you can skip this step. |
| C. Your own login for the Joget Maven repository | You have one. Add the repository and the credentials to your Maven `settings.xml`. The sample poms contain no `<repositories>` section on purpose. |

Way A, Windows (PowerShell):

```powershell
.\install-joget-jars.ps1 -JogetHome "C:\Joget-DX9-Enterprise"
```

Way A, Linux, macOS or Git Bash:

```bash
./install-joget-jars.sh /opt/joget
```

You can also set the `JOGET_HOME` environment variable and leave the argument out.

Expected result (the version and the Tomcat folder are those of your installation):

```text
Reading Joget jars from C:\Joget-DX9-Enterprise\apache-tomcat-11.0.26\webapps\jw\WEB-INF\lib
Installed org.joget:wflow-core:9.1.2  (from wflow-core-9.1.2.jar)
Installed org.joget:wflow-commons:9.1.2  (from wflow-commons-9.1.2.jar)
Installed org.joget:wflow-plugin-base:9.1.2  (from wflow-plugin-base-9.1.2.jar)
Installed org.joget:wflow-wfengine:9.1.2  (from wflow-wfengine-9.1.2.jar)
Installed org.joget:wflow-directory:9.1.2  (from wflow-directory-9.1.2.jar)

Done. Build the samples with:  mvn clean package "-Djoget.version=9.1.2"
```

How the script works: it looks for `apache-tomcat*/webapps/jw/WEB-INF/lib` under the Joget folder, reads the
version from the file name `wflow-core-<version>.jar`, and runs `mvn install:install-file` for each of the five jars
with group `org.joget` and a generated pom without dependencies. It copies nothing into this repository.

Options:

| PowerShell parameter | Shell equivalent | Meaning |
|---|---|---|
| `-JogetHome <folder>` | first argument, or `JOGET_HOME` | The Joget installation folder |
| `-LibDir <folder>` | `LIB_DIR=<folder>` | The folder that holds the `wflow-*.jar` files, when it is not under a standard Joget folder |
| `-Version <text>` | `JOGET_VERSION=<text>` | Install under this version instead of the version in the file names |
| `-RepoLocal <folder>` | `MAVEN_REPO_LOCAL=<folder>` (for `install-api-jar.sh`: first argument) | Use another local Maven repository folder |

## Step 3: build a sample

```bash
cd ../01-agent-builder-tools
mvn clean package "-Djoget.version=9.1.2"
```

Use the version that step 2 printed. On PowerShell keep the quotation marks around `-Djoget.version=...`.

The default of `joget.version` in the poms is `9.1.2`, so on Joget DX 9.1.2 a plain `mvn clean package` gives the
same result. The samples were built and unit-tested against the platform version `9.1.2`. See the
top-level [README](../README.md#joget-versions) for details.

## Step 4: install a jar on Joget

Manual route (always works):

1. Log in to Joget as an administrator.
2. Open **Settings > Manage Plugins**.
3. Click **Upload**. In the **Upload Plugin** dialog choose the jar from the sample's `target` folder and click
   **Upload**.
4. The plugin appears in the list **Installed Plugins** under the name of its jar, for example **Indonesian ID Tools
   for Agent Builder**. No restart is needed. **Filter by Type** lists the jars that contain a plugin of a type, for
   example **AI Agent Tool**.

Optional script, for a Joget on the same machine. It first copies an installed jar with the same file name to
`<JOGET_HOME>/wflow/app_plugins_backup`, then uploads the new jar through the same upload form (this needs
`npm install` in the repository root, and Chrome):

```powershell
$env:JOGET_HOME = "C:\Joget-DX9-Enterprise"
.\deploy-plugin.ps1 -Jar ..\01-agent-builder-tools\target\indonesian-id-tools-1.0-SNAPSHOT.jar
```

```bash
JOGET_HOME=/opt/joget ./deploy-plugin.sh ../01-agent-builder-tools/target/indonesian-id-tools-1.0-SNAPSHOT.jar
```

Without a browser: add `-ViaCopy` (PowerShell) or `--via-copy` (shell). The script then copies the jar into
`<JOGET_HOME>/wflow/app_plugins`, and Joget loads it within a few seconds.

The upload uses `JW_BASE`, `JW_USER` and `JW_PASS` (defaults `http://localhost:8080/jw`, `admin`, `admin`).

## Step 5: check

In the console: **Settings > Manage Plugins** lists the uploaded plugin, and **Settings > AI Central Config** opens.

Optional script:

```bash
node check-install.js
```

Expected result when everything is installed:

```text
Joget at http://localhost:8080/jw: 345 plugins
FOUND    Agent Builder: AI Agent Builder [Custom Builder, Web Service] version 8.2.5-ALPHA
FOUND    Agent Builder: AI Central Config [Console Page, Web Service] version 8.2.5-ALPHA
FOUND    Composer MCP (optional, Joget DX 9.1 or later): Composer MCP Server [Web Filter] version 9.1.0-ALPHA
FOUND    01-agent-builder-tools: Indonesian ID Validator [AI Agent Tool] version 1.0-SNAPSHOT
FOUND    01-agent-builder-tools: NIK PII Guard [AI Agent Guard] version 1.0-SNAPSHOT
FOUND    01-agent-builder-tools: NPWP Format and Notice [AI Agent Enhancer] version 1.0-SNAPSHOT
FOUND    02-system-agent: Form Naming Auditor [Console Page] version 1.0
FOUND    02-system-agent: App Naming Check [AI Agent Tool] version 1.0
FOUND    03-composer-custom-tool: Acme Standards Lookup [AI Agent Tool] version 1.0.0
FOUND    06-custom-llm-provider: Private Gateway [LLM Provider Plugin] version 1.0.0
FOUND    06-custom-llm-provider: Private Gateway LLM [AI Agent LLM] version 1.0.0
FOUND    07-custom-composer: AI Composer - Checklist Builder Agent [UI HTML Injector, WebSocket] version 1.0.0
FOUND    07-custom-composer: Checklist Composer [AI Agent Tool] version 1.0.0
```

The script lists plugin classes with their types; **Manage Plugins** lists the jars. The number of plugins and the
version texts depend on your installation.

## Limitations and things to know

- The API jar belongs to Agent Builder 8.2.5-ALPHA. When you receive a newer Agent Builder jar, use the API jar that
  comes with it and build again.
- `install-joget-jars` installs the jars with a pom that has no dependencies. The sample poms name every library
  they need themselves, so this is enough for the samples. A project of your own that expects the full dependency
  tree of `wflow-core` needs way B or C.
- A SNAPSHOT plugin jar that you upload again under the same file name replaces the earlier one. If Joget still
  shows old resources, raise `<version>` in the pom and upload the new file.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `mvn is not on the PATH` | Maven is not installed or not on the `PATH` | Install Maven 3.8 or later |
| `No apache-tomcat*\webapps\jw\WEB-INF\lib under ...` | Wrong folder, or the web application was never unpacked | Give the folder that contains `apache-tomcat-*` and `wflow`; start Joget once; or use `-LibDir` / `LIB_DIR` |
| `No wflow-core-<version>.jar in ...` | The folder is not a Joget `WEB-INF/lib` | Check the folder |
| PowerShell says running scripts is disabled | The execution policy blocks unsigned scripts | Run `powershell -ExecutionPolicy Bypass -File .\install-api-jar.ps1`, or use the `.sh` script in Git Bash |
| The build still says `Could not find artifact org.joget:wflow-core:jar:9.1.2` (or another version) | The version you build with is not the version that was installed. Without `-Djoget.version` the build uses `9.1.2` | Pass the version that `install-joget-jars` printed |
| `class file has wrong version 61.0` | The build runs on a JDK older than 17 | Use JDK 17 |
| The upload script ends with `The upload form was not found` | The user is not an administrator, or `JW_BASE` is wrong | Check `JW_BASE`, `JW_USER`, `JW_PASS` |

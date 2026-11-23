package org.joget.samples.composertool;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.joget.apps.app.service.AppUtil;

/** Constants shared by the classes of this bundle. Touches no Agent Builder class. */
public final class Ext {

    /** Message bundle (no extension) used to translate the "@@key@@" labels of the property options. */
    public static final String MESSAGE_PATH = "messages/composerTool";
    /** The POM version, filtered into the messages file at build time. */
    public static final String VERSION = readVersion();

    private Ext() {
    }

    /** Reads a property-options JSON out of this bundle and translates its @@key@@ labels. */
    public static String propertyOptions(String className, String name) {
        return AppUtil.readPluginResource(className, "/properties/" + name + ".json", null, true, MESSAGE_PATH);
    }

    static String readVersion() {
        try (InputStream in = Ext.class.getResourceAsStream("/" + MESSAGE_PATH + ".properties")) {
            Properties p = new Properties();
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            String v = p.getProperty("ext.version", "").trim();
            return v.isEmpty() || v.startsWith("${") ? "unknown" : v;
        } catch (IOException e) {
            return "unknown";
        }
    }
}

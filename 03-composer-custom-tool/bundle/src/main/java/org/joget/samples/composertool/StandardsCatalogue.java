package org.joget.samples.composertool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Where the standards come from. This sample reads a JSON file bundled in the jar and lets the tool template overlay
 * it, topic by topic, with a JSON text typed into the template's "Override" box.
 *
 * <p>This class is the seam to replace in a real project: keep {@link #topics()} and {@link #topic(String)} and fetch the
 * data from your ERP, a Joget form table (FormDataDao) or a REST call instead of a file.
 */
final class StandardsCatalogue {

    static final String RESOURCE = "/standards/acme-standards.json";

    private final JSONObject topics;

    private StandardsCatalogue(JSONObject topics) {
        this.topics = topics;
    }

    /** The bundled defaults, overlaid with {@code overrideJson} (may be null or blank). */
    static StandardsCatalogue load(String overrideJson) {
        JSONObject merged = new JSONObject(bundled().toString());
        if (overrideJson != null && !overrideJson.trim().isEmpty()) {
            JSONObject override = new JSONObject(overrideJson); // JSONException is handled by the caller
            for (Iterator<String> it = override.keys(); it.hasNext();) {
                String key = it.next();
                JSONObject value = override.optJSONObject(key);
                if (value != null) {
                    merged.put(key, value);
                }
            }
        }
        return new StandardsCatalogue(merged);
    }

    /** Topic names in file order of the bundled defaults first, then any topic added by the override. */
    List<String> topics() {
        List<String> names = new ArrayList<>();
        for (Iterator<String> it = topics.keys(); it.hasNext();) {
            names.add(it.next());
        }
        names.sort(String::compareTo);
        return names;
    }

    /** A copy of one topic, or null when the topic is unknown. */
    JSONObject topic(String name) {
        JSONObject t = topics.optJSONObject(name);
        return t == null ? null : new JSONObject(t.toString());
    }

    private static JSONObject bundled() {
        try (InputStream in = StandardsCatalogue.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE + ": " + e.getMessage(), e);
        }
    }
}

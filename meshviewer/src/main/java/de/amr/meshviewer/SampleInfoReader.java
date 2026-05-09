/*
 * Copyright (c) 2026 Armin Reichert (MIT License)
 */

package de.amr.meshviewer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.amr.meshviewer.info.SampleInfo;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SampleInfoReader {

    public static List<SampleInfo> loadSampleInfo(URL url) {
        final Gson gson = new Gson();
        List<SampleInfo> result = List.of();
        try (final InputStream in = url.openStream()) {
            if (in != null) {
                result = gson.fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<List<SampleInfo>>() {}.getType());
            }
        } catch (IOException x) {
            Logger.error(x, "Could not load samples/toc.json");
        }
        return result;
    }

    public static List<SampleInfo> loadUserProvidedSampleInfo(File userDir) throws MalformedURLException {
        final URL url = new File(userDir, "toc.json").toURI().toURL();
        return loadSampleInfo(url);
    }
}

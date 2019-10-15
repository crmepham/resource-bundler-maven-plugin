package com.github.crmepham;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 * Attempts to fetch the contents of an external.
 */
public final class ExternalDependencyFetcher {

    /**
     * Attempt to get the contents of an external dependency.
     * @param uri The fully qualified URI where the document resides.
     * @return The contents of the URI as a String.
     * @throws IOException If something went wrong.
     */
    public static String fetch(String uri) throws IOException {
        final URL url = new URL(uri);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(false);
        con.disconnect();
        return IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8.name());
    }
}

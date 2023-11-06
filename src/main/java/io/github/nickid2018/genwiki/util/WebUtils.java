package io.github.nickid2018.genwiki.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebUtils {

    public static JsonElement getJson(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createSystem()) {
            HttpGet get = new HttpGet(url);
            return client.execute(get, response -> {
                if (response.getCode() != 200)
                    throw new IOException("Failed to get json from " + url + ": " + response.getCode());
                return JsonParser.parseReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
            });
        }
    }

    public static void downloadFile(String url, File file) throws IOException {
        try (InputStream input = new URL(url).openStream(); FileOutputStream output = new FileOutputStream(file)) {
            IOUtils.copy(input, output);
        }
    }
}

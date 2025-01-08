package io.github.nickid2018.genwiki.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebUtils {

    public static JsonElement getJson(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createSystem()) {
            HttpGet get = new HttpGet(url);
            return client.execute(
                get, response -> {
                    if (response.getCode() != 200)
                        throw new IOException("Failed to get json from " + url + ": " + response.getCode());
                    return JsonParser.parseReader(new InputStreamReader(
                        response.getEntity().getContent(),
                        StandardCharsets.UTF_8
                    ));
                }
            );
        }
    }

    @SneakyThrows
    public static void downloadFile(String url, File file) {
        if (!file.getParentFile().isDirectory())
            file.getParentFile().mkdirs();
        URLConnection connection = new URI(url).toURL().openConnection();
        String name = url.substring(url.lastIndexOf('/') + 1);
        name = name.length() >= 15 ? name : name + " ".repeat(15 - name.length());
        try (
            InputStream input = ProgressBar.wrap(
                connection.getInputStream(),
                new ProgressBarBuilder()
                    .setTaskName(name)
                    .setInitialMax(connection.getContentLength())
                    .setUnit("KiB", 1024)
                    .showSpeed()
                    .hideEta()
                    .continuousUpdate()
            ); FileOutputStream output = new FileOutputStream(file)
        ) {
            IOUtils.copy(input, output);
        }
    }

    public static void downloadInBatch(Map<String, File> mappings) {
        Set<String> failed = new HashSet<>(mappings.keySet());
        int retry = 0;
        while (!failed.isEmpty() && retry < 5) {
            ExecutorService service = Executors.newCachedThreadPool();
            Set<String> newFailed = new HashSet<>();
            for (String url : failed) {
                File file = mappings.get(url);
                int finalRetry = retry;
                service.submit(() -> {
                    try {
                        downloadFile(url, file);
                    } catch (Exception e) {
                        newFailed.add(url);
                        log.error("Failed to download file: {} (Retry {})", url, finalRetry, e);
                    }
                });
            }
            failed = newFailed;
            service.close();
            retry++;
        }
        if (!failed.isEmpty())
            throw new RuntimeException("Failed to download files: " + failed);
    }
}

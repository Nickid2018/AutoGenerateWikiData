package io.github.nickid2018.genwiki;

import com.google.gson.JsonElement;
import io.github.nickid2018.genwiki.util.JsonUtils;
import io.github.nickid2018.genwiki.util.WebUtils;
import io.github.nickid2018.mcde.format.MappingFormat;
import io.github.nickid2018.mcde.format.MojangMappingFormat;
import io.github.nickid2018.mcde.remapper.FileProcessor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
public class GenerateWikiData {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Please specify the Minecraft version!");
            System.exit(1);
        }
        String version = args[0];
        log.info("Generate Wiki Data, Minecraft Version: {}", version);

        File serverFile = new File(Constants.SERVER_FOLDER, version + ".jar");
        File serverMapping = new File(Constants.MAPPING_FOLDER, version + ".txt");
        if (!serverFile.exists() || !serverMapping.exists()) {
            try {
                log.info("Downloading server jar and mapping...");
                downloadServerJar(version);
            } catch (Exception e) {
                log.error("Failed to download server jar or mapping!", e);
                System.exit(1);
            }
        } else
            log.info("Server jar and mapping already exists, skip downloading.");

        File remappedFile = new File(Constants.REMAPPED_FOLDER, version + ".jar");
        if (!remappedFile.exists()) {
            try {
                log.info("Remapping server jar...");
                doRemap(serverFile, serverMapping, remappedFile);
            } catch (Exception e) {
                log.error("Failed to remap server jar!", e);
                System.exit(1);
            }
        } else
            log.info("Remapped server jar already exists, skip remapping.");
    }

    private static void downloadServerJar(String version) throws Exception {
        if (!Constants.SERVER_FOLDER.exists())
            Constants.SERVER_FOLDER.mkdirs();
        if (!Constants.MAPPING_FOLDER.exists())
            Constants.MAPPING_FOLDER.mkdirs();

        JsonElement versionManifest = WebUtils.getJson(Constants.VERSION_MANIFEST);
        String serverURL = null;
        for (JsonElement element : versionManifest.getAsJsonObject().getAsJsonArray("versions")) {
            String id = JsonUtils.getStringOrElse(element.getAsJsonObject(), "id", "");
            if (id.equalsIgnoreCase(version)) {
                serverURL = JsonUtils.getStringOrNull(element.getAsJsonObject(), "url");
                break;
            }
        }

        if (serverURL == null) {
            log.error("Cannot find server jar for version {}!", version);
            return;
        }

        JsonElement serverManifest = WebUtils.getJson(serverURL);
        String serverDownloadURL = JsonUtils.getStringInPath(serverManifest.getAsJsonObject(), "downloads.server.url")
                .orElseThrow(() -> new IOException("Cannot find server jar download url!"));
        String serverMappingURL = JsonUtils.getStringInPath(serverManifest.getAsJsonObject(),
                        "downloads.server_mappings.url")
                .orElseThrow(() -> new IOException("Cannot find server jar mapping url!"));

        File serverFile = new File(Constants.SERVER_FOLDER, version + ".jar");
        WebUtils.downloadFile(serverDownloadURL, serverFile);
        log.info("Downloaded server jar to {}", serverFile.getAbsolutePath());
        File serverMapping = new File(Constants.MAPPING_FOLDER, version + ".txt");
        WebUtils.downloadFile(serverMappingURL, serverMapping);
        log.info("Downloaded server jar mapping to {}", serverMapping.getAbsolutePath());
    }

    private static void doRemap(File input, File mapping, File output) throws Exception {
        if (!Constants.REMAPPED_FOLDER.exists())
            Constants.REMAPPED_FOLDER.mkdirs();
        MappingFormat format = new MojangMappingFormat(new FileInputStream(mapping));
        FileProcessor.processServer(input, format, output);
        log.info("Remapped server jar to {}", output.getAbsolutePath());
    }
}

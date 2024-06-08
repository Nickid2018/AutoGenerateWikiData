package io.github.nickid2018.genwiki;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.genwiki.util.JsonUtils;
import io.github.nickid2018.genwiki.util.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InitializeEnvironment {

    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest.json";

    public static final File SERVER_FOLDER = new File("servers");
    public static final File CLIENT_FOLDER = new File("clients");
    public static final File ASSETS_FOLDER = new File("assets");
    public static final File INDEXES_FOLDER = new File(ASSETS_FOLDER, "indexes");
    public static final File OBJECTS_FOLDER = new File(ASSETS_FOLDER, "objects");
    public static final File LIBRARIES_FOLDER = new File("libraries");
    public static final File MAPPING_FOLDER = new File("mappings");
    public static final File REMAPPED_FOLDER = new File("remapped");
    public static final File RUNTIME_FOLDER = new File("runtime");

    public static void createDirectories(boolean isClient) {
        if (!SERVER_FOLDER.exists())
            SERVER_FOLDER.mkdirs();
        if (!SERVER_FOLDER.exists() && !isClient)
            SERVER_FOLDER.mkdirs();
        if (!ASSETS_FOLDER.exists() && isClient)
            ASSETS_FOLDER.mkdirs();
        if (!INDEXES_FOLDER.exists() && isClient)
            INDEXES_FOLDER.mkdirs();
        if (!OBJECTS_FOLDER.exists() && isClient)
            OBJECTS_FOLDER.mkdirs();
        if (!CLIENT_FOLDER.exists() && isClient)
            CLIENT_FOLDER.mkdirs();
        if (!LIBRARIES_FOLDER.exists())
            LIBRARIES_FOLDER.mkdirs();
        if (!MAPPING_FOLDER.exists())
            MAPPING_FOLDER.mkdirs();
        if (!REMAPPED_FOLDER.exists())
            REMAPPED_FOLDER.mkdirs();
        if (!RUNTIME_FOLDER.exists())
            RUNTIME_FOLDER.mkdirs();
    }

    public static Map<String, File> downloadBaseFiles(boolean isClient, String version, boolean forceReDownload) throws IOException {
        File mappingFile = new File(MAPPING_FOLDER, version + (isClient ? "-client" : "-server") + ".txt");
        File jarFile = new File(isClient ? CLIENT_FOLDER : SERVER_FOLDER, version + ".jar");
        File jsonFile = new File(MAPPING_FOLDER, version + ".json");

        if (!jsonFile.isFile() || forceReDownload) {
            String versionURL = null;
            for (JsonElement element : WebUtils
                .getJson(VERSION_MANIFEST)
                .getAsJsonObject()
                .getAsJsonArray("versions")) {
                String id = JsonUtils.getStringOrElse(element.getAsJsonObject(), "id", "");
                if (id.equalsIgnoreCase(version)) {
                    versionURL = JsonUtils.getStringOrNull(element.getAsJsonObject(), "url");
                    break;
                }
            }

            if (versionURL == null) {
                log.error("Cannot find url for version {}!", version);
                System.exit(-1);
            }

            log.info("Downloading version manifest for version {}...", version);
            WebUtils.downloadFile(versionURL, jsonFile);
        }

        JsonObject manifest = JsonParser.parseReader(new FileReader(jsonFile)).getAsJsonObject();
        String jarPath = isClient ? "downloads.client.url" : "downloads.server.url";
        String jarSHA1Path = isClient ? "downloads.client.sha1" : "downloads.server.sha1";
        String mappingPath = isClient ? "downloads.client_mappings.url" : "downloads.server_mappings.url";
        String mappingSHA1Path = isClient ? "downloads.client_mappings.sha1" : "downloads.server_mappings.sha1";
        String jarDownload = JsonUtils
            .getStringInPath(manifest, jarPath)
            .orElseThrow(() -> new IOException("Cannot find jar download url!"));
        String jarSHA1 = JsonUtils
            .getStringInPath(manifest, jarSHA1Path)
            .orElseThrow(() -> new IOException("Cannot find jar download sha1!"));
        String mappingURL = JsonUtils
            .getStringInPath(manifest, mappingPath)
            .orElseThrow(() -> new IOException("Cannot find jar mapping url!"));
        String mappingSHA1 = JsonUtils
            .getStringInPath(manifest, mappingSHA1Path)
            .orElseThrow(() -> new IOException("Cannot find jar mapping sha1!"));

        Map<String, File> downloadsMap = new HashMap<>();
        Map<String, File> downloadedData = new HashMap<>();
        downloadedData.put("jar", jarFile);
        downloadedData.put("mapping", mappingFile);

        if (!jarFile.isFile() || forceReDownload) {
            downloadsMap.put(jarDownload, jarFile);
        } else {
            String hash = Hashing.sha1().hashBytes(IOUtils.toByteArray(new FileInputStream(jarFile))).toString().toLowerCase();
            if (!hash.equals(jarSHA1))
                downloadsMap.put(jarDownload, jarFile);
        }

        if (!mappingFile.isFile() || forceReDownload) {
            downloadsMap.put(mappingURL, mappingFile);
        } else {
            String hash = Hashing.sha1().hashBytes(IOUtils.toByteArray(new FileInputStream(mappingFile))).toString().toLowerCase();
            if (!hash.equals(mappingSHA1))
                downloadsMap.put(mappingURL, mappingFile);
        }

        if (isClient) {
            String assetsID = JsonUtils
                .getStringInPath(manifest, "assetIndex.id")
                .orElseThrow(() -> new IOException("Cannot find assets index id!"));
            String assetsIndex = JsonUtils
                .getStringInPath(manifest, "assetsIndex.url")
                .orElseThrow(() -> new IOException("Cannot find assets index url!"));
            String indexSHA1  = JsonUtils
                .getStringInPath(manifest, "assetsIndex.sha1")
                .orElseThrow(() -> new IOException("Cannot find assets index sha1!"));
            File indexFile = new File(INDEXES_FOLDER, assetsID + ".json");
            if (!indexFile.isFile() || forceReDownload) {
                downloadsMap.put(assetsIndex, indexFile);
            } else {
                String hash = Hashing.sha1().hashBytes(IOUtils.toByteArray(new FileInputStream(indexFile))).toString().toLowerCase();
                if (!hash.equals(indexSHA1))
                    downloadsMap.put(assetsIndex, indexFile);
            }
            downloadedData.put("index", indexFile);
        }

        log.info("Downloading base files...");
        WebUtils.downloadInBatch(downloadsMap);

        return downloadedData;
    }

    public static void downloadAssetsIndex(File index) throws IOException {
        JsonObject indexObject = JsonParser.parseReader(new FileReader(index)).getAsJsonObject().getAsJsonObject("objects");
        Map<String, File> collectedFiles = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : indexObject.entrySet()) {
            String hash = entry.getKey();
            String directory = hash.substring(0, 2);
            String path = hash.substring(0, 2) + "/" + hash;
            if (!new File(OBJECTS_FOLDER, directory).isDirectory())
                new File(OBJECTS_FOLDER, directory).mkdirs();
            File file = new File(OBJECTS_FOLDER, path);
            if (!file.isFile()) {
                collectedFiles.put("https://resources.download.minecraft.net/" + path, file);
            } else {
                String fileHash = Hashing.sha1().hashBytes(IOUtils.toByteArray(new FileInputStream(file))).toString().toLowerCase();
                if (!fileHash.equals(hash))
                    collectedFiles.put("https://resources.download.minecraft.net/" + path, file);
            }
        }
        log.info("Downloading assets...");
        WebUtils.downloadInBatch(collectedFiles);
    }
}

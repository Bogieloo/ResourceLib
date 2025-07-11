package gq.arcstudio.resourceLib;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ResourcePackBuilder {

    public static byte[] buildZip(List<ResourceEntry> entries) throws IOException {
        Map<String, MergedEntry> mergedEntries = new HashMap<>();

        for (ResourceEntry entry : entries) {
            MergedEntry existing = mergedEntries.get(entry.virtualPath);
            if (existing != null && isJsonFile(entry.virtualPath)) {
                JsonObject newJson = parseJson(entry.file);
                existing.mergeJson(newJson, entry.force);
            } else {
                mergedEntries.put(entry.virtualPath, new MergedEntry(entry));
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (var e : mergedEntries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(e.getKey());
                zip.putNextEntry(zipEntry);
                zip.write(e.getValue().getBytes());
                zip.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    private static boolean isJsonFile(String path) {
        return path.toLowerCase().endsWith(".json");
    }

    private static JsonObject parseJson(File file) throws IOException {
        try (Reader r = new FileReader(file)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static class MergedEntry {
        private byte[] raw;
        private JsonObject json;

        public MergedEntry(ResourceEntry entry) throws IOException {
            if (isJsonFile(entry.virtualPath)) {
                this.json = parseJson(entry.file);
            } else {
                this.raw = Files.readAllBytes(entry.file.toPath());
            }
        }

        public void mergeJson(JsonObject newJson, boolean forcePlugin) {
            if (json == null) json = newJson;
            else mergeRecursive(json, newJson, forcePlugin);
        }

        public byte[] getBytes() {
            if (json != null) return json.toString().getBytes(StandardCharsets.UTF_8);
            return raw;
        }

        private void mergeRecursive(JsonObject base, JsonObject addition, boolean force) {
            for (var e : addition.entrySet()) {
                if (base.has(e.getKey()) && base.get(e.getKey()).isJsonObject() && e.getValue().isJsonObject()) {
                    mergeRecursive(base.getAsJsonObject(e.getKey()), e.getValue().getAsJsonObject(), force);
                } else if (!base.has(e.getKey()) || force) {
                    base.add(e.getKey(), e.getValue());
                }
            }
        }
    }
}
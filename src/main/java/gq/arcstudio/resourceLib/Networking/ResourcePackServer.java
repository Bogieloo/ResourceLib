package gq.arcstudio.resourceLib.Networking;

import com.sun.net.httpserver.*;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackBuilder;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

public class ResourcePackServer {

    private HttpServer server;
    private byte[] zipBytes;
    private byte[] sha1;

    public void start(int port) throws IOException {
        buildPack();
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/pack.zip", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, zipBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(zipBytes);
            }
        });

        server.createContext("/pack.zip", exchange -> {
            System.out.println("[ResourceLib] Serving resource pack to " + exchange.getRemoteAddress());
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, zipBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(zipBytes);
            }
        });

        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public String getPackUrl(String host, int port) {
        return "http://" + host + ":" + port + "/pack.zip";
    }

    public byte[] getSha1() {
        return sha1;
    }

    public void buildPack() {
        try {
            zipBytes = ResourcePackBuilder.buildZip(ResourcePackManager.getInstance().getRegisteredResources());
            sha1 = MessageDigest.getInstance("SHA-1").digest(zipBytes);
            System.out.println("[ResourceLib] Resource pack built, SHA-1: " + bytesToHex(sha1));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build resource pack", e);
        }
    }
    private String bytesToHex(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

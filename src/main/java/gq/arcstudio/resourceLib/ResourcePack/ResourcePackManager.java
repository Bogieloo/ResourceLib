package gq.arcstudio.resourceLib.ResourcePack;

import gq.arcstudio.resourceLib.ResourceEntry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ResourcePackManager {

    private static final ResourcePackManager instance = new ResourcePackManager();
    public static ResourcePackManager getInstance() {
        return instance;
    }

    private final List<ResourceEntry> resources = new ArrayList<>();

    private boolean locked = false;

    public void lock() {
        locked = true;
    }

    public void registerResource(String virtualPath, File file, boolean force) {
        if (locked) throw new IllegalStateException("ResourcePackManager is locked after build.");
        resources.add(new ResourceEntry(virtualPath, file, force));
    }

    public void registerResourceEntry(JavaPlugin plugin, String resourceFolder, boolean force) {
        try {

            Enumeration<URL> resources = plugin.getClass().getClassLoader().getResources(resourceFolder);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();

                if (url.getProtocol().equals("jar")) {
                    try (JarInputStream jarStream = new JarInputStream(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().openStream())) {
                        JarEntry entry;

                        while ((entry = jarStream.getNextJarEntry()) != null) {
                            if (!entry.isDirectory() && entry.getName().startsWith(resourceFolder + "/")) {
                                String virtualPath = "assets/" + entry.getName().substring(resourceFolder.length() + 1);
                                InputStream is = plugin.getResource(entry.getName());

                                if (is != null) {
                                    File temp = File.createTempFile("reslib_", null);
                                    temp.deleteOnExit();

                                    try (OutputStream os = new FileOutputStream(temp)) {
                                        is.transferTo(os);
                                    }

                                    registerResource(virtualPath, temp, force);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read resources from: " + resourceFolder + " - " + e.getMessage());
        }
    }

    private void addDirectoryResources(File folder, String basePath, boolean force) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            String pathInPack = basePath + file.getName();
            if (file.isDirectory()) {
                addDirectoryResources(file, pathInPack + "/", force);
            } else {
                registerResource(pathInPack, file, force);
            }
        }
    }

    public List<ResourceEntry> getRegisteredResources() {
        return resources;
    }
}
package gq.arcstudio.resourceLib;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public void registerResourceEntry(JavaPlugin plugin, String pluginResourceFolder, boolean force) {
        File pluginFolder = new File(plugin.getDataFolder(), pluginResourceFolder);
        if (!pluginFolder.exists()) {
            plugin.getLogger().warning("Resource folder not found: " + pluginFolder.getAbsolutePath());
            return;
        }

        addDirectoryResources(pluginFolder, "assets/", force);
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
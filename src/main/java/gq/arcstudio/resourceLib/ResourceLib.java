package gq.arcstudio.resourceLib;

import gq.arcstudio.resourceLib.Networking.ResourcePackSender;
import gq.arcstudio.resourceLib.Networking.ResourcePackServer;
import gq.arcstudio.resourceLib.ResourcePack.ResourcePackManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ResourceLib extends JavaPlugin {

    private ResourcePackServer packServer;
    private ResourcePackSender packSender;
    private String localHost;
    private int port = 61234;

    @Override
    public void onEnable() {
        packServer = new ResourcePackServer();
        try {
            packServer.start(port);
        } catch (IOException e) {
            getLogger().severe("Failed to start resource pack server: " + e.getMessage());
            return;
        }

        localHost = Bukkit.getIp();
        if (localHost.isEmpty()) {
            try {
                localHost = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Unable to resolve local IP", e);
            }
        }

        packServer.buildPack();
        ResourcePackManager.getInstance().lock();

        packSender = new ResourcePackSender(packServer, localHost, port);

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Player p = e.getPlayer();
                String address = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";
                String targetHost = address.equals("127.0.0.1") || address.equals("::1") || address.equals(localHost)
                        ? "127.0.0.1" : localHost;

                new ResourcePackSender(packServer, targetHost, port).sendTo(p);
            }
        }, this);
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
    }
}
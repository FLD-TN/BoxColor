package vn.boxpvp.boxcolor.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import vn.boxpvp.boxcolor.BoxColorPlugin;

public final class SessionListener implements Listener {

    private final BoxColorPlugin plugin;

    public SessionListener(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        try {
            plugin.nicks().onJoin(event.getPlayer());
        } catch (Exception e) {
            plugin.report("join", e);
        }
        plugin.cache().load(event.getPlayer(), data -> plugin.colors().refresh(event.getPlayer()));
        plugin.shaderPack().onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        plugin.shaderPack().onStatus(event.getPlayer(), event.getID(), event.getStatus());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.shaderPack().onQuit(event.getPlayer().getUniqueId());
        plugin.prompts().cancel(event.getPlayer().getUniqueId());
        plugin.menus().forget(event.getPlayer().getUniqueId());
        plugin.cache().unload(event.getPlayer().getUniqueId());
    }
}

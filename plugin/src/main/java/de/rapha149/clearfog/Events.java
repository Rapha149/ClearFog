package de.rapha149.clearfog;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(ClearFog.getInstance(), () -> Util.updateViewDistance(event.getPlayer(), true));
    }

    @EventHandler
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        Util.updateViewDistance(event.getPlayer(), true);
    }
}

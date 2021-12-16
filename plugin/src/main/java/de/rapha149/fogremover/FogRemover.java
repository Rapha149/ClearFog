package de.rapha149.fogremover;

import de.rapha149.fogremover.version.VersionWrapper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FogRemover extends JavaPlugin implements Listener {

    private final String HANDLER_NAME = "FogRemover";
    private VersionWrapper WRAPPER;

    @Override
    public void onEnable() {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + version).newInstance();
        } catch (IllegalAccessException | InstantiationException exception) {
            throw new IllegalStateException("Failed to load support for server version " + version, exception);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("FogRemover does not support the server version \"" + version + "\"", exception);
        }

        FileConfiguration config = getConfig();
        config.addDefault("view-distance", 32);
        config.options().copyDefaults(true);
        saveConfig();

        getCommand("fogremoverreload").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Plugin loaded successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        reloadConfig();
        sender.sendMessage("§7Config was reloaded.");
        return true;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        ChannelPipeline pipeline = WRAPPER.getPipeline(event.getPlayer());
        if(pipeline.names().contains(HANDLER_NAME))
            pipeline.remove(HANDLER_NAME);
        pipeline.addAfter("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                try {
                    if(msg.getClass() == WRAPPER.getPacketClass()) {
                        getLogger().info("Test");
                        msg = WRAPPER.replaceViewDistance(msg, getConfig().getInt("view-distance"));
                        pipeline.remove(HANDLER_NAME);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.write(ctx, msg, promise);
            }
        });
    }
}

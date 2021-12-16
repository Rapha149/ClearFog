package de.rapha149.fogremover;

import de.rapha149.fogremover.version.VersionWrapper;
import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FogRemover extends JavaPlugin {

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

        try {
            registerServerChannelHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("fogremoverreload").setExecutor(this);
        getLogger().info("Plugin loaded successfully.");
    }

    private void registerServerChannelHandler() throws NoSuchFieldException, IllegalAccessException {
        ChannelHandler packetHandler = new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                try {
                    if (msg.getClass() == WRAPPER.getPacketClass()) {
                        msg = WRAPPER.replaceViewDistance(msg, getConfig().getInt("view-distance"));
                        ctx.pipeline().remove(HANDLER_NAME);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.write(ctx, msg, promise);
            }
        };
        ChannelHandler packetInit = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.eventLoop().submit(() -> {
                    ChannelPipeline pipeline = channel.pipeline();
                    if (!pipeline.names().contains(HANDLER_NAME)) {
                        pipeline.addLast(HANDLER_NAME, packetHandler);
                    }
                });
            }
        };
        ChannelHandler init = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(packetInit);
            }
        };
        ChannelHandler handler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                ((Channel) msg).pipeline().addFirst(init);
                ctx.fireChannelRead(msg);
            }
        };
        WRAPPER.getServerPipelines().forEach(pipeline -> pipeline.addFirst(handler));
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
}

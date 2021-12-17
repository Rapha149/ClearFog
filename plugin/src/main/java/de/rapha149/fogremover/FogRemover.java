package de.rapha149.fogremover;

import de.rapha149.fogremover.version.VersionWrapper;
import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class FogRemover extends JavaPlugin {

    private static final int DEFAULT_VIEW_DISTANCE = 32;
    private static final String HANDLER_NAME = "FogRemover";
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
        config.addDefault("enabled", true);
        config.addDefault("view-distance", DEFAULT_VIEW_DISTANCE);
        config.addDefault("individual-distances.enabled", false);
        config.addDefault("individual-distances.permission", null);
        config.createSection("individual-distances.players");
        config.options().copyDefaults(true);
        saveConfig();

        try {
            if (config.getBoolean("enabled"))
                registerHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("fog").setExecutor(this);
        getLogger().info("Plugin loaded successfully.");
    }

    @Override
    public void onDisable() {
        try {
            unregisterHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        getLogger().info("Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        switch (command.getName()) {
            case "fogremoverreload":
                reloadConfig();
                try {
                    unregisterHandler();
                    if (getConfig().getBoolean("enabled"))
                        registerHandler();
                    sender.sendMessage("§7Config was reloaded.");
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    sender.sendMessage("§cAn error occured whilst re-registering handlers.");
                }
                break;
            case "setfog":

        }
        return true;
    }

    private void registerHandler() throws NoSuchFieldException, IllegalAccessException {
        ChannelHandler packetInit = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.eventLoop().submit(() -> {
                    ChannelPipeline pipeline = channel.pipeline();
                    getLogger().info("Test 2");
                    if (!pipeline.names().contains(HANDLER_NAME)) {
                        getLogger().info("Test 3");
                        pipeline.addAfter("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {

                            private UUID player;

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                try {
                                    if(msg.getClass() == WRAPPER.getLoginStartPacketClass())
                                        player = WRAPPER.getUUID(msg);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                super.channelRead(ctx, msg);
                            }

                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                try {
                                    if (msg.getClass() == WRAPPER.getLoginPacketClass()) {
                                        getLogger().info("Test 5");

                                        int viewDistance = getConfig().getInt("view-distance");
                                        if(player != null && getConfig().getBoolean("individual-distances.enabled") &&
                                           getConfig().isSet("individual-distances.players." + player)) {
                                            viewDistance = getConfig().getInt("individual-distances.players." + player);
                                        }
                                        msg = WRAPPER.replaceViewDistance(msg, viewDistance);
                                        pipeline.remove(HANDLER_NAME);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                super.write(ctx, msg, promise);
                            }
                        });
                    }
                });
            }
        };
        ChannelHandler init = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                getLogger().info("Test 1");
                channel.pipeline().addLast(packetInit);
            }
        };
        ChannelHandler handler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                getLogger().info("Test 0");
                ((Channel) msg).pipeline().addFirst(init);
                ctx.fireChannelRead(msg);
            }
        };
        WRAPPER.getServerPipelines().forEach(pipeline -> {
            if (pipeline.names().contains(HANDLER_NAME))
                pipeline.remove(HANDLER_NAME);
            pipeline.addFirst(HANDLER_NAME, handler);
        });
    }

    private void unregisterHandler() throws NoSuchFieldException, IllegalAccessException {
        if (WRAPPER != null)
            WRAPPER.getServerPipelines().forEach(pipeline -> {
                if (pipeline.names().contains(HANDLER_NAME))
                    pipeline.remove(HANDLER_NAME);
            });
    }
}

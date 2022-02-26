package de.rapha149.clearfog;

import de.rapha149.clearfog.version.VersionWrapper;
import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class Util {

    private static final String HANDLER_NAME = "ClearFog";
    public static FileConfiguration config;
    public static VersionWrapper WRAPPER;
    private static Map<UUID, Integer> lastViewDistances = new HashMap<>();

    public static int checkViewDistance(int distance) {
        return Math.max(1, distance);
    }

    public static void checkViewDistances() {
        if (config.getBoolean("default.enabled")) {
            int viewDistance = config.getInt("default.view-distance");
            if (viewDistance < 1)
                ClearFog.getInstance().getLogger().warning("The view distance set in the config is invalid. It has to be above or equal to 1.");
        }
        if (config.getBoolean("world.enabled")) {
            config.getConfigurationSection("world.worlds").getKeys(false).forEach(world -> {
                int viewDistance = config.getInt("world.worlds." + world);
                if (viewDistance < 1) {
                    ClearFog.getInstance().getLogger().warning("The world view distance for \"" + world +
                                                               "\" set in the config is invalid. It has to be above or equal to 1.");
                }
            });
        }
        if (config.getBoolean("individual.enabled")) {
            config.getConfigurationSection("individual.players").getKeys(false).forEach(uuid -> {
                int viewDistance = config.getInt("individual.players." + uuid);
                if (viewDistance < 1) {
                    ClearFog.getInstance().getLogger().warning("The individual view distance for \"" + uuid +
                                                               "\" set in the config is invalid. It has to be above or equal to 1.");
                }
            });
        }
    }

    private static int getViewDistance(UUID uuid) {
        if (uuid != null) {
            if (config.getBoolean("individual.enabled") && config.isSet("individual.players." + uuid))
                return config.getInt("individual.players." + uuid);

            Player player = Bukkit.getPlayer(uuid);
            String world;
            if (player != null && config.getBoolean("world.enabled") &&
                config.isSet("world.worlds." + (world = player.getWorld().getName()))) {
                return config.getInt("world.worlds." + world);
            }
        }

        if (config.getBoolean("default.enabled"))
            return config.getInt("default.view-distance");
        return -1;
    }

    public static void updateViewDistance(Player player) {
        updateViewDistances(Arrays.asList(player));
    }

    public static void updateViewDistances() {
        updateViewDistances(Bukkit.getOnlinePlayers());
    }

    public static void updateViewDistances(Collection<? extends Player> players) {
        if (!config.getBoolean("direct-view-distance-updates"))
            return;

        for (Player player : players) {
            int viewDistance = getViewDistance(player.getUniqueId());
            if (viewDistance == -1)
                viewDistance = player.getWorld().getViewDistance();

            if (lastViewDistances.get(player.getUniqueId()) != viewDistance)
                WRAPPER.updateViewDistance(player, viewDistance);
        }
    }

    public static void registerHandler() throws NoSuchFieldException, IllegalAccessException {
        ChannelHandler packetInit = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.eventLoop().submit(() -> {
                    ChannelPipeline pipeline = channel.pipeline();
                    if (!pipeline.names().contains(HANDLER_NAME)) {
                        pipeline.addAfter("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {

                            private UUID player;

                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                try {
                                    Class<?> clazz = msg.getClass();

                                    if (clazz == WRAPPER.getLoginSuccessPacketClass())
                                        player = WRAPPER.getUUIDFromLoginPacket(msg);

                                    if (clazz == WRAPPER.getLoginPlayPacketClass() ||
                                        clazz == WRAPPER.getUpdateViewDistanceClass()) {
                                        int viewDistance = getViewDistance(player);
                                        if (viewDistance != -1)
                                            msg = WRAPPER.replaceViewDistance(msg, checkViewDistance(viewDistance));
                                        lastViewDistances.put(player, WRAPPER.getViewDistanceFromPacket(msg));
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
        WRAPPER.getServerPipelines().forEach(pipeline -> {
            if (pipeline.names().contains(HANDLER_NAME))
                pipeline.remove(HANDLER_NAME);
            pipeline.addFirst(HANDLER_NAME, handler);
        });
    }

    public static void unregisterHandler() throws NoSuchFieldException, IllegalAccessException {
        if (WRAPPER != null) {
            WRAPPER.getServerPipelines().forEach(pipeline -> {
                if (pipeline.names().contains(HANDLER_NAME))
                    pipeline.remove(HANDLER_NAME);
            });
        }
    }
}

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
                ClearFog.getInstance().getLogger().warning("The view distance set in the config is invalid. It has to be between 2 and 32.");
        }
        if (config.getBoolean("individual.enabled")) {
            config.getConfigurationSection("individual.players").getKeys(false).forEach(uuid -> {
                int viewDistance = config.getInt("individual.players." + uuid);
                if (viewDistance < 1)
                    ClearFog.getInstance().getLogger().warning("The individual view distance for " + uuid +
                                                               " set in the config is invalid. It has to be between 2 and 32.");
            });
        }
    }

    private static int getViewDistance(UUID uuid) {
        if (uuid != null && config.getBoolean("individual.enabled") && config.isSet("individual.players." + uuid))
            return config.getInt("individual.players." + uuid);
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

    private static void updateViewDistances(Collection<? extends Player> players) {
        if(!config.getBoolean("direct-view-distance-updates"))
            return;

        for (Player player : players) {
            int viewDistance = getViewDistance(player.getUniqueId());
            if (viewDistance == -1)
                viewDistance = Math.min(32, Math.max(3, Bukkit.getViewDistance()));

            if(lastViewDistances.get(player.getUniqueId()) != viewDistance)
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
                                        lastViewDistances.put(player, WRAPPER.getViewDistanceFromPacket(msg));

                                        int viewDistance = getViewDistance(player);
                                        if (viewDistance != -1)
                                            msg = WRAPPER.replaceViewDistance(msg, checkViewDistance(viewDistance));
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

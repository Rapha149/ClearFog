package de.rapha149.clearfog.version;

import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface VersionWrapper {

    List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException;

    Class<?> getLoginSuccessPacketClass();

    Class<?> getLoginPlayPacketClass();

    Class<?> getUpdateViewDistanceClass();

    UUID getUUIDFromLoginPacket(Object obj);

    int getViewDistanceFromPacket(Object obj);

    Object replaceViewDistance(Object obj, int viewDistance);

    void updateViewDistance(Player player, int viewDistance);
}

package de.rapha149.clearfog.version;

import io.netty.channel.ChannelPipeline;

import java.util.List;
import java.util.UUID;

public interface VersionWrapper {

    List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException;

    Class<?> getLoginSuccessPacketClass();

    Class<?> getLoginPlayPacketClass();

    UUID getUUIDFromLoginPacket(Object obj);

    Object replaceViewDistance(Object obj, int viewDistance);
}

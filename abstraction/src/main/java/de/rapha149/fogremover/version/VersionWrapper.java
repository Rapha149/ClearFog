package de.rapha149.fogremover.version;

import io.netty.channel.ChannelPipeline;

import java.util.List;
import java.util.UUID;

public interface VersionWrapper {

    List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException;

    Class<?> getLoginPacketClass();

    Class<?> getLoginStartPacketClass();

    Object replaceViewDistance(Object obj, int viewDistance);

    UUID getUUID(Object obj);
}

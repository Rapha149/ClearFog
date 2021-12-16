package de.rapha149.fogremover.version;

import io.netty.channel.ChannelPipeline;

import java.util.List;

public interface VersionWrapper {

    List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException;

    Class<?> getPacketClass();

    Object replaceViewDistance(Object obj, int viewDistance);
}

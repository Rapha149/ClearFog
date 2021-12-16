package de.rapha149.fogremover.version;

import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

public interface VersionWrapper {

    ChannelPipeline getPipeline(Player player);

    Class<?> getPacketClass();

    Object replaceViewDistance(Object obj, int viewDistance);
}

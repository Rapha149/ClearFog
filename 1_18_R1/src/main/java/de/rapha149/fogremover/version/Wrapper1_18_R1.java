package de.rapha149.fogremover.version;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Wrapper1_18_R1 implements VersionWrapper {

    @Override
    public ChannelPipeline getPipeline(Player player) {
        return ((CraftPlayer) player).getHandle().b.a.k.pipeline();
    }

    @Override
    public Class<?> getPacketClass() {
        return PacketPlayOutLogin.class;
    }

    @Override
    public Object replaceViewDistance(Object obj, int viewDistance) {
        if(!(obj instanceof PacketPlayOutLogin packet))
            throw new IllegalArgumentException("Object not instance of class " + getPacketClass().getName());

        return new PacketPlayOutLogin(packet.b(), packet.c(), packet.d(), packet.e(), packet.f(), packet.g(),
                packet.h(), packet.i(), packet.j(), packet.k(), packet.l(), viewDistance, packet.n(), packet.o(),
                packet.p(), packet.q());
    }
}

package de.rapha149.fogremover.version;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.login.PacketLoginOutSuccess;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class Wrapper1_18_R1 implements VersionWrapper {

    @Override
    public List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException {
        ServerConnection conn = ((CraftServer) Bukkit.getServer()).getServer().ad();
        Field field = conn.getClass().getDeclaredField("f");
        field.setAccessible(true);
        List<ChannelFuture> channels = (List<ChannelFuture>) field.get(conn);
        field.setAccessible(false);
        return channels.stream().map(ChannelFuture::channel).map(Channel::pipeline).toList();
    }

    @Override
    public Class<?> getLoginSuccessPacketClass() {
        return PacketLoginOutSuccess.class;
    }

    @Override
    public Class<?> getLoginPlayPacketClass() {
        return PacketPlayOutLogin.class;
    }

    @Override
    public UUID getUUIDFromLoginPacket(Object obj) {
        if(!(obj instanceof PacketLoginOutSuccess packet))
            throw new IllegalArgumentException("Object not instance of class " + PacketLoginOutSuccess.class.getName());

        return packet.b().getId();
    }

    @Override
    public Object replaceViewDistance(Object obj, int viewDistance) {
        if(!(obj instanceof PacketPlayOutLogin packet))
            throw new IllegalArgumentException("Object not instance of class " + PacketPlayOutLogin.class.getName());

        return new PacketPlayOutLogin(packet.b(), packet.c(), packet.d(), packet.e(), packet.f(), packet.g(),
                packet.h(), packet.i(), packet.j(), packet.k(), viewDistance, packet.m(), packet.n(), packet.o(),
                packet.p(), packet.q());
    }
}

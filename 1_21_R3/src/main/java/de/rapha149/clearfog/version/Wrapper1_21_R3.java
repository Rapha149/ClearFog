package de.rapha149.clearfog.version;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.game.PacketPlayOutViewDistance;
import net.minecraft.network.protocol.login.PacketLoginOutSuccess;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class Wrapper1_21_R3 implements VersionWrapper {

    @Override
    public List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException {
        ServerConnection conn = ((CraftServer) Bukkit.getServer()).getServer().ah();
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
    public Class<?> getUpdateViewDistanceClass() {
        return PacketPlayOutViewDistance.class;
    }

    @Override
    public UUID getUUIDFromLoginPacket(Object obj) {
        if (!(obj instanceof PacketLoginOutSuccess packet))
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + PacketLoginOutSuccess.class.getName());

        return packet.b().getId();
    }

    @Override
    public int getViewDistanceFromPacket(Object obj) {
        if (obj instanceof PacketPlayOutLogin packet)
            return packet.h();
        else if (obj instanceof PacketPlayOutViewDistance packet)
            return packet.b();
        else {
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + PacketPlayOutLogin.class.getName() +
                    " nor instance of class " + PacketPlayOutViewDistance.class.getName());
        }
    }

    @Override
    public Object replaceViewDistance(Object obj, int viewDistance) {
        if (obj instanceof PacketPlayOutLogin packet) {
            return new PacketPlayOutLogin(packet.b(), packet.e(), packet.f(), packet.g(), viewDistance,
                    packet.i(), packet.j(), packet.k(), packet.l(), packet.m(), packet.n());
        } else if (obj instanceof PacketPlayOutViewDistance) {
            return new PacketPlayOutViewDistance(viewDistance);
        } else {
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + PacketPlayOutLogin.class.getName() +
                    " nor instance of class " + PacketPlayOutViewDistance.class.getName());
        }
    }

    @Override
    public void updateViewDistance(Player player, int viewDistance, boolean directUpdate) {
        EntityPlayer p = ((CraftPlayer) player).getHandle();
        p.f.b(new PacketPlayOutViewDistance(viewDistance));

        if (directUpdate) {
            PlayerChunkMap map = p.y().m().a;
            Location loc = player.getLocation();
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();

            p.f(x + 1000, y, z + 1000);
            map.a(p);
            p.f(x, y, z);
            map.a(p);
        }
    }
}

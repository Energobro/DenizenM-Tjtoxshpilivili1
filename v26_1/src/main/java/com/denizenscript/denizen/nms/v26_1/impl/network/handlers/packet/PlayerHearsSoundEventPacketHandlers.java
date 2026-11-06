package com.denizenscript.denizen.nms.v26_1.impl.network.handlers.packet;

import com.denizenscript.denizen.events.player.PlayerHearsSoundScriptEvent;
import com.denizenscript.denizen.nms.v26_1.impl.network.handlers.DenizenNetworkManagerImpl;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;

public class PlayerHearsSoundEventPacketHandlers {

    public static void registerHandlers() {
        DenizenNetworkManagerImpl.registerPacketHandler(ClientboundSoundPacket.class, PlayerHearsSoundEventPacketHandlers::processSoundPacket);
        DenizenNetworkManagerImpl.registerPacketHandler(ClientboundSoundEntityPacket.class, PlayerHearsSoundEventPacketHandlers::processSoundPacket);
    }

    public static Packet<ClientGamePacketListener> processSoundPacket(DenizenNetworkManagerImpl networkManager, Packet<ClientGamePacketListener> packet) {
        if (!PlayerHearsSoundScriptEvent.instance.eventData.isEnabled) {
            return packet;
        }
        if (!DenizenCore.isMainThread() && (PlayerHearsSoundScriptEvent.instance.threadSplit == null || !(packet instanceof ClientboundSoundPacket))) {
            Packet<ClientGamePacketListener>[] result = new Packet[] {packet};
            try {
                DenizenCore.runOnMainThreadAndWait(() -> result[0] = processSoundPacket(networkManager, packet));
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
            return result[0];
        }
        if (packet instanceof ClientboundSoundPacket) {
            ClientboundSoundPacket spacket = (ClientboundSoundPacket) packet;
            return PlayerHearsSoundScriptEvent.instance.run(networkManager.player.getBukkitEntity(), spacket.getSound().value().location().getPath(), spacket.getSource().name(),
                    false, null, new Location(networkManager.player.getBukkitEntity().getWorld(), spacket.getX(), spacket.getY(), spacket.getZ()), spacket.getVolume(), spacket.getPitch()) ? null : packet;
        }
        else if (packet instanceof ClientboundSoundEntityPacket) {
            ClientboundSoundEntityPacket spacket = (ClientboundSoundEntityPacket) packet;
            Entity entity = networkManager.player.level().getEntity(spacket.getId());
            if (entity == null) {
                return packet;
            }
            return PlayerHearsSoundScriptEvent.instance.run(networkManager.player.getBukkitEntity(), spacket.getSound().value().location().getPath(), spacket.getSource().name(),
                    false, entity.getBukkitEntity(), null, spacket.getVolume(), spacket.getPitch()) ? null : packet;
        }
        return packet;
    }
}

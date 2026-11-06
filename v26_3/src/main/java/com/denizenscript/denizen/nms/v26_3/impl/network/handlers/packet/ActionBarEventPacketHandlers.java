package com.denizenscript.denizen.nms.v26_3.impl.network.handlers.packet;

import com.denizenscript.denizen.events.player.PlayerReceivesActionbarScriptEvent;
import com.denizenscript.denizen.nms.v26_3.impl.network.handlers.DenizenNetworkManagerImpl;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.bukkit.craftbukkit.util.CraftChatMessage;

public class ActionBarEventPacketHandlers {

    public static void registerHandlers() {
        DenizenNetworkManagerImpl.registerPacketHandler(ClientboundSetActionBarTextPacket.class, ActionBarEventPacketHandlers::processActionbarPacket);
    }

    public static ClientboundSetActionBarTextPacket processActionbarPacket(DenizenNetworkManagerImpl networkManager, ClientboundSetActionBarTextPacket actionbarPacket) {
        PlayerReceivesActionbarScriptEvent event = PlayerReceivesActionbarScriptEvent.instance;
        if (!event.loaded) {
            return actionbarPacket;
        }
        String rawJson = CraftChatMessage.toJSON(actionbarPacket.text());
        try {
            event = (PlayerReceivesActionbarScriptEvent) event.fireFor(networkManager.player.getBukkitEntity(), PaperAPITools.instance.parseJsonToText(rawJson), rawJson, false);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return actionbarPacket;
        }
        if (event.cancelled) {
            return null;
        }
        if (event.modified) {
            return new ClientboundSetActionBarTextPacket(CraftChatMessage.fromJSON(event.rawJson.asString()));
        }
        return actionbarPacket;
    }
}

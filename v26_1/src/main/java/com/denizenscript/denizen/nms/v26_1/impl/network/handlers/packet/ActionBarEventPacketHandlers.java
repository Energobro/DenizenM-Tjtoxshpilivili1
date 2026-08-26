package com.denizenscript.denizen.nms.v26_1.impl.network.handlers.packet;

import com.denizenscript.denizen.events.player.PlayerReceivesActionbarScriptEvent;
import com.denizenscript.denizen.nms.v26_1.impl.network.handlers.DenizenNetworkManagerImpl;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
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
        if (!DenizenCore.isMainThread()) {
            // A packet handler runs on whichever thread sent the packet, and the actionbar command is async-safe now, so that can be a script's own thread.
            // Firing the event from there would be wrong twice over: the event object is a single shared instance whose fields are filled in
            // immediately before it fires, and what it fires are ordinary scripts. So the whole thing goes to the main thread and this side waits.
            // The wait only ever happens on a server that actually handles this event - without one loaded, the check above returns first.
            ClientboundSetActionBarTextPacket[] result = new ClientboundSetActionBarTextPacket[] {actionbarPacket};
            try {
                DenizenCore.runOnMainThreadAndWait(() -> result[0] = processActionbarPacket(networkManager, actionbarPacket));
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
            return result[0];
        }
        event.reset();
        String rawJson = CraftChatMessage.toJSON(actionbarPacket.text());
        event.message = new ElementTag(PaperAPITools.instance.parseJsonToText(rawJson), true);
        event.rawJson = new ElementTag(rawJson, true);
        event.system = new ElementTag(false);
        event.player = PlayerTag.mirrorBukkitPlayer(networkManager.player.getBukkitEntity());
        event = (PlayerReceivesActionbarScriptEvent) event.triggerNow();
        if (event.cancelled) {
            return null;
        }
        if (event.modified) {
            return new ClientboundSetActionBarTextPacket(CraftChatMessage.fromJSON(event.rawJson.asString()));
        }
        return actionbarPacket;
    }
}

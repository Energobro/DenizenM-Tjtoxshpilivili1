package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.utilities.packets.NetworkInterceptHelper;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.entity.Player;

public class PlayerReceivesMessageScriptEvent extends BukkitScriptEvent {

    // <--[event]
    // @Events
    // player receives message
    //
    // @Regex ^on player receives message$
    //
    // @Group Player
    //
    // @Cancellable true
    //
    // @Warning Using this will forcibly sync the chat thread, unless every path uses the 'thread:network' switch.
    //
    // @Switch thread:network to run the path right away on the thread that sends the message, without waiting for the main thread. See <@link language Network Thread Events>.
    //
    // @Triggers when a player receives any chat message from the server. This does not normally include *player* chat, instead prefer <@link event player chats> for that.
    //
    // @Context
    // <context.message> returns an ElementTag of the message.
    // <context.raw_json> returns an ElementTag of the raw JSON used for the message.
    // <context.system_message> returns true if the message is a system message (not player chat).
    //
    // @Determine
    // "MESSAGE:<ElementTag>" to change the message.
    // "RAW_JSON:<ElementTag>" to change the JSON used for the message.
    //
    // @Player Always.
    //
    // -->

    public PlayerReceivesMessageScriptEvent() {
        instance = this;
        registerSwitches("thread");
    }

    public static PlayerReceivesMessageScriptEvent instance;
    public ElementTag message;
    public ElementTag rawJson;
    public boolean didModify;
    public ElementTag system;
    public boolean modified;
    public PlayerTag player;
    public boolean loaded;

    public void reset() {
        player = null;
        message = null;
        rawJson = null;
        system = null;
        cancelled = false;
        modified = false;
        didModify = false;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player receives message");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!threadMatches(path)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public void init() {
        NetworkInterceptHelper.enable();
        buildThreadSplit();
        loaded = true;
    }

    @Override
    public void destroy() {
        loaded = false;
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag) {
            String determination = determinationObj.toString();
            String lower = CoreUtilities.toLowerCase(determination);
            if (lower.startsWith("message:")) {
                message = new ElementTag(determination.substring("message:".length()), true);
                rawJson = new ElementTag(PaperAPITools.instance.parseTextToJson(message.asString(), PaperAPITools.BaseColor.WHITE), true);
                modified = true;
                return true;
            }
            if (lower.startsWith("raw_json:")) {
                rawJson = new ElementTag(determination.substring("raw_json:".length()), true);
                message = new ElementTag(PaperAPITools.instance.parseJsonToText(rawJson.asString()), true);
                modified = true;
                return true;
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "message": return message;
            case "system_message": return system;
            case "raw_json": return rawJson;
        }
        return super.getContext(name);
    }

    public PlayerReceivesMessageScriptEvent triggerNow() {
        return (PlayerReceivesMessageScriptEvent) fire();
    }

    public void fill(Player target, String text, String json, boolean isSystem) {
        reset();
        player = new PlayerTag(target);
        message = new ElementTag(text, true);
        rawJson = new ElementTag(json, true);
        system = new ElementTag(isSystem);
    }

    public PlayerReceivesMessageScriptEvent fireFor(Player target, String text, String json, boolean isSystem) {
        if (DenizenCore.isMainThread()) {
            fill(target, text, json, isSystem);
            return triggerNow();
        }
        ThreadSplit split = threadSplit;
        if (split != null) {
            PlayerReceivesMessageScriptEvent copy = (PlayerReceivesMessageScriptEvent) clone();
            copy.fill(target, text, json, isSystem);
            return (PlayerReceivesMessageScriptEvent) copy.fireSplitAndWait(split);
        }
        PlayerReceivesMessageScriptEvent[] result = new PlayerReceivesMessageScriptEvent[1];
        DenizenCore.runOnMainThreadAndWait(() -> result[0] = fireFor(target, text, json, isSystem));
        return result[0];
    }
}
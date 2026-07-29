package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinsScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player joins
    // player join
    //
    // @Regex ^on player (joins|join)$
    //
    // @Location true
    //
    // @Group Player
    //
    // @Triggers when a player joins the server.
    //
    // @Context
    // <context.message> returns an ElementTag of the join message.
    //
    // @Determine
    // ElementTag to change the join message.
    // "NONE" to cancel the join message.
    //
    // @Player Always.
    //
    // -->

    public PlayerJoinsScriptEvent() {
        this.<PlayerJoinsScriptEvent>registerTextDetermination("none", (evt) -> {
            event.setJoinMessage(null);
        });
        this.<PlayerJoinsScriptEvent, ElementTag>registerDetermination(null, ElementTag.class, (evt, context, determination) -> {
            event.setJoinMessage(determination.asString());
        });
    }

    public PlayerJoinEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player join");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, event.getPlayer().getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("message")) {
            return new ElementTag(event.getJoinMessage());
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerJoins(PlayerJoinEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        this.event = event;
        fire(event);
    }
}
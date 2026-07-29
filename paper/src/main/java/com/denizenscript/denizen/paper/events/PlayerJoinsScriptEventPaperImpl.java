package com.denizenscript.denizen.paper.events;

import com.denizenscript.denizen.events.player.PlayerJoinsScriptEvent;
import com.denizenscript.denizen.paper.utilities.FormattedTextHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerJoinsScriptEventPaperImpl extends PlayerJoinsScriptEvent {

    public PlayerJoinsScriptEventPaperImpl() {
        this.<PlayerJoinsScriptEventPaperImpl>registerTextDetermination("none", (evt) -> {
            event.joinMessage(null);
        });
        this.<PlayerJoinsScriptEventPaperImpl, ElementTag>registerDetermination(null, ElementTag.class, (evt, context, determination) -> {
            event.joinMessage(FormattedTextHelper.parse(determination.asString(), NamedTextColor.WHITE));
        });
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "message" -> new ElementTag(FormattedTextHelper.stringify(event.joinMessage()), true);
            default -> super.getContext(name);
        };
    }
}
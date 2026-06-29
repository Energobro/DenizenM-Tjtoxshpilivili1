package com.denizenscript.denizen.paper.events;

import com.denizenscript.denizen.events.player.PlayerItemTakesDamageScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class PlayerItemTakesDamageScriptEventPaperImpl extends PlayerItemTakesDamageScriptEvent {

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "original_damage": return new ElementTag(event.getOriginalDamage());
        }
        return super.getContext(name);
    }
}

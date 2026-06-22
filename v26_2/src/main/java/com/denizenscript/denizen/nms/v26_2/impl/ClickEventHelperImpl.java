package com.denizenscript.denizen.nms.v26_2.impl;

import com.denizenscript.denizen.paper.utilities.ClickEventHelper;
import net.kyori.adventure.text.event.ClickEvent;

public class ClickEventHelperImpl extends ClickEventHelper {

    @Override
    public String getValue(ClickEvent click) {
        if (click.payload() instanceof ClickEvent.Payload.Text text) {
            return text.value();
        }
        return "";
    }

    @Override
    public ClickEvent createSuggestCommand(String value) {
        return ClickEvent.suggestCommand(value);
    }

    @Override
    public ClickEvent createClickEvent(ClickEvent.Action action, String value) {
        return ClickEvent.suggestCommand(value);
    }
}
package com.denizenscript.denizen.nms.v1_21.impl;

import com.denizenscript.denizen.paper.utilities.ClickEventHelper;
import net.kyori.adventure.text.event.ClickEvent;

public class ClickEventHelperImpl extends ClickEventHelper {

    @Override
    public String getValue(ClickEvent click) {
        return click.value();
    }

    @Override
    public ClickEvent createSuggestCommand(String value) {
        return ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, value);
    }

    @Override
    public ClickEvent createClickEvent(ClickEvent.Action action, String value) {
        return ClickEvent.clickEvent(action, value);
    }
}
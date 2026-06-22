package com.denizenscript.denizen.paper.utilities;

import net.kyori.adventure.text.event.ClickEvent;

public abstract class ClickEventHelper {

    public static ClickEventHelper instance;

    public abstract String getValue(ClickEvent click);
    public abstract ClickEvent createSuggestCommand(String value);
    public abstract ClickEvent createClickEvent(ClickEvent.Action action, String value);
}
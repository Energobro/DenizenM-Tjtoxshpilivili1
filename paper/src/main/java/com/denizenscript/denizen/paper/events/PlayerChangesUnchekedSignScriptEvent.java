package com.denizenscript.denizen.paper.events;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerChangesUnchekedSignScriptEvent extends BukkitScriptEvent implements Listener {

    public PlayerChangesUnchekedSignScriptEvent() {
        registerCouldMatcher("player unchecked sign edits");
    }

    public UncheckedSignChangeEvent event;

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
        return switch (name) {
            case "location" -> {
                int x = (int) event.getEditedBlockPosition().x();
                int y = (int) event.getEditedBlockPosition().y();
                int z = (int) event.getEditedBlockPosition().z();

                yield new LocationTag(event.getPlayer().getWorld(), x, y, z);
            }
            case "side" -> new ElementTag(event.getSide());
            case "lines" -> {
                ListTag lines = new ListTag();
                for (Component line : event.lines()) {
                    String legacyText = LegacyComponentSerializer.legacySection().serialize(line);
                    lines.addObject(new ElementTag(legacyText));
                }
                yield lines;
            }
            default -> super.getContext(name);
        };
    }

    @EventHandler
    public void onPlayerUncheckedSignChange(UncheckedSignChangeEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        this.event = event;
        fire(event);
    }
}

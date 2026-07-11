package com.denizenscript.denizen.paper.events;

import com.denizenscript.denizen.events.server.ListPingScriptEvent;
import com.denizenscript.denizen.nms.abstracts.ProfileEditor;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.paper.utilities.FormattedTextHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;

import java.util.*;

public class ServerListPingScriptEventPaperImpl extends ListPingScriptEvent {

    public ServerListPingScriptEventPaperImpl() {
        this.<ServerListPingScriptEventPaperImpl, ElementTag>registerOptionalDetermination("protocol_version", ElementTag.class, (evt, context, version) -> {
            if (version.isInt()) {
                evt.getEvent().setProtocolVersion(version.asInt());
                return true;
            }
            return false;
        });
        this.<ServerListPingScriptEventPaperImpl, ElementTag>registerDetermination("version_name", ElementTag.class, (evt, context, name) -> {
            evt.getEvent().setVersion(name.asString());
        });
        this.<ServerListPingScriptEventPaperImpl, ListTag>registerDetermination("exclude_players", ListTag.class, (evt, context, list) -> {
            HashSet<UUID> exclusions = new HashSet<>();
            for (PlayerTag player : list.filter(PlayerTag.class, context)) {
                exclusions.add(player.getUUID());
            }
            ListedPlayersEditor.excludeListedPlayers(evt.getEvent(), exclusions);
        });
        this.<ServerListPingScriptEventPaperImpl, ListTag>registerOptionalDetermination("alternate_player_text", ListTag.class, (evt, context, text) -> {
            if (!CoreConfiguration.allowRestrictedActions) {
                Debug.echoError("Cannot use 'alternate_player_text' in list ping event: 'Allow restricted actions' is disabled in Denizen config.yml.");
                return false;
            }
            ListedPlayersEditor.setListedPlayerInfo(evt.getEvent(), text);
            return true;
        });
    }

    public PaperServerListPingEvent getEvent() {
        return (PaperServerListPingEvent) event;
    }

    // TODO: workaround for Java trying to load ListedPlayerInfo on old versions, remove once 1.20 is the minimum supported version
    public static class ListedPlayersEditor {
        public static void setListedPlayerInfo(PaperServerListPingEvent event, List<String> lines) {
            event.getListedPlayers().clear();
            for (String line : lines) {
                event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo(line, ProfileEditor.NIL_UUID));
            }
        }

        public static void excludeListedPlayers(PaperServerListPingEvent event, Set<UUID> exclude) {
            event.getListedPlayers().removeIf(listedPlayerInfo -> exclude.contains(listedPlayerInfo.id()));
        }
    }

    @Override
    public void setMotd(String text) {
        event.motd(FormattedTextHelper.parse(text, NamedTextColor.WHITE));
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "motd" -> new ElementTag(FormattedTextHelper.stringify(event.motd()), true);
            case "protocol_version" -> new ElementTag(getEvent().getProtocolVersion());
            case "version_name" -> new ElementTag(getEvent().getVersion(), true);
            case "client_protocol_version" -> new ElementTag(getEvent().getClient().getProtocolVersion());
            default -> super.getContext(name);
        };
    }

    @EventHandler
    public void onListPing(PaperServerListPingEvent event) {
        syncFire(event);
    }
}
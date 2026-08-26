package com.denizenscript.denizen.tags.core;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.Settings;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.tags.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTagBase implements Listener {

    public PlayerTagBase() {

        // <--[tag]
        // @attribute <player[(<player>)]>
        // @returns PlayerTag
        // @description
        // Returns a player object constructed from the input value.
        // Refer to <@link objecttype PlayerTag>.
        // If no input value is specified, returns the linked player.
        // -->
        Bukkit.getServer().getPluginManager().registerEvents(this, Denizen.getInstance());
        TagManager.registerTagHandler(PlayerTag.class, "player", (attribute) -> {
            if (!attribute.hasParam()) {
                PlayerTag player = ((BukkitTagContext) attribute.context).player;
                if (player != null) {
                    return player;
                }
                else {
                    attribute.echoError("Missing player for player tag.");
                    return null;
                }
            }
            return PlayerTag.valueOf(attribute.getParam(), attribute.context);
        });
    }

    ///////////
    // Player Chat History
    /////////

    /**
     * The last few things each player said.
     * <p>
     * Concurrent, and the lists in it are replaced rather than edited, because async queues read this through
     * "<player.chat_history>" while the main thread adds to it on every chat message.
     */
    public static Map<UUID, List<String>> playerChatHistory = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void addMessage(final AsyncPlayerChatEvent event) {
        final int maxSize = Settings.chatHistoryMaxMessages();
        if (maxSize > 0) {
            Bukkit.getScheduler().runTaskLater(Denizen.getInstance(), () -> {
                List<String> old = playerChatHistory.get(event.getPlayer().getUniqueId());
                // Copied first, then edited, rather than editing the stored list in place: a script reading this player's history off an
                // async queue's thread would otherwise be walking a list being added to and removed from underneath it.
                // This way a reader holds either the whole old history or the whole new one. It is at most a handful of strings.
                // The trimming below is exactly what it was before - deliberately unchanged, odd as it looks.
                List<String> history = old == null ? new ArrayList<>() : new ArrayList<>(old);
                // Maximum history size is specified by config.yml
                if (history.size() > maxSize) {
                    history.remove(maxSize - 1);
                }
                // Add message to history
                history.add(0, event.getMessage());
                // Store the new history
                playerChatHistory.put(event.getPlayer().getUniqueId(), history);
            }, 1);
        }
    }
}


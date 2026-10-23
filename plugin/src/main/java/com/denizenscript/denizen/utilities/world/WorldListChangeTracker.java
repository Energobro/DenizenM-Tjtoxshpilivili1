package com.denizenscript.denizen.utilities.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.UUID;

public class WorldListChangeTracker implements Listener {

    public static int changes;

    public static volatile HashMap<UUID, String> worldNamesById = new HashMap<>();

    public static String nameOf(UUID worldId) {
        return worldId == null ? null : worldNamesById.get(worldId);
    }

    public static void rebuildNames(World excluded) {
        HashMap<UUID, String> replacement = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            if (world != excluded) {
                replacement.put(world.getUID(), world.getName());
            }
        }
        worldNamesById = replacement;
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent event) {
        changes++;
        rebuildNames(null);
    }

    @EventHandler
    public void onWorldUnloaded(WorldUnloadEvent event) {
        changes++;
        rebuildNames(event.getWorld());
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        changes++;
        rebuildNames(null);
    }
}

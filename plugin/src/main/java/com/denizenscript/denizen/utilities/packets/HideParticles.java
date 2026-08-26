package com.denizenscript.denizen.utilities.packets;

import org.bukkit.Particle;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HideParticles {

    /**
     * Which particles each player has hidden.
     * Read by the particle packet handler, which runs on whatever thread sent the packet - that used to mean the main thread and nothing else,
     * but 'playeffect' naming its targets is async-safe now, so an async script sends particles from its own thread.
     * The per-player set is concurrent for the same reason: the hide_particles mechanism adds to it in place on the main thread,
     * which a reader on another thread could otherwise be walking.
     */
    public static Map<UUID, Set<Particle>> hidden = new ConcurrentHashMap<>();
}

package com.denizenscript.denizen.utilities;

import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.objects.properties.bukkit.BukkitColorExtensions;
import com.denizenscript.denizen.tags.core.*;
import com.denizenscript.denizencore.objects.ObjectType;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.depends.Depends;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class CommonRegistries {

    // <--[language]
    // @name ObjectTags
    // @group Object System
    // @description
    // ObjectTags are a system put into place by Denizen that make working with things, or 'objects',
    // in Minecraft and Denizen easier. Many parts of scripts will require some kind of object as an
    // argument, identifier/type, or such as in world events, part of an event name. The ObjectTags notation
    // system helps both you and Denizen know what type of objects are being referenced and worked with.
    //
    // So when should you use ObjectTags? In arguments, event names, replaceable tags, configs, flags, and
    // more! If you're just a beginner, you've probably been using them without even realizing it!
    //
    // ObjectTag is a broader term for a 'type' of object that more specifically represents something,
    // such as a LocationTag or ScriptTag, often times just referred to as a 'location' or 'script'. Denizen
    // employs many object types that you should be familiar with. You'll notice that many times objects
    // are referenced with their 'ObjectTag notation' which is in the format of 'x@', the x being the specific
    // notation of an object type. Example: player objects use the p@ notation, and locations use l@.
    // This notation is automatically generated when directly displaying objects, or saving them into data files.
    // It should never be manually typed into a script.
    //
    // Let's take the tag system, for example. It uses the ObjectTags system pretty heavily. For instance,
    // every time you use <player.name> or <npc.id>, you're using a ObjectTag, which brings us to a simple
    // clarification: Why <player.name> and not <PlayerTag.name>? That's because Denizen allows Players,
    // NPCs and other 'in-context objects' to be linked to certain scripts. In short, <player> already
    // contains a reference to a specific player, such as the player that died in a world event 'on player dies'.
    // <PlayerTag.name> is instead the format for documentation, with "PlayerTag" simply indicating 'any player object here'.
    //
    // ObjectTags can be used to CREATE new instances of objects, too! Though not all types allow 'new'
    // objects to be created, many do, such as ItemTags. With the use of tags, it's easy to reference a specific
    // item, say -- an item in the Player's hand -- items are also able to use a constructor to make a new item,
    // and say, drop it in the world. Take the case of the command/usage '- drop diamond_ore'. The item object
    // used is a brand new diamond_ore, which is then dropped by the command to a location of your choice -- just
    // specify an additional location argument.
    //
    // There's a great deal more to learn about ObjectTags, so be sure to check out each object type for more
    // specific information. While all ObjectTags share some features, many contain goodies on top of that!
    // -->

    // <--[language]
    // @name Tick
    // @group Common Terminology
    // @description
    // A 'tick' is usually referred to as 1/20th of a second, the speed at which Minecraft servers update
    // and process everything on them.
    // -->

    public static void registerMainTagHandlers() {
        // Objects
        if (Depends.citizens != null) {
            new NPCTagBase();
        }
        new PlayerTagBase();
        // Other bases
        new CustomColorTagBase();
        new ServerTagBase();
        new TextTagBase();
        registerAsyncTagSafety();
    }

    /**
     * Names of the flag tags, which read plain saved data and so are safe to read off the main thread
     * (their storage is thread-safe) even when the object type they're on is not.
     */
    public static final String[] FLAG_TAGS = new String[] { "flag", "has_flag", "flag_expiration", "flag_map", "list_flags" };

    /** The given async-safe tag names plus {@link #FLAG_TAGS}, for a type or base whose flags are safe to read off-thread alongside them. */
    public static String[] withFlagTags(String... tagNames) {
        String[] combined = Arrays.copyOf(FLAG_TAGS, FLAG_TAGS.length + tagNames.length);
        System.arraycopy(tagNames, 0, combined, FLAG_TAGS.length, tagNames.length);
        return combined;
    }

    /**
     * Server tags that read data fixed at startup - a registry, an enum, or a server property out of server.properties - rather than anything
     * that changes as the server runs, and so are safe to read off the main thread.
     * <p>
     * Two things this list is deliberately careful about. First, being registered with {@code registerStaticTag} does not make a tag safe:
     * 'biome_types' is static and still excluded, because it builds a BiomeTag per entry and that constructor calls Bukkit.getWorlds()
     * (see BiomeTag:104). Second, only the modern name of each tag is listed - the deprecated 'list_*' aliases are separate registrations,
     * and leaving them main-thread-only keeps the deprecation warning path off async threads entirely.
     * <p>
     * Accepted caveat: registries are rebuilt by a datapack reload, so a script reading one of the list tags at the exact moment an admin
     * reloads datapacks races with that rebuild. Same shape as the world list caveat on LocationTag, and about as rare.
     * <p>
     * Two tags that look like they belong here and don't, both because the table behind them is a plain HashMap that is mutated in place
     * rather than swapped: the 'vanilla_*_tags' family reads VanillaTagHelper's maps, which {@code loadTagsCache} clears and refills on a
     * tag reload, and 'current_bossbars' reads BossBarCommand's map, which the bossbar command rewrites while the server runs.
     * A registry being rebuilt swaps a reference; a HashMap being cleared under a reader can hand back garbage.
     */
    /**
     * PlayerTag sub-tags that read nothing live, so they cost nothing off-thread.
     * Used for the '<player...>' tag base and for the PlayerTag type alike - a player reached through the base and the same player reached
     * through a definition run the very same tag code, so a name free one way and costly the other is nothing but a trap. What each of these
     * is doing instead of reading the live player is written out at the type marking further down.
     */
    public static final String[] PLAYER_ASYNC_SAFE_TAGS = new String[] {
            "uuid", "is_online", "name", "is_player",
            "is_op", "whitelisted", "is_whitelisted", "chat_history", "chat_history_list",
            "is_banned", "ban_expiration_time", "ban_expiration", "ban_reason", "ban_created_time", "ban_created", "ban_source", "ban_info",
            "has_played_before", "first_played_time", "first_played", "last_played_time", "last_played",
            "fake_block", "fake_block_locations", "fake_entities", "disguise_to_self",
            "sidebar_lines", "sidebar_title", "sidebar_scores" };


    /**
     * World tags that read a plain field off the level rather than going and gathering anything, so they are safe off the main thread.
     * All 60 of WorldTag's tags were read for this; what is missing from the list is missing on purpose, and the three reasons are below.
     * <p>
     * The first group cannot change at all while the world exists - the seed, the environment, the world type, the sea level and height
     * limits, hardcore, whether structures generate. Reading those off-thread has no staleness question in it whatsoever.
     * <p>
     * The second group is settings: pvp, animals and monsters, autosave, keep-spawn, the four spawn limits, the two spawn intervals,
     * difficulty, view and simulation distance. They change only when something changes them, and every one is an int, a boolean or an enum
     * read straight off the level data, so a reader gets the old value or the new one and never a broken one.
     * <p>
     * The third group is time and weather, which change on their own every tick, so any answer is a snapshot by nature - the same argument
     * that freed ChunkTag.is_loaded, and the reason it does not apply to flags is that a flag holds what the script itself put there.
     * <p>
     * Left out, and each for its own reason: everything that gathers a collection (entities, living_entities, players, spawned_npcs, npcs,
     * loaded_chunks, biomes) walks live server structures; the border family reads a border object that may be mid-lerp; gamerule and
     * gamerule_map go into the level's rule store; the dragon family (ender_dragon, dragon_portal_location, gateway_locations,
     * first_dragon_killed) touches the end fight's live state; enough_sleeping and enough_deep_sleeping count players; and spawn_location
     * is left alone because it hands back a LocationTag, which is judged by its own type marking anyway.
     */
    public static final String[] WORLD_ASYNC_SAFE_TAGS = new String[] {
            "name", "seed", "environment", "world_type", "sea_level", "max_height", "min_height", "hardcore", "can_generate_structures",
            "allows_animals", "allows_monsters", "allows_pvp", "auto_save", "keep_spawn",
            "ambient_spawn_limit", "animal_spawn_limit", "monster_spawn_limit", "water_animal_spawn_limit",
            "ticks_per_animal_spawn", "ticks_per_monster_spawn", "difficulty", "view_distance", "simulation_distance",
            "time", "time_duration", "time_full", "time_period", "moon_phase", "duration_since_created",
            "has_storm", "thundering", "thunder_duration", "weather_duration", "is_day", "is_night", "sky_darkness" };
    public static final String[] SERVER_STATIC_DATA_TAGS = new String[] {
            // Enum or registry listings. All of these end in Utilities.listTypes/registryKeys (an enum's constants or a registry stream)
            // or the equivalent inline loop, and the objects they build - MaterialTag, EnchantmentTag - are already async-safe types.
            "art_types", "nbt_attribute_types", "damage_causes", "teleport_causes", "particle_types", "effect_types",
            "pattern_types", "potion_types", "tree_types", "map_cursor_types", "world_types",
            "entity_types", "material_types", "sound_keys", "statistic_types", "statistic_type", "structures", "enchantments",
            // GameRuleReflect holds static MethodHandles set at class init, and GameRule.values() hands back a copy of the registry array.
            "gamerules",
            // Server properties and build info: plain fields on the server object, read-only, no world involved.
            "max_players", "motd", "view_distance", "port", "idle_timeout", "bukkit_name", "bukkit_version", "version", "denizen_version",
            // Depends.permissions/economy are static fields, and isEnabled() reads a boolean off the plugin.
            "has_permissions", "has_economy",
            // The server's recentTps array, which the main thread overwrites once per tick - so a reader off-thread gets a value at most
            // one tick stale, which is meaningless for a TPS measurement. Nothing here touches a world.
            "recent_tps",
            // The online player list. This one looks live and is not: Bukkit.getOnlinePlayers() hands back CraftServer's 'playerView' field
            // without copying, but that view's backing list - PlayerList.players - is a CopyOnWriteArrayList, so iterating it off-thread
            // can neither throw nor read a torn list; it just sees the players as of when iteration started. What comes out is a PlayerTag
            // holding nothing but a UUID (PlayerTag:208-214), and the NPC check on the way is Bukkit metadata, whose store is a
            // ConcurrentHashMap. 'online_players_flagged' is the same loop plus a player flag read, which is already thread-safe storage.
            // 'ops' joins them for a different reason: Bukkit.getOperators iterates ServerOpList's entries, and StoredUserList's backing map
            // is a Maps.newConcurrentMap, while the CraftOfflinePlayer it builds per entry is cached into CraftServer.offlinePlayers -
            // a Guava MapMaker map, also concurrent. That path never looks a player up by UUID, which is what makes it safe.
            //
            // 'players' is here for a subtler reason. It does reach CraftServer.getPlayer(UUID) - a plain 'playersByUUID.get(uuid)' on
            // PlayerList, the one Maps.newHashMap() in the whole chain, rewritten by the main thread on every join and quit. But read what
            // getOfflinePlayer(UUID) does with the answer: it only picks whether to hand back the live CraftPlayer or build a
            // CraftOfflinePlayer for the same uuid, and either way the only thing taken from it here is getUniqueId(), which is the uuid
            // that went in. A racing get can return null or a stale entry; it cannot change this tag's output. (It also does a File.list()
            // over the playerdata directory per call - slow, but blocking I/O is a reason to want this off the main thread, not to forbid it.)
            //
            // That reasoning is load-bearing: it holds only while nothing but the UUID is taken from the returned OfflinePlayer.
            // If PlayerTag ever starts holding the Bukkit object instead, this tag has to come back off the list.
            "players",
            // 'offline_players' and the ops pair used to be excluded, because they *branched* on OfflinePlayer.isOnline() - the same racing
            // HashMap read, except that there a stale answer puts an online player in an offline list, which is wrong output rather than a
            // different object. Rather than weigh how narrow that window is, the three tags were rewritten to take the online UUIDs in one
            // pass off the CopyOnWriteArrayList instead (see ServerTagBase.onlinePlayerIds), which has nothing to race against at all.
            "offline_players", "online_ops", "offline_ops",
            "online_players", "online_players_flagged", "ops",
            // The whitelist and ban lists are the same shape as 'ops' again - a StoredUserList walk plus getOfflinePlayer(NameAndId),
            // never a UUID lookup. 'banned_addresses' collects into a fresh Set, and 'is_banned' looks an address up in that same
            // concurrent map (its expiry cleanup writes back, but into the concurrent map, so that is fine too).
            // 'ban_info' is left out only because its sub-tags weren't read, not because anything about it looked wrong.
            "has_whitelist", "whitelisted_players", "banned_players", "banned_addresses", "is_banned",
            // 'match_player' is the online-player loop again, comparing names off the profile; the PlayerTag it builds is a UUID.
            // 'match_offline_player' walks PlayerTag.playerNames instead - Denizen's own ConcurrentHashMap, never Bukkit - and asks
            // nothing of the server but which of those UUIDs are online, which it now takes in one pass off the CopyOnWriteArrayList
            // exactly as 'offline_players' does. It used to ask that per candidate through PlayerTag.isOnline(), which is safe off-thread
            // but costs a scan of the online list each time, so a loose input like 'a' meant a scan per matching name.
            "match_player", "match_offline_player", "potion_effect_types"
    };

    /**
     * Tells the engine which tag bases read live server state, and therefore must be handed to the main thread when an async script reads them.
     * See <@link language Async Tag Safety>.
     * <p>
     * The rule for this list is simple: if reading the tag touches Bukkit, Citizens, or any other live server data, it belongs here.
     * Anything not listed here is treated as pure data processing and runs directly on the async script's own thread.
     */
    public static void registerAsyncTagSafety() {
        // Object types - all of these wrap live Bukkit objects.
        // Note 'material' is deliberately absent: building one is a lookup in the Material enum plus, for a block, a BlockData from the
        // static block registry - it never asks the server for anything. See the MaterialTag marking further down.
        // Note 'location' is deliberately absent - see below.
        for (String objectBase : new String[] { "biome", "chunk", "cuboid", "ellipsoid", "enchantment", "entity", "inventory", "item",
                "plugin", "polygon", "trade", "world" }) {
            TagManager.markMainThreadOnly(objectBase, FLAG_TAGS);
        }
        // The 'location' base is not marked at all, which is different from the bases above having exceptions: the base itself is free,
        // and each sub-tag is then judged on its own by the LocationTag type marking further down (main-thread-only with a long safe list).
        // That works because building a location from text no longer touches anything live. LocationTag.valueOf does three things:
        // a NoteManager lookup (its maps are ConcurrentHashMap), splitting and parsing numbers, and the world-name constructor -
        // which now stores the name and leaves resolving the world to whoever actually asks for it. So "<location[0,1,0]>" and
        // "<location[0,1,0,badworldname]>" both resolve entirely on the async script's own thread, and "<location[...].block>"
        // still costs exactly one hand-off, for the 'block' part rather than for the whole tag.
        // Player data is Bukkit-backed, but player flags are cached in a thread-safe way, so those stay fast for async scripts.
        // PLAYER_ASYNC_SAFE_TAGS joins them, the same list the PlayerTag type marking below uses - without this, "<player.is_online>" costs a
        // hand-off (up to a full tick) while "<[p].is_online>" off a defined player costs nothing, for the same answer.
        // The two markings are given one list rather than two on purpose: they were written separately once, and the base list then sat at
        // three names while the type list grew to nearly thirty, so most of what had been freed was only free when the player arrived
        // through a definition. Anything added to the list from here on is free both ways or neither.
        // Listing them here is only safe because building the PlayerTag itself is now safe: PlayerTag.valueOfInternal resolves a UUID
        // string with no server call at all, and a name through the concurrent playerNames map. See it for what it used to do instead.
        TagManager.markMainThreadOnly("player", withFlagTags(PLAYER_ASYNC_SAFE_TAGS));
        // "<player>" on its own hands back the queue's own linked player and never touches Bukkit, so it needn't cost a hand-off.
        // Note the parameterized form is fine off-thread now too - it is only the sub-tags outside the exception list that still cost anything.
        TagManager.markBareBaseAsyncSafe("player");
        // The server base is mostly live data (online players, worlds, plugins, bans, bossbars, recipes, ...), so it stays main-thread-only,
        // with two exceptions: server flags, which are plain saved data, and the startup-fixed data listed in SERVER_STATIC_DATA_TAGS.
        TagManager.markMainThreadOnly("server", withFlagTags(SERVER_STATIC_DATA_TAGS));
        if (Depends.citizens != null) {
            // NPC flags live in a Citizens trait, so unlike players even those have to be read on the main thread.
            TagManager.markMainThreadOnly("npc");
            TagManager.markBareBaseAsyncSafe("npc");
        }
        // The same types again, but marked on the object type rather than the tag base.
        // This is what covers an object reaching a tag without going through its own base, eg "<[my_entity].flag[x]>" or "<context.entity.location>".
        // InventoryTag is what is left in it: an inventory is a window onto live contents, and every tag on it reads through that window.
        TagManager.markObjectTypeMainThreadOnly(InventoryTag.class);
        // PluginTag is deliberately not marked at all, the way BiomeTag below is not. All seven of its tags - name, version, description,
        // authors, depends, soft_depends, commands - read the PluginDescriptionFile, which is parsed out of plugin.yml when the plugin loads
        // and never touched again. Its flags are no exception: they redirect into the server flag map (a RedirectionFlagTracker over
        // DenizenCore.serverFlagMap), which is the concurrent publish-on-write one. Finding a plugin in the first place is a different question
        // and stays where it was - that belongs to the 'plugin' tag base above, not to an object a script is already holding.
        // An ItemTag is a value, not a window onto an inventory: its constructor clones the stack it is handed (ItemTag:225), so the item a script
        // holds is its own copy - the same reason the trade and material types further down could be freed. What is listed here is everything that
        // only reads that copy. Each name was read:
        // 'material' both ways round - an ordinary item hands back the stored Material, and a block item (shulker box, spawner, sign, shield) goes
        // through CraftMetaBlockState.getBlockState. That one was checked in the purpur jar rather than assumed: it builds a *detached* block entity
        // at BlockPos.ZERO out of the item's own NBT and the frozen registries, so it never asks a world about a block.
        // 'quantity'/'max_stack'/'durability' are plain reads off the stack or its meta. 'display', 'lore' and the book family read components:
        // the NMS-side ones go through CraftItemStack.asNMSCopy, ie a copy of a copy, and stringify it. 'enchantments' and its relatives read the
        // stack's enchantment map and wrap each key in an EnchantmentTag, a registry singleton that is already marked safe above.
        // Flags belong here too: an item keeps them in its own custom data, and getCustomData is asNMSCopy plus copyTag (ItemHelperImpl:399),
        // so reading them touches nothing live. 'with_flag' writes, but onto a fresh clone that nothing else can see yet.
        // 'script' reads the item's own custom data for a script name and looks it up. It is here because ItemScriptHelper's two maps were given
        // the same treatment the core script registry got: a reload builds its set aside and publishes it with one write, instead of clearing the
        // live map and refilling it entry by entry. Without that, an async read landing in the middle of a reload could answer null for an item
        // whose script plainly exists. What it hands back is a ScriptTag, and script tags are safe already.
        // What is deliberately NOT here, having been looked at: 'skull_skin', which deals in player profiles;
        // 'recipe_ids', 'crafted_recipes' and the knowledge-book recipes, which read the server's recipe registry; 'map_to_image' and the map
        // property, which want a live MapView; and the lodestone location, which resolves a world. Everything unlisted stays on the main thread.
        // Two things to know about reading an item off-thread. ItemTag.getItemMeta caches lazily, so two async readers can each build a meta and one
        // wins - harmless here because both are rebuilt from the same private copy, unlike EntityTag.getUUID, whose lazy cache pins an identity that
        // was resolved from the live server. And nothing here protects one ItemTag object from a script adjusting it on another thread at the same
        // moment; that is the script writer's problem, the same as two '~sql' lines sharing one connection.
        TagManager.markObjectTypeMainThreadOnly(ItemTag.class, withFlagTags(
                "material", "quantity", "max_stack", "durability",
                "display", "has_display", "lore", "has_lore",
                "enchantments", "enchantment_map", "enchantment_types", "is_enchanted",
                "book_author", "book_title", "book_pages", "book_map",
                "script", "with_flag"));
        // BiomeTag is deliberately not marked at all - every one of its tags is readable off-thread, so there is nothing to list.
        // A BiomeTag holds a BiomeNMS, which is a NamespacedKey, a World, and a Holder.Reference into the biome registry - a handle on
        // datapack data, not on anything live. Read against the 26.2 jar, tag by tag:
        // 'name' is the stored key; 'humidity', 'base_temperature'(+'temperature'), 'has_downfall' and 'downfall_type' read Biome.climateSettings;
        // 'foliage_color' reads specialEffects and otherwise does its own arithmetic; 'fog_color', 'water_fog_color' and 'attribute' read
        // EnvironmentAttributeMap, whose entries map is built through a builder and never mutated in place; 'spawnable_entities' reads
        // MobSpawnSettings.spawners (also datapack-built) and converts through the entity registry.
        // 'temperature_at' and 'downfall_at' were the surprise: they look world-bound and are not. Biome.getTemperature is
        // getHeightAdjustedTemperature, which is pure maths over climateSettings, the BlockPos and a static noise field - it does not even
        // touch Biome's ThreadLocal temperature cache - and the sea level it takes comes from ServerLevel.getSeaLevel, a chain of final fields
        // down to the chunk generator. Biome.getPrecipitationAt is built on the same call.
        // Flags on a biome redirect into the server flag map, which is already safe.
        // What Denizen's own 'adjust' mechanisms do is replace climateSettings/specialEffects/attributes wholesale by reflection, so a reader
        // racing one sees the old object or the new one, never a half-built one. Same accepted caveat as a datapack reload rebuilding registries.
        // Only the 'biome' tag *base* stays main-thread-only (it is in the list above): building one from text goes through Bukkit.getWorlds(),
        // which copies out of CraftServer.worlds - a plain LinkedHashMap - and through WorldTag.valueOf, which iterates it.
        // A cuboid is pairs of LocationTags, so the geometry tags are arithmetic on stored coordinates and compare worlds by name.
        // Everything that goes and looks at what is *inside* the area - blocks, entities, players, npcs, chunks, outline/walls - stays off this list.
        // Each name below was read: 'contains' and 'is_within' were freed by making their world comparison use names rather than resolving worlds,
        // and 'center'/'size'/'volume' by adding and subtracting vectors instead of locations (the location overloads compare worlds).
        // The outline/walls/shell family only walks coordinates and names its results' world rather than resolving it, so it belongs here too -
        // it hands back where the blocks *would* be, without asking the world what is actually there. 'blocks' and friends do ask, and don't.
        // Flags are in here for the reason spelled out on PolygonTag below: a noted area keeps them in a SavableMapFlagTracker,
        // whose storage is concurrent, which is why the 'cuboid' tag base has listed them as safe all along - the type marking has to say so too.
        TagManager.markObjectTypeMainThreadOnly(CuboidTag.class, withFlagTags(
                "center", "size", "volume", "max", "min", "corners", "shift",
                "contains", "contains_location", "contains_cuboid", "intersects", "is_within",
                "with_min", "with_max",
                "outline", "get_outline", "outline_2d", "walls", "shell"));
        // An ellipsoid is a center LocationTag plus a size LocationTag, so the same reasoning as the cuboid above applies: its own
        // geometry is arithmetic on stored coordinates, and only the tags that go and look at what is *inside* the area cost a hand-off.
        // 'contains' was freed the same way as the cuboid's, by comparing world names instead of resolving worlds (see EllipsoidTag.contains) -
        // that also frees 'include' and 'chunks', which are built on it. 'chunks' is safe the rest of the way too: the ChunkTag(Location)
        // constructor takes a LocationTag's world by name (ChunkTag:156) and 'getCenter' does the same, and WorldTag holds a plain name string.
        // 'shell' walks the ellipsoid's own maths and names its results' world rather than resolving it, and 'bounding_box' builds its cuboid
        // through the vector overloads of add/subtract, which don't compare worlds.
        // 'random' uses CoreUtilities.getRandom(), a shared java.util.Random - correct off-thread (it CASes its seed), just contended.
        // Excluded: 'with_world' resolves the world it's handed; 'approximate_overlap_areas' reads NotedAreaTracker, whose per-world sets are
        // plain HashMaps and ArrayLists that the main thread rewrites whenever an area is noted; and 'note_name' is a NoteManager lookup,
        // left alone for the same reason it was on the cuboid.
        TagManager.markObjectTypeMainThreadOnly(EllipsoidTag.class, withFlagTags(
                "location", "size", "random", "add", "include", "with_location", "with_size", "chunks",
                "bounding_box", "world", "contains", "contains_location", "shell", "is_within"));
        // A polygon is a WorldTag holding a name, a y-range, and a list of plain x/z corner pairs - so, like the cuboid and the ellipsoid
        // above, its own geometry is arithmetic over stored numbers and the only tags that need the main thread are the ones that go
        // and look at what is *inside* the area.
        // Its two containment methods were freed the same way as the other two areas': by comparing the world's name rather than resolving
        // the world out of the location handed in (see PolygonTag.containsPrecise). That is what frees 'contains', 'contains_inclusive',
        // and the shell tags, which walk the polygon's own edges and ask containment about each candidate block.
        // Every location these produce is built through the LocationTag(x, y, z, worldName) constructor, and 'bounding_box' builds its cuboid
        // from two of those (CuboidTag.addPair only reads world names for the first pair). Note what that constructor actually does: it does
        // NOT merely name the world, it resolves it eagerly through Bukkit.getWorld(name) - a get on CraftServer.worlds, a plain LinkedHashMap.
        // That map only changes when a world loads or unloads, which is the same accepted caveat the LocationTag family below already carries.
        // What the constructor avoids is the worse half: it never calls getWorld() on the *source* location, so it never writes a resolved
        // world back into an object that may be shared. That is what actually freed this family.
        // Excluded: 'blocks_inclusive' reads the actual blocks, 'with_world' resolves the world it's handed, 'approximate_overlap_areas'
        // reads NotedAreaTracker's plain HashMaps, and 'note_name' is a NoteManager lookup - all for the same reasons as on the other areas.
        // The flag tags are in here for the same reason as on the two areas above: a noted area keeps its flags in a SavableMapFlagTracker,
        // whose storage is concurrent, which is why the 'polygon' tag base has listed them as safe all along.
        // A type marking has to say so separately though, or "<[my_polygon].flag[x]>" pays a hand-off that "<polygon[my_polygon].flag[x]>" does not.
        TagManager.markObjectTypeMainThreadOnly(PolygonTag.class, withFlagTags(
                "max_y", "min_y", "corners", "shift", "with_corner", "with_y_min", "with_y_max", "include_y",
                "outline", "outline_2d", "shell_inclusive", "contains_inclusive", "contains_location",
                "bounding_box", "world", "contains", "shell", "is_within"));
        // A trade is a MerchantRecipe and nothing else - all nine of its tags come from trade property classes, and every one of them
        // just reads a field off that recipe. 'result' and 'inputs' hand back ItemTags, but the ItemTag constructor clones the stack,
        // so what comes out is a copy rather than a window onto anything live. Note the 'trade' tag base stays main-thread-only:
        // building a trade from text applies properties, which parses items out of that text.
        // An enchantment is a registry singleton, so reading its data (levels, costs, rarity, conflicts, ...) touches nothing live -
        // the NMS helpers behind those just unwrap CraftEnchantment and read the handle. Two exceptions, both only for enchantments
        // registered by an enchantment script: 'full_name' and 'can_enchant' call into that script to produce their answer,
        // and a script can do anything at all. See EnchantmentHelperImpl.
        TagManager.markObjectTypeTagsMainThreadOnly(EnchantmentTag.class, "full_name", "can_enchant");
        // What an EntityTag keeps on itself, rather than reads off the entity. The type is decided when the tag object is built and stored.
        // 'uuid' answers from a stored field, or takes it off the entity once - CraftEntity.getUniqueId is two field reads, and the value can
        // never come back different, which is what makes its lazy cache harmless where getBukkitEntity's would not be: UUID has final fields,
        // so even a racing publish hands back a whole object. 'script' reads the stored script name and looks it up in the core registry, which
        // publishes whole since the item-script round. 'translated_name' reads the stored entity type and builds a string from its key.
        // Nothing else can be freed, and that is structural rather than a matter of effort: an EntityTag holds the live entity, and
        // getBukkitEntity re-fetches it through Bukkit.getEntity when the reference has gone stale. Flags are no exception - an entity keeps
        // them in its PDC, whose backing map is a plain HashMap belonging to CraftBukkit, where a resize can hide an entry that is present.
        TagManager.markObjectTypeMainThreadOnly(EntityTag.class, "type", "entity_type", "uuid", "script", "translated_name");
        // ChunkTag was read tag by tag, all 23 of them, and they fall into three groups.
        // Free because they never leave the tag object: 'x', 'z', 'xz' and 'world' hand back stored fields; 'simple' formats those plus the
        // stored world name; 'add' and 'sub' are arithmetic on the stored coordinates and hand back a new ChunkTag; 'cuboid' asks the world for
        // its height limits, which are fixed when the world is created (ServerLevel.getMinY/getMaxY are plain reads), and builds its corners
        // from the world *name*, which is the lazy path.
        // 'is_loaded' is free on the strength of the server's own line: CraftWorld.isChunkLoaded carries no AsyncCatcher, while isChunkGenerated
        // directly beside it opens with Bukkit.isPrimaryThread() - so CraftBukkit itself treats the two questions differently. Underneath, the
        // query lands in Paper's Moonrise chunk system, where the holder map is a ConcurrentLong2ReferenceChainedHashTable, lock-free by design
        // for exactly this kind of lookup. The answer is a snapshot: the status field it ends at is not volatile, so it can be a moment out of
        // date. That is acceptable for this question in a way it would not be for a flag - a flag holds what the script itself put there and
        // expects back exactly, while chunk loadedness changes on its own, constantly, with no script involved, and the use the tag exists for
        // ('if !is_loaded: chunkload') is unharmed by a stale yes or no.
        // Everything else needs the chunk itself, through getChunkForTag, and stays here: force_loaded, plugin_tickets, tile_entities, entities,
        // living_entities, players, height_map, average_height, is_flat, surface_blocks, blocks_flagged, spawn_slimes, inhabited_time.
        // 'is_generated' stays for a different and sharper reason, worth knowing before anyone marks it: off the main thread CraftWorld does not
        // refuse it, it wraps it in CompletableFuture.supplyAsync(..., mainThreadProcessor).join() - so the script blocks on the main thread
        // anyway, through a hand-off Denizen never made and its counter cannot see. A tag like that reads as free while costing more than one
        // that admits it.
        TagManager.markObjectTypeMainThreadOnly(ChunkTag.class, "x", "z", "xz", "world", "simple", "add", "sub", "cuboid", "is_loaded");
        // A world's name is a stored string, and the rest of WORLD_ASYNC_SAFE_TAGS reads a plain field off the level - see that list for
        // what is in it and, more usefully, for why each of the rest is not. Everything unlisted goes and gathers something live.
        TagManager.markObjectTypeMainThreadOnly(WorldTag.class, WORLD_ASYNC_SAFE_TAGS);
        // A material is a plain value, not a handle on anything live: it holds a Material enum constant and, for blocks, a BlockData -
        // and Bukkit hands out a copy of that rather than the block's own, so nothing here can change under a reader.
        // Its tags read that data, static registries (vanilla tags, instruments, sound groups), or the block properties, and none of the
        // 31 material property classes touch the world either. So the type is readable off-thread, with one exception:
        // 'is_enabled' takes a world and resolves it.
        TagManager.markObjectTypeTagsMainThreadOnly(MaterialTag.class, "is_enabled");
        // A location is mostly a plain value - x/y/z/yaw/pitch and a world name, all stored on the object itself.
        // Only some of its tags reach into the world (block, material, inventory, ray_trace, ...), so the ones that are pure arithmetic
        // on those stored fields are listed here and stay on the async script's own thread.
        // Each name below was checked against its implementation: none of them call getWorld(), and none build their result through
        // the LocationTag(Location) constructor, which does. Do not add a name here without reading what it actually does.
        // Tags here that take another location as raw text ('distance', 'add', ...) used to carry a caveat, because parsing that text
        // resolved the world it named. It no longer does - the world-name constructor stores the name - so passing text is as safe as
        // passing an object now, and 'rotate_yaw'/'rotate_pitch' are in the list below for the same reason: they rebuild the location
        // through identify() and LocationTag.valueOf, both of which now work purely from the world's name.
        // 'center' joined them by being changed the same way the area types' outline family was, to name the world instead of resolving it.
        // Excluded on purpose despite looking like pure arithmetic: 'direction' calls getWorld() outright to build its vector.
        TagManager.markObjectTypeMainThreadOnly(LocationTag.class,
                "x", "y", "z", "pitch", "yaw", "simple", "distance", "distance_squared",
                "round", "round_up", "round_down", "round_to", "round_to_precision",
                "with_x", "with_y", "with_z", "with_yaw", "with_pitch",
                "add", "sub", "mul", "div",
                // 'chunk' only divides the coordinates and copies the world name - it doesn't go and fetch the actual chunk,
                // and 'world' hands back the name wrapped in a WorldTag rather than resolving the world itself.
                "chunk", "get_chunk", "world",
                // These move the location by a vector worked out from its own yaw and pitch, then hand the result back through
                // the LocationTag(Location) constructor. That constructor used to resolve the world and so kept this whole family
                // off limits; it no longer does when copying a LocationTag, which is what freed them.
                "above", "below", "forward", "forward_flat", "backward", "backward_flat",
                "left", "right", "up", "down", "points_between",
                // Same family, same reasoning: trigonometry over the location's own coordinates, handed back through clone() and the
                // LocationTag(Location) constructor. 'clone' is a shallow field copy and copying a LocationTag never asks for the world.
                "rotate_around_x", "rotate_around_y", "rotate_around_z",
                "points_around_x", "points_around_y", "points_around_z",
                "relative", "random_offset",
                // Pure arithmetic on the stored coordinates, with no location input at all.
                "simplex_3d", "vector_to_face", "center", "rotate_yaw", "rotate_pitch",
                // The rest of the vector family, registered onto this same processor by VectorObject.register (see LocationTag.register).
                // They treat the location as three numbers and build their result through duplicate(), which for a location is clone().
                // 'x'/'y'/'z'/'with_*'/'add'/'sub'/'mul'/'div' above come from there too, and were already trusted on exactly this basis.
                "xyz", "normalize", "vector_length", "vector_length_squared",
                "to_axis_angle_quaternion", "quaternion_between_vectors",
                // Text forms of the location. They read the world's *name*, which getWorldName hands back without resolving anything.
                // 'format' does the same, substituting coordinates into the script's own format string.
                "format", "formatted", "raw");
        // Note: no flag exception for entities and the rest - only player flags have thread-safe storage, everything else keeps its flags in live Bukkit data.
        // Two more get through here. 'uuid' is a stored field on the tag object and touches nothing at all - PlayerTag is only ever a UUID.
        // 'is_online' had to be earned: every route to it (PlayerTag.isOnline, CraftPlayer.isOnline, CraftOfflinePlayer.isOnline) ends at
        // CraftServer.getPlayer(UUID), which is a 'get' on PlayerList.playersByUUID - a plain HashMap the main thread rewrites on join and quit.
        // Since the tag hands that answer straight back, a read racing a resize of that map would simply return the wrong thing.
        // So PlayerTag.isOnline was rewritten to walk the online player list off-thread instead, which is a CopyOnWriteArrayList - see it for why.
        // 'name' is here for a third kind of reason: Denizen notes every player's name itself, so PlayerTag.getName() was rewritten to answer
        // from that rather than from OfflinePlayer.getName(), which went to the same racy map and then read the player's NBT off disk.
        // Freeing it also meant deleting the deprecated 'name.list'/'name.display' sub-forms, which read the live player - see PlayerTag.register.
        // The rest below is what a full pass over the type turned up. Note most of PlayerTag can never join them: 50 of its tags are registered
        // through registerOnlineOnlyTag, which by definition reads the live Player entity.
        // - 'is_player' is a constant true.
        // - 'is_op', 'whitelisted'/'is_whitelisted' and the whole ban family read Bukkit's op/whitelist/ban lists, which are StoredUserLists,
        //   and StoredUserList.map is a Maps.newConcurrentMap. That is the same finding that already freed server.ops and server.banned_players.
        //   The ban tags reach that list by player *name*, which is now an in-memory lookup too.
        // - the played-time family reads the player's data file. That is a disk read and an NBT parse, which is slow but not unsafe:
        //   PlayerDataStorage.save writes a temp file and swaps it in through Util.safeReplaceFile, so a reader gets the whole old file
        //   or the whole new one. Off-thread is arguably where this work belongs anyway.
        // - 'chat_history'/'chat_history_list' read PlayerTagBase.playerChatHistory, which was a plain HashMap holding lists that were edited
        //   in place on every chat message; it is concurrent now and its lists are replaced whole. See it.
        // - 'fake_block'/'fake_block_locations'/'fake_entities'/'disguise_to_self' read Denizen's own FakeBlock, FakeEntity and DisguiseCommand
        //   stores. Those were plain HashMaps edited in place, which is why these four used to be the controls in the async player test;
        //   the maps a reader touches are concurrent now, and the fields the tags hand back (FakeBlock.material, FakeEntity.entity,
        //   TrackedDisguise.fakeToSelf) are volatile, because each is written after its holder is already published into the map.
        //   The disguise map's inner maps stay plain HashMaps published whole - a global disguise lives under a null key, which no
        //   concurrent map accepts. Everything these four return is either a value object (MaterialTag) or a stored one (the fake EntityTag).
        // - 'sidebar_lines'/'sidebar_title'/'sidebar_scores' read Denizen's own sidebar store, which is concurrent now, and one sidebar's
        //   contents are a Display published whole rather than three fields written one after another - so a reader can no longer catch a
        //   line count that its lines haven't caught up with. See Sidebar. The deprecated '<player.sidebar.lines>' family stays behind.
        // Left main-thread-only among the rest: 'money'/'formatted_money'/'chat_prefix'/'chat_suffix'/'groups'/'in_group'/'has_permission'
        // all go out to Vault and a third-party plugin.
        TagManager.markObjectTypeMainThreadOnly(PlayerTag.class, withFlagTags(PLAYER_ASYNC_SAFE_TAGS));
        if (Depends.citizens != null) {
            TagManager.markObjectTypeMainThreadOnly(NPCTag.class);
        }
    }

    public static void registerMainObjects() {
        registerObjectTypes();
        registerNotables();
        registerConversions();
        registerSubtypeSets();
        // Final debug
        if (CoreConfiguration.debugVerbose) {
            StringBuilder debug = new StringBuilder(256);
            for (ObjectType<?> objectType : ObjectFetcher.objectsByPrefix.values()) {
                debug.append(DebugInternals.getClassNameOpti(objectType.clazz)).append(" as ").append(objectType.prefix).append(", ");
            }
            Debug.echoApproval("Loaded core object types: [" + debug.substring(0, debug.length() - 2) + "]");
        }
    }

    public static ObjectType<BiomeTag> TYPE_BIOME;
    public static ObjectType<ChunkTag> TYPE_CHUNK;
    public static ObjectType<CuboidTag> TYPE_CUBOID;
    public static ObjectType<EllipsoidTag> TYPE_ELLIPSOID;
    public static ObjectType<EnchantmentTag> TYPE_ENCHANTMENT;
    public static ObjectType<EntityTag> TYPE_ENTITY;
    public static ObjectType<InventoryTag> TYPE_INVENTORY;
    public static ObjectType<ItemTag> TYPE_ITEM;
    public static ObjectType<LocationTag> TYPE_LOCATION;
    public static ObjectType<MaterialTag> TYPE_MATERIAL;
    public static ObjectType<NPCTag> TYPE_NPC;
    public static ObjectType<PlayerTag> TYPE_PLAYER;
    public static ObjectType<PluginTag> TYPE_PLUGIN;
    public static ObjectType<PolygonTag> TYPE_POLYGON;
    public static ObjectType<TradeTag> TYPE_TRADE;
    public static ObjectType<WorldTag> TYPE_WORLD;

    private static void registerObjectTypes() {

        // <--[tag]
        // @attribute <biome[<biome>]>
        // @returns BiomeTag
        // @description
        // Returns a biome object constructed from the input value.
        // Refer to <@link objecttype BiomeTag>.
        // -->
        TYPE_BIOME = ObjectFetcher.registerWithObjectFetcher(BiomeTag.class, BiomeTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // b@

        // <--[tag]
        // @attribute <chunk[<chunk>]>
        // @returns ChunkTag
        // @description
        // Returns a chunk object constructed from the input value.
        // Refer to <@link objecttype ChunkTag>.
        // -->
        TYPE_CHUNK = ObjectFetcher.registerWithObjectFetcher(ChunkTag.class, ChunkTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // ch@

        // <--[tag]
        // @attribute <cuboid[<cuboid>]>
        // @returns CuboidTag
        // @description
        // Returns a cuboid object constructed from the input value.
        // Refer to <@link objecttype CuboidTag>.
        // -->
        TYPE_CUBOID = ObjectFetcher.registerWithObjectFetcher(CuboidTag.class, CuboidTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // cu@

        // <--[tag]
        // @attribute <ellipsoid[<ellipsoid>]>
        // @returns EllipsoidTag
        // @description
        // Returns an ellipsoid object constructed from the input value.
        // Refer to <@link objecttype EllipsoidTag>.
        // -->
        TYPE_ELLIPSOID = ObjectFetcher.registerWithObjectFetcher(EllipsoidTag.class, EllipsoidTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // ellipsoid@

        // <--[tag]
        // @attribute <enchantment[<enchantment>]>
        // @returns EnchantmentTag
        // @description
        // Returns an enchantment object constructed from the input value.
        // Refer to <@link objecttype EnchantmentTag>.
        // -->
        TYPE_ENCHANTMENT = ObjectFetcher.registerWithObjectFetcher(EnchantmentTag.class, EnchantmentTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // enchantment@

        // <--[tag]
        // @attribute <entity[<entity>]>
        // @returns EntityTag
        // @description
        // Returns an entity object constructed from the input value.
        // Refer to <@link objecttype EntityTag>.
        // -->
        TYPE_ENTITY = ObjectFetcher.registerWithObjectFetcher(EntityTag.class, EntityTag.tagProcessor).generateBaseTag(); // e@
        TYPE_ENTITY.typeChecker = (inp) -> { // This is adapted 'no other type code' but for e@, p@, and n@
            if (inp == null) {
                return false;
            }
            if (inp instanceof PlayerTag || inp instanceof EntityTag || inp instanceof NPCTag) {
                return true;
            }
            if (inp instanceof ElementTag) {
                String simple = inp.identifySimple();
                int atIndex = simple.indexOf('@');
                if (atIndex != -1) {
                    String code = simple.substring(0, atIndex);
                    if (!code.equals("e") && !code.equals("p") && !code.equals("n") && !code.equals("el")) {
                        if (ObjectFetcher.objectsByPrefix.containsKey(code)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        };
        TYPE_ENTITY.typeConverter = (obj, context) -> {
            if (obj instanceof PlayerTag) {
                if (!((PlayerTag) obj).isOnline()) {
                    if (context.showErrors()) {
                        Debug.echoError("Player '" + obj.debuggable() + "' is offline, cannot convert to EntityTag.");
                    }
                    return null;
                }
                return new EntityTag(((PlayerTag) obj).getPlayerEntity());
            }
            else if (obj instanceof NPCTag) {
                if (!((NPCTag) obj).isSpawned() && !EntityTag.allowDespawnedNpcs) {
                    if (context.showErrors()) {
                        Debug.echoError("NPC '" + obj.debuggable() + "' is unspawned, cannot convert to EntityTag.");
                    }
                    return null;
                }
                return new EntityTag((NPCTag) obj);
            }
            return EntityTag.valueOf(obj.toString(), context);
        };
        TYPE_ENTITY.typeShouldBeChecker = (obj) -> {
            if (obj instanceof EntityFormObject) {
                return true;
            }
            String raw = obj.toString();
            if (raw.startsWith("p@") || raw.startsWith("e@") || raw.startsWith("n@")) {
                return true;
            }
            return false;
        };

        // <--[tag]
        // @attribute <inventory[<inventory>]>
        // @returns InventoryTag
        // @description
        // Returns an inventory object constructed from the input value.
        // Refer to <@link objecttype InventoryTag>.
        // -->
        // non-static due to notes and inventory scripts
        TYPE_INVENTORY = ObjectFetcher.registerWithObjectFetcher(InventoryTag.class, InventoryTag.tagProcessor).setAsNOtherCode().generateBaseTag(); // in@

        // <--[tag]
        // @attribute <item[<item>]>
        // @returns ItemTag
        // @description
        // Returns an item object constructed from the input value.
        // Refer to <@link objecttype ItemTag>.
        // -->
        // non-static as item scripts can contain tags
        TYPE_ITEM = ObjectFetcher.registerWithObjectFetcher(ItemTag.class, ItemTag.tagProcessor).setAsNOtherCode().generateBaseTag(); // i@

        // <--[tag]
        // @attribute <location[<location>]>
        // @returns LocationTag
        // @description
        // Returns a location object constructed from the input value.
        // Refer to <@link objecttype LocationTag>.
        // -->
        TYPE_LOCATION = ObjectFetcher.registerWithObjectFetcher(LocationTag.class, LocationTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // l@

        // <--[tag]
        // @attribute <material[<material>]>
        // @returns MaterialTag
        // @description
        // Returns a material object constructed from the input value.
        // Refer to <@link objecttype MaterialTag>.
        // -->
        TYPE_MATERIAL = ObjectFetcher.registerWithObjectFetcher(MaterialTag.class, MaterialTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // m@

        if (Depends.citizens != null) {
            // Tag generated externally as input is optional
            TYPE_NPC = ObjectFetcher.registerWithObjectFetcher(NPCTag.class, NPCTag.tagProcessor); // n@
            TYPE_NPC.typeChecker = (inp) -> { // This is adapted 'no other type code' but allows instanceof EntityTag
                if (inp == null) {
                    return false;
                }
                if (inp instanceof NPCTag || inp instanceof EntityTag) {
                    return true;
                }
                if (inp instanceof ElementTag) {
                    String simple = inp.identifySimple();
                    int atIndex = simple.indexOf('@');
                    if (atIndex != -1) {
                        String code = simple.substring(0, atIndex);
                        if (!code.equals("n") && !code.equals("el")) {
                            if (ObjectFetcher.objectsByPrefix.containsKey(code)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                return false;
            };
            TYPE_NPC.typeConverter = (obj, context) -> {
                if (obj instanceof EntityTag && ((EntityTag) obj).isCitizensNPC()) {
                    return ((EntityTag) obj).getDenizenNPC();
                }
                return NPCTag.valueOf(obj.toString(), context);
            };
        }

        // Tag generated externally as input is optional
        TYPE_PLAYER = ObjectFetcher.registerWithObjectFetcher(PlayerTag.class, PlayerTag.tagProcessor); // p@
        TYPE_PLAYER.typeChecker = (inp) -> { // This is adapted 'no other type code' but allows instanceof EntityTag
            if (inp == null) {
                return false;
            }
            if (inp instanceof PlayerTag || inp instanceof EntityTag) {
                return true;
            }
            if (inp instanceof ElementTag) {
                String simple = inp.identifySimple();
                int atIndex = simple.indexOf('@');
                if (atIndex != -1) {
                    String code = simple.substring(0, atIndex);
                    if (!code.equals("p") && !code.equals("el")) {
                        if (ObjectFetcher.objectsByPrefix.containsKey(code)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        };
        TYPE_PLAYER.typeConverter = (obj, context) -> {
            if (obj instanceof EntityTag && ((EntityTag) obj).isPlayer()) {
                return ((EntityTag) obj).getDenizenPlayer();
            }
            return PlayerTag.valueOf(obj.toString(), context);
        };

        // <--[tag]
        // @attribute <plugin[<plugin>]>
        // @returns PluginTag
        // @description
        // Returns a plugin object constructed from the input value.
        // Refer to <@link objecttype PluginTag>.
        // -->
        TYPE_PLUGIN = ObjectFetcher.registerWithObjectFetcher(PluginTag.class, PluginTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // pl@

        // <--[tag]
        // @attribute <polygon[<polygon>]>
        // @returns PolygonTag
        // @description
        // Returns a polygon object constructed from the input value.
        // Refer to <@link objecttype PolygonTag>.
        // -->
        TYPE_POLYGON = ObjectFetcher.registerWithObjectFetcher(PolygonTag.class, PolygonTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // polygon@

        // <--[tag]
        // @attribute <trade[<trade>]>
        // @returns TradeTag
        // @description
        // Returns a trade object constructed from the input value.
        // Refer to <@link objecttype TradeTag>.
        // -->
        // Non-static due to potential for dynamic items.
        TYPE_TRADE = ObjectFetcher.registerWithObjectFetcher(TradeTag.class, TradeTag.tagProcessor).setAsNOtherCode().generateBaseTag(); // trade@

        // <--[tag]
        // @attribute <world[<world>]>
        // @returns WorldTag
        // @description
        // Returns a world object constructed from the input value.
        // Refer to <@link objecttype WorldTag>.
        // -->
        // non-static as worlds can be dynamically loaded
        TYPE_WORLD = ObjectFetcher.registerWithObjectFetcher(WorldTag.class, WorldTag.tagProcessor).setAsNOtherCode().generateBaseTag(); // w@
    }

    private static void registerNotables() {
        NoteManager.registerObjectTypeAsNotable(CuboidTag.class);
        NoteManager.registerObjectTypeAsNotable(EllipsoidTag.class);
        NoteManager.registerObjectTypeAsNotable(InventoryTag.class);
        NoteManager.registerObjectTypeAsNotable(ItemTag.class);
        NoteManager.registerObjectTypeAsNotable(LocationTag.class);
        NoteManager.registerObjectTypeAsNotable(PolygonTag.class);
    }

    private static void registerConversions() {
        CoreUtilities.objectConversions.add((obj) -> {
            if (obj instanceof Biome) {
                return new BiomeTag((Biome) obj);
            }
            if (obj instanceof Chunk) {
                return new ChunkTag((Chunk) obj);
            }
            if (obj instanceof Color) {
                return BukkitColorExtensions.fromColor((Color) obj);
            }
            if (obj instanceof Enchantment) {
                return new EnchantmentTag((Enchantment) obj);
            }
            if (obj instanceof Entity) {
                return new EntityTag((Entity) obj).getDenizenObject();
            }
            if (obj instanceof Inventory) {
                return InventoryTag.mirrorBukkitInventory((Inventory) obj);
            }
            if (obj instanceof ItemStack) {
                return new ItemTag((ItemStack) obj);
            }
            if (obj instanceof Location) {
                return new LocationTag((Location) obj);
            }
            if (obj instanceof Material) {
                return new MaterialTag((Material) obj);
            }
            if (obj instanceof BlockData) {
                return new MaterialTag((BlockData) obj);
            }
            if (obj instanceof Block) {
                return new LocationTag(((Block) obj).getLocation());
            }
            if (Depends.citizens != null && obj instanceof NPC) {
                return new NPCTag((NPC) obj);
            }
            if (obj instanceof OfflinePlayer) {
                return new PlayerTag((OfflinePlayer) obj);
            }
            if (obj instanceof Plugin) {
                return new PluginTag((Plugin) obj);
            }
            if (obj instanceof MerchantRecipe) {
                return new TradeTag((MerchantRecipe) obj);
            }
            if (obj instanceof World) {
                return new WorldTag((World) obj);
            }
            return null;
        });
    }

    private static void registerSubtypeSets() {
        ObjectFetcher.registerCrossType(EntityTag.class, EntityFormObject.class);
        ObjectFetcher.registerCrossType(PlayerTag.class, EntityTag.class);
        ObjectFetcher.registerCrossType(PlayerTag.class, EntityFormObject.class);
        if (Depends.citizens != null) {
            ObjectFetcher.registerCrossType(NPCTag.class, EntityTag.class);
            ObjectFetcher.registerCrossType(NPCTag.class, EntityFormObject.class);
        }
    }
}

The Denizen Scripting Language - Paper Impl (Unofficial version!)
--------------------------------------------

An implementation of the Denizen Scripting Language for Paper servers, with strong Citizens interlinks to emphasize the power of using Denizen with NPCs!

**Version 1.3.4M**: Compatible with Paper 1.21.11, 26.1.2, 26.2 and 26.3!

> [!IMPORTANT]
> Support for versions below **1.21.11** has been officially dropped.

> ⚡ **Full Compatibility:** Works flawlessly with **[Denizen Utilities](https://modrinth.com/plugin/denizen-utilities)** and **[Denizen Reflect](https://modrinth.com/plugin/denizen-reflect)** right after installation.

## 💎 Extension Highlights
* **Next-Gen Support:** Fully updated extension for VS Code by `Humususus`.
* **All-in-One:** Includes all the latest changes and community-requested tweaks.
* **Source & Guide:** Check it out on 🔗 **[GitHub: refined-denizenScript](https://github.com/Humususus/refined-denizenScript)**

## ✨ New Features
* **Internal Migration:** Fully migrated to **Paper Components** for improved performance and modern API compatibility.
* **Events:**
    * Added support for the Paper-specific event `on player changes unchecked sign`.
    * Migrated `on player equips|unequips ...` to a generalized `on <entity> equips|unequips <item> or armor|helmet|chestplate|leggings|boots|body` syntax, extending support to all entities and adding the new body slot.
        * `<context.entity>` – The **EntityTag** involved.
        * `<context.slot>` – The name of the equipment slot.
        * `<context.new_item>` / `<context.old_item>` – The current and previous **ItemTag** in the slot.
* **BiomeTag (by `isnsest`):**
    * Added 'BiomeTag.attribute' mechanism ang tag to set a specific biome vanilla attributes, accepts a `MapTag`.
    * Note: Specify only the attribute name directly (e.g. `SKY_COLOR`, not `visuals/SKY_COLOR`).
    * More details you can find [here](https://minecraft.wiki/w/Environment_attribute).
    * Usage examples:
       * Gives the plains biome a red sky and nether music.
         * `adjust <biome[plains]> attribute:[SKY_COLOR=<ColorTag or valid HEX>;CLOUD_HEIGHT=600]`
       * Gives the plains biome cave ambience, with its mood sound coming around far more often than usual.
         * `adjust <biome[plains]> attribute:<map[ambient_sounds=<map[loop=AMBIENT_CAVE;mood=<map[sound=AMBIENT_CAVE;tick_delay=1200]>]>]>`
       * Stops players sleeping in deserts, and tells them why.
         * `adjust <biome[desert]> attribute:<map[bed_rule=<map[can_sleep=NEVER;error_message=<&c>It is far too hot to sleep here.]>]>`
       * Fills the plains biome with drifting white ash.
         * `adjust <biome[plains]> attribute:<map[ambient_particles=<map[WHITE_ASH=0.12]>]>` 
* **Text & Formatting:**
    * New tags: `<&sprite>`, `<&shadow_color>`, `<&shadow_gradient>`, `<&dual_gradient>` and `<&head>`.
      * Sprite usage example - `<&sprite[minecraft:items:item/porkchop]>`
      * Shadow Color usage examples:
         * Simple variant - `<&shadow_color[#51a2ff]>`
         * With adjustable transparency - `<&shadow_color[<color[#51a2ff].with_alpha[255]>]>`
      * Shadow gradient usage examples:
         * Simple variant - `<&shadow_gradient[from=#51a2ff;to=#FFF085]>`
         * With adjustable transparency - `<&shadow_gradient[from=<color[#51a2ff].with_alpha[0]>;to=<color[#FFF085].with_alpha[255]>]>`
      * Dual gradient including Shadow color and Simple color gradients, usage examples:
         * Simple variant - `<&dual_gradient[from=#51a2ff;to=#FFF085;s_from=#FFF085;s_to=#51a2ff]>`
         * Tags `from` and `to` for simple color gradient adjusting.
         * Tags `s_from` and `s_to` for shadow color gradient adjusting.
      * Head usage examples:
         * Full Face Texture - `<&head[Tjtoxshpilivili1]>`
         * Only Face Texture (without surface pixels) - `<&head[!Tjtoxshpilivili1]>`
         * Head tag also accepts `<PlayerTag.skin_blob>`, `UUID` and `base64` skin textures.
    * Added `.shadow_color`, `.shadow_gradient` and `.dual_gradient` tags to `ElementTag`.
* **New Tags & Utilities:**
  * **List Operations:**
    * `<ListTag.activation[<type>]>` – Applies activation functions (e.g., RELU, SIGMOID, TANH) directly to list elements.
    * `<ListTag.matrix_mul[<list>]>` – Performs matrix multiplication.
    * `<ListTag.dot_product[<list>]>` – Calculates the dot product of two numerical lists.
  * **Queue & Thread Diagnostics:**
    * `<QueueTag.is_async>` – Checks if a queue is running asynchronously.
    * `<QueueTag.async_stats>` – Returns performance statistics for async queues.
    * `<util.is_main_thread>` – Checks if the current thread is the main server thread.
    * `<util.current_thread>` – Returns details about the active thread.
    * `<util.current_time_nanos>` – Returns precise time in nanoseconds.

## ⚡ Async Scripting Engine
* **Off-Thread Execution:**
  * Script queues can now run off the main thread. Internal logic (tags, math, text, lists, maps) no longer consumes main thread CPU time, preventing slow scripts from lagging the server.
* **Automatic Main Thread Syncing:**
  * Non-thread-safe actions automatically sync to the main thread with zero risk of corruption.
  * The first thread-crossing operation may take up to a tick, while subsequent sequential calls take only microseconds.
  * Fire-and-forget commands (`narrate`, `playsound`, `playeffect`, `runlater`, etc.) hand over without waiting at all.
* **New Script Commands & Switches:**
  * Added `async` parameter support to `run`, `runlater`, `define`, and `definemap` executions.
  * Usage examples:
    * Run async - `run my_task async`
    * Schedule async task - `runlater nightly_report delay:1h async`
    * Async definition - `~define result <[huge_list].parse_tag[<[parse_value].to_uppercase>]>`
    * Async block:
      ```
      - async:
        - define sorted <server.flag[scores].sort_by_value>
      - narrate "Top: <[sorted].keys.last>"
      ```
* **New Configuration Settings:**
  * Added `Scripts.Async` section: `Allow`, `Main thread wait timeout`, `Shutdown timeout`, `Main thread task budget ms`, `Main thread wait linger us`, `Warn at queue count`, and `Max queue count`.
* **Documentation:**
  * Full write-up on thread safety is available in the [Core README](https://github.com/Energobro/DenizenM-Core#async-scripts) and the **Async Queues / Async Tag Safety** language meta pages.

## 👾 Commands
* **Teleport:**
  * An `async` option has been added that teleports the player to unloaded chunks without causing server lag; sometimes there is a slight delay in execution. Details: https://docs.papermc.io/paper/dev/entity-teleport/
  * Added `~waitable` tag support for teleports utilizing the async parameter. This allows scripts to precisely track and wait until the asynchronous teleportation process is fully completed.
  * Usage examples:
    * Async teleport - `teleport <object> <location> async`
    * Waitable async teleport - `~teleport <object> <location> async`
* **Playeffect:**
  * Migrated to Paper's modern `ParticleBuilder` API, fixing a vanilla limitation where particles wouldn't render beyond 32 blocks from the player.
  * Added automatic handling for the `forced` parameter. Previously, even if you specified a high visibility radius (e.g., visibility:100) for a particle spawned 50 blocks away, it wouldn't display. Forcing the particle now ensures it correctly renders at extended distances.
  * Usage example - `playeffect effect:END_ROD quantity:100 <location> visibility:100 forced`
* **Resourcepack:**
    * Added a new `add` argument to the `resourcepack` command to send additional resource packs to a player.
    * Added `PlayerTag.remove_resource_pack` mechanism to remove a specific resource pack by ID from a player.
    * Added `PlayerTag.remove_resource_packs` mechanism to remove all resource packs from a player.
* **Adjustblock:**
    * Added support for passing a **MapTag**, matching the behavior of `adjust`.
    * Usage example - `adjustblock <location> <map[direction=north;half=top]>`
* **Playsound:**
  * Added `source:<entity>` parameter to play sound directly from an entity, causing the audio to follow its movement and automatically stop if the entity dies.
  * Added `targets:<player>|...` parameter to specify which players hear the sound, which also offloads execution from the main server thread.
  * Usage examples:
    * Sound following an entity - `playsound <location> sound:<sound> source:<entity>`
    * Targeted off-thread play - `playsound <location> sound:<sound> targets:<player>`
* **Run / Runlater:**
  * Added `async` argument to run the scheduled script in its own asynchronous queue when executed.
  * Usage examples:
    * `runlater <script> delay:5s async`
    * `run <script> async`

## 🧪 Items & Mechanics
* **Attributes:**
    * Added new `.rarity_color` tag for items, returns ColorTag.
       * Usage example - `<player.item_in_hand.rarity_color>`
* **Custom Model Data:**
  * Updated `custom_model_data` mechanism and property, returns a MapTag of `floats`, `strings`, `flags` (booleans), and `colors`.
  * Now accepts a `MapTag` containing `Lists` of `floats`, `strings`, `flags` (booleans), and `colors` (RGB) for advanced item model selection, while fully retaining backward compatibility for single-number inputs.
  * Usage examples:
    * Backward compatibility - `custom_model_data: 1000`
    * New format - `custom_model_data: <map[floats=<list[1000]>;flags=<list[...]>;strings=<list[foo:bar]>;colors=<list[<color[127,0,0]>]>]>`
    * Simple usage - `custom_model_data: [floats=1000;flags=...;strings=foo:bar;colors=<color[127,0,0]>]`

## 🧱 Mechanisms & Tags
* **MaterialTag:**
  * Added `MaterialTag.chain_part` tag and mechanism to set or get the shelf connection state (`LEFT`, `CENTER`, `RIGHT`, `UNCONNECTED`).
  * Usage examples:
    * Tag - `<material.chain_part>`
    * Mechanism - `adjust <material> chain_part:CENTER`

## 🧹 Optimization & Cleanup
* **Core Optimization:** Implementation of custom optimizations across several internal classes.
* **Tags Modification:**
    * Added new sub-tag `.unsorted` to tag `.find_entities[<#.#>].within[<#.#>]` to bypass distance-based sorting. Use this for better performance when the order of entities in the list is not required.
    * Optimized the `.distance` tag by replacing `Math.pow` with direct multiplication. This reduces computational overhead and results in faster distance calculations across the script.
* **Removals:**
    * The `.scriptname` tag has been removed from all objects.
    * `Denizen ASAP Strong Warning` has been fully removed.
    * Some very old, deprecated Denizen tags.
    * **[WIP]** Removed NMSVersion checks for versions prior to 26.1.

## 🐛 Bug Fixes
* **showfake:** Fixed an issue where the command would trigger an error message despite functioning correctly.
* **fakeinternaldata:** Fixed a critical bug where the command was non-functional and threw an error.

**Learn about Denizen from the Beginner's guide:** https://guide.denizenscript.com/guides/background/index.html

#### Need help using Denizen? Try one of these places:

- **My Telegram Channel** - spoilers, works with Denizen, new features: https://t.me/energ0bro
- **Denizen Home Page** - a link directory: https://denizenscript.com/
- **Meta Documentation (!! WITHOUT NEW CHANGES !!)** - command/tag/event/etc. search: https://meta.denizenscript.com/
- **Beginner's Guide** - text form: https://guide.denizenscript.com/

#### Also check out:

- **Citizens2 (NPC support)**: https://github.com/CitizensDev/Citizens2/
- **Depenizen (Other plugin support)**: https://github.com/DenizenScript/Depenizen
- **dDiscordBot (Adds a Discord bot to Denizen)**: https://github.com/DenizenScript/dDiscordBot
- **DenizenCore (Our core, needed for building)**: https://github.com/DenizenScript/Denizen-Core

### Building

- Built against JDK 25, using maven `pom.xml` as project file.
- Requires building all listed versions of Spigot via Spigot BuildTools: https://www.spigotmc.org/wiki/buildtools/
- Install all Paper dependencies.

### Licensing pre-note:

This is an open source project, provided entirely freely, for everyone to use and contribute to.

If you make any changes that could benefit the community as a whole, please contribute upstream.

### The long version of the license follows:

The MIT License (MIT)

Copyright (c) 2026 Tjtoxshpilivili1

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

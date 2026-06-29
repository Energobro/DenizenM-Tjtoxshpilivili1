The Denizen Scripting Language - Paper Impl (Unofficial version!)
--------------------------------------------

An implementation of the Denizen Scripting Language for Paper servers, with strong Citizens interlinks to emphasize the power of using Denizen with NPCs!

**Version 1.3.3M**: Compatible with Paper 1.21.11, 26.1.2 and 26.2!

> [!IMPORTANT]
> Support for versions below **1.21.11** has been officially dropped.

> ⚡ **Full Compatibility:** Works flawlessly with **[Denizen Utilities](https://modrinth.com/plugin/denizen-utilities)** and **[Denizen Reflect](https://modrinth.com/plugin/denizen-reflect)** right after installation.

## 💎 Extension Highlights
* **Next-Gen Support:** Fully updated extension for VS Code by `Humususus`.
* **All-in-One:** Includes all the latest changes and community-requested tweaks.
* **Source & Guide:** Check it out on 🔗 **[GitHub: refined-denizenScript](https://github.com/Humususus/refined-denizenScript)**

## ✨ New Features
* **Events:**
    * Added support for the Paper-specific event `on player unchecked sign edits`.
    * Migrated `on player equips|unequips ...` to a generalized `on <entity> equips|unequips <item> or armor|helmet|chestplate|leggings|boots|body` syntax, extending support to all entities and adding the new body slot.
        * `<context.entity>` – The `EntityTag` involved.
        * `<context.slot>` – The name of the equipment slot.
        * `<context.new_item>` / `<context.old_item>` – The current and previous `ItemTag` in the slot.
* **Resource Pack:** Fully overhauled the logic for the `resourcepack` command to support adding multiple resource packs.
    * Added a new `add` argument to the `resourcepack` command to send additional resource packs to a player.
    * Added `PlayerTag.remove_resource_pack` mechanism to remove a specific resource pack by ID from a player.
    * Added `PlayerTag.remove_resource_packs` mechanism to remove all resource packs from a player.
* **Text & Formatting:**
    * New tags: `<&sprite>`, `<&shadow_color>`, `<&shadow_gradient>`, `<&dual_gradient>` and `<&head>`.
      * Sprite usage example: `<&sprite[minecraft:items:item/porkchop]>`
      * Shadow Color usage examples:
         * Simple variant - `<&shadow_color[#51a2ff]>`
         * With adjustable transparency - `<&shadow_color[<color[#51a2ff].with_alpha[254]>]>`
      * Shadow gradient usage examples:
         * Simple variant - `<&shadow_gradient[from=#51a2ff;to=#FFF085]>`
         * With adjustable transparency - `<&shadow_gradient[from=<color[#51a2ff].with_alpha[0]>;to=<color[#FFF085].with_alpha[254]>]>`
      * Dual gradient including Shadow color and Simple color gradients, usage examples:
         * Simple variant - `<&dual_gradient[from=#51a2ff;to=#FFF085;s_from=#FFF085;s_to=#51a2ff]>`
         * Tags `from` and `to` for simple color gradient adjusting.
         * Tags `s_from` and `s_to` for shadow color gradient adjusting.
      * Head usage examples:
         * Full Face Texture - `<&head[Tjtoxshpilivili1]>`
         * Only Face Texture (without surface pixels) - `<&head[!Tjtoxshpilivili1]>`
    * Added `.shadow_color`, `.shadow_gradient` and `.dual_gradient` tags to `ElementTag`.
* **Internal Migration:** Fully migrated to **Paper Components** for improved performance and modern API compatibility.

## 👾 Commands
* **Teleport**:
  * An `async` option has been added that teleports the player to unloaded chunks without causing server lag; sometimes there is a slight delay in execution. Details: https://docs.papermc.io/paper/dev/entity-teleport/
  * Added `~waitable` tag support for teleports utilizing the async parameter. This allows scripts to precisely track and wait until the asynchronous teleportation process is fully completed.
  * Usage examples:
    * Async teleport - `teleport <player> <player.location.random_offset[99999,0,99999]> async`
    * Waitable async teleport - `~teleport <player> <player.location.random_offset[99999,0,99999]> async`
* **Playeffect**:
  * Migrated to Paper's modern `ParticleBuilder` API, fixing a vanilla limitation where particles wouldn't render beyond 32 blocks from the player.
  * Added automatic handling for the `forced` parameter. Previously, even if you specified a high visibility radius (e.g., visibility:100) for a particle spawned 50 blocks away, it wouldn't display. Forcing the particle now ensures it correctly renders at extended distances.
  * Usage example - `playeffect effect:END_ROD quantity:100 <player.location.random_offset[50,0,50]> visibility:100 forced`

## 🧪 Items & Mechanics
* **Attributes:**
    * Added new `.rarity_color` tag for items, returns ColorTag.
       * Usage example - `<player.item_in_hand.rarity_color>`
* **Custom Model Data:**
  * Updated `custom_model_data` mechanism and property, returns a MapTag of `floats`, `strings`, `flags` (booleans), and `colors`.
  * Now accepts a `MapTag` containing `Lists` of `floats`, `strings`, `flags` (booleans), and `colors` (RGB) for advanced item model selection, while fully retaining backward compatibility for single-number inputs.
  * Usage examples:
    * Backward compatibility - `inventory adjust slot:hand custom_model_data:1000`
    * New format - `inventory adjust slot:hand custom_model_data:<map[floats=<list[1000]>;flags=<list[...]>;strings=<list[foo:bar]>;colors=<list[<color[127,0,0]>]>]>`
      * Simple usage - `custom_model_data:[floats=1000;flags=...;strings=foo:bar;colors=<color[127,0,0]>]`

## 🧹 Optimization & Cleanup
* **Core Optimization:** Implementation of custom optimizations across several internal classes.
* **Tags Modification:**
    * Added new sub-tag `.unsorted` to tag `.find_entities[<#.#>].within[<#.#>]` to bypass distance-based sorting. Use this for better performance when the order of entities in the list is not required.
    * Optimized the `.distance` tag by replacing `Math.pow` with direct multiplication. This reduces computational overhead and results in faster distance calculations across the script.
* **Removals:**
    * The `.scriptname` tag has been removed from all objects.
    * `Denizen ASAP Strong warning` has been fully removed.

## 🐛 Bug Fixes
* **showfake:** Fixed an issue where the command would trigger an error message despite functioning correctly.
* **fakeinternaldata:** Fixed a critical bug where the command was non-functional and threw an error.

## ⚠️ Known Issues (Official Denizen problem, not mine)
* `.has_potion_effect`: Currently not working.

**Learn about Denizen from the Beginner's guide:** https://guide.denizenscript.com/guides/background/index.html

#### Need help using Denizen? Try one of these places:

- **Discord** - chat room (Modern, strongly recommended): https://dsc.gg/dsng
- **My Telegram Channel** - spoilers, works with Denizen, new features: https://t.me/energ0bro
- **Denizen Home Page** - a link directory (Modern): https://denizenscript.com/
- **Meta Documentation (!! WITHOUT NEW CHANGES !!)** - command/tag/event/etc. search (Modern): https://meta.denizenscript.com/
- **Beginner's Guide** - text form (Modern): https://guide.denizenscript.com/

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

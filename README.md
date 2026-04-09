The Denizen Scripting Language - Paper Impl (Unofficial version!)
--------------------------------------------

An implementation of the Denizen Scripting Language for Paper servers, with strong Citizens interlinks to emphasize the power of using Denizen with NPCs!

**Version 1.3.2M**: Compatible with Paper 26.1.1 and 1.21.11!

> [!IMPORTANT]
> Support for versions below **1.21.11** has been officially dropped.

## ✨ New Features & API Improvements
* **Events:** Added support for the Paper-specific event `on player unchecked sign edits`.
* **Resource Pack:** Fully overhauled the logic for the `resourcepack` command to support adding multiple resource packs.
    * Added a new `add` argument to the `resourcepack` command to send additional resource packs to a player.
    * Added `PlayerTag.remove_resource_pack` mechanism to remove a specific resource pack by ID from a player.
    * Added `PlayerTag.remove_resource_packs` mechanism to remove all resource packs from a player.
* **Text & Formatting:**
    * New tags: `<&sprite>`, `<&shadow_color>`, and `<&player_head>`.
      * Sprite usage example: `<&sprite[minecraft:items:item/porkchop]>`
      * Shadow Color usage examples:
         * Simple variant - `<&shadow_color[#51a2ff]>`
         * With adjustable transparency - `<&shadow_color[<color[#51a2ff].with_alpha[254].hex>]>`
           **(!! DON'T FORGET ABOUT .hex TAG !!)**
      * Player Head usage examples:
         * Full Face Texture - `<&player_head[Tjtoxshpilivili1]>`
         * Only Face Texture (without surface pixels) - `<&player_head[!Tjtoxshpilivili1]>`
           **(!! DON'T FORGET ABOUT «!» !!)**
    * Added `.shadow_color` tag to `ElementTag`.
* **Internal Migration:** Fully migrated to **Paper Components** for improved performance and modern API compatibility.

## 🧪 Items & Mechanics
* **Attributes:**
    * Added new `.rarity_color` tag for items, returns ColorTag.
       * Usage example - `<player.item_in_hand.rarity_color>`

## 🧹 Optimization & Cleanup
* **Core Optimization:** Implementation of custom optimizations across several internal classes.
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
- **DenizenVSCode (extension for writing Denizen scripts in VS Code)**: https://github.com/DenizenScript/DenizenVSCode

### Building

- Built against JDK 21, using maven `pom.xml` as project file.
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

The Denizen Scripting Language - Spigot Impl
--------------------------------------------

An implementation of the Denizen Scripting Language for Spigot servers, with strong Citizens interlinks to emphasize the power of using Denizen with NPCs!

**Version 1.3.1M**: Compatible with Paper only on 1.21.11!

> [!IMPORTANT]
> Support for versions below **1.21.11** has been officially dropped.

## ✨ New Features & API Improvements
* **Events:** Added support for the Paper-specific event `on player unchecked sign edits`.
* **Resource Pack:** Fully overhauled the logic for the `resourcepack` command.
* **Text & Formatting:**
    * New tags: `<&sprite>`, `<&shadow_color>`, and `<&player_head>`.
    * Added `.shadow_color` attribute to `ElementTag`.
* **Internal Migration:** Fully migrated to **Paper Components** for improved performance and modern API compatibility.

## 🧪 Items & Mechanics
* **Enhanced Max Durability:** * Major modification to durability handling. 
    * You can now retrieve the actual durability of the item in hand.
    * New mechanism to set custom durability for any item:
      `inventory adjust slot:hand max_durability:4`
* **Attributes:**
    * Added new `rarity_color` parameter for items.
    * Fixed the `.rarity` parameter.

## 🧹 Optimization & Cleanup
* **Core Optimization:** Implementation of custom optimizations across several internal classes.
* **Removals:**
    * The `scriptname` tag has been removed from all objects.
    * `Denizen asap` has been fully removed.

## 🐛 Bug Fixes
* **showfake:** Fixed an issue where the command would trigger an error message despite functioning correctly.
* **fakeinternaldata:** Fixed a critical bug where the command was non-functional and threw an error.

## ⚠️ Known Issues (Official Denizen problem, not mine)
* `has_potion_effect`: Currently not working.
* `firework_data`: Functionality is under review/questionable.

**Learn about Denizen from the Beginner's guide:** https://guide.denizenscript.com/guides/background/index.html

#### Need help using Denizen? Try one of these places:

- **Discord** - chat room (Modern, strongly recommended): https://dsc.gg/dsng
- **Denizen Home Page** - a link directory (Modern): https://denizenscript.com/
- **Forum and script sharing** (Modern): https://forum.denizenscript.com/
- **Meta Documentation (!!WITHOUT NEW CHANGES!!)** - command/tag/event/etc. search (Modern): https://meta.denizenscript.com/
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

### Maven

```xml
    <repository>
        <id>citizens-repo</id>
        <url>https://maven.citizensnpcs.co/repo</url>
    </repository>
    <dependencies>
        <dependency>
            <groupId>com.denizenscript</groupId>
            <artifactId>denizen</artifactId>
            <version>1.3.1-SNAPSHOT</version>
            <type>jar</type>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
```

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

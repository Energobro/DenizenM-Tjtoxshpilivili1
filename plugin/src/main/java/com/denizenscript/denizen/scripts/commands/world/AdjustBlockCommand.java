package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.List;
import java.util.Map;

public class AdjustBlockCommand extends AbstractCommand {

    public AdjustBlockCommand() {
        setName("adjustblock");
        setSyntax("adjustblock [<location>|...] [<mechanism>](:<value>)/[<map>] (no_physics)");
        setRequiredArguments(2, 3);
        isProcedural = false;
        allowedDynamicPrefixes = true;
        autoCompile();
    }

    // <--[command]
    // @Name AdjustBlock
    // @Syntax adjustblock [<location>|...] [<mechanism>](:<value>)/[<map>] (no_physics)
    // @Required 2
    // @Maximum 3
    // @Short Adjusts a mechanism on the material of a block at the location.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/basics/mechanisms.html
    //
    // @Description
    // Adjusts a mechanism on the material of a block at the location.
    // That is, an equivalent to <@link command adjust>, but that directly applies a "MaterialTag" mechanism onto a block.
    //
    // Input a location or list of locations, and the mechanism to apply.
    //
    // You can optionally adjust a MapTag of mechanisms to values, which are applied in the order the map lists them.
    // Physics, if not disabled, apply once per location after the whole map, not once per mechanism.
    //
    // Use the "no_physics" argument to indicate that the change should not apply a physics update.
    // If not specified, physics will apply to the block and nearby blocks.
    //
    // @Tags
    // <LocationTag.material>
    //
    // @Usage
    // Use to put snow on the block at the player's feet.
    // - adjustblock <player.location.below> snowy:true
    //
    // @Usage
    // Use to switch on the lever that the player is looking at, without actually providing redstone power.
    // - adjustblock <player.cursor_on> switched:true no_physics
    //
    // @Usage
    // Use to apply several mechanisms to one block at once.
    // - adjustblock <player.cursor_on> <map[waterlogged=true;facing=north]>
    //
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.add(PropertyParser.propertiesByClass.get(MaterialTag.class).propertiesByMechanism.keySet());
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("locations") @ArgLinear @ArgSubType(LocationTag.class) List<LocationTag> locations,
                                   @ArgName("mechanism") @ArgRaw @ArgLinear @ArgDefaultNull String mechanismRaw,
                                   @ArgName("no_physics") boolean noPhysics) {
        if (mechanismRaw == null) {
            throw new InvalidArgumentsRuntimeException("You must specify a mechanism!");
        }
        MapTag mechanismMap = null;
        Argument mechanismArgument = null;
        if (mechanismRaw.startsWith("map@")) {
            mechanismMap = MapTag.valueOf(mechanismRaw, scriptEntry.getContext());
            if (mechanismMap == null) {
                throw new InvalidArgumentsRuntimeException("Invalid map of mechanisms: " + mechanismRaw);
            }
        }
        else {
            mechanismArgument = new Argument(mechanismRaw);
        }
        boolean doPhysics = !noPhysics;
        for (LocationTag location : locations) {
            Block block = location.getBlock();
            BlockData data = block.getBlockData();
            MaterialTag specialMaterial = new MaterialTag(data);
            if (mechanismMap != null) {
                for (Map.Entry<StringHolder, ObjectTag> entry : mechanismMap.entrySet()) {
                    specialMaterial.safeAdjust(new Mechanism(entry.getKey().str, entry.getValue(), scriptEntry.getContext()));
                }
            }
            else {
                boolean hasValue = mechanismArgument.hasPrefix();
                specialMaterial.safeAdjust(new Mechanism(hasValue ? mechanismArgument.getPrefix().getValue() : mechanismArgument.getValue(),
                        hasValue ? mechanismArgument.object : null, scriptEntry.getContext()));
            }
            if (doPhysics) {
                block.setBlockData(data, false);
                applyPhysicsAt(location);
            }
            else {
                ModifyBlockCommand.setBlock(block.getLocation(), specialMaterial, false, null, 0);
            }
        }
    }

    public static void applyPhysicsAt(Location location) {
        NMSHandler.blockHelper.applyPhysics(location);
        NMSHandler.blockHelper.applyPhysics(location.clone().add(1, 0, 0));
        NMSHandler.blockHelper.applyPhysics(location.clone().add(-1, 0, 0));
        NMSHandler.blockHelper.applyPhysics(location.clone().add(0, 0, 1));
        NMSHandler.blockHelper.applyPhysics(location.clone().add(0, 0, -1));
        NMSHandler.blockHelper.applyPhysics(location.clone().add(0, 1, 0));
        NMSHandler.blockHelper.applyPhysics(location.clone().add(0, -1, 0));
    }
}

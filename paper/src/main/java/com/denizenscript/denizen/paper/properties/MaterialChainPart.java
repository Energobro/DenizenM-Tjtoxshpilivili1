package com.denizenscript.denizen.paper.properties;

import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.objects.properties.material.MaterialProperty;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.block.data.SideChaining;

public class MaterialChainPart extends MaterialProperty<ElementTag> {

    // <--[property]
    // @object MaterialTag
    // @name chain_part
    // @input ElementTag
    // @description
    // Controls which part of a merged row of shelves a shelf is: LEFT, CENTER, RIGHT, or UNCONNECTED.
    // Vanilla calls this block state 'side_chain'.
    // Shelves merge when powered: up to three that face the same way join into one row, and a shelf standing on its own is UNCONNECTED.
    // The server sets this itself as shelves are powered and unpowered, so setting it by hand is for fake blocks and for items.
    // -->

    public static boolean describes(MaterialTag material) {
        return material.getModernData() instanceof SideChaining;
    }

    @Override
    public ElementTag getPropertyValue() {
        return new ElementTag(as(SideChaining.class).getSideChain());
    }

    @Override
    public void setPropertyValue(ElementTag value, Mechanism mechanism) {
        if (mechanism.requireEnum(SideChaining.ChainPart.class)) {
            as(SideChaining.class).setSideChain(value.asEnum(SideChaining.ChainPart.class));
        }
    }

    @Override
    public String getPropertyId() {
        return "chain_part";
    }

    public static void register() {
        autoRegister("chain_part", MaterialChainPart.class, ElementTag.class, false);
    }
}

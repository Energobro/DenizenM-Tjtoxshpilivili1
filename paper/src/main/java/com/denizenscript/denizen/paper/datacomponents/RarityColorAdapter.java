package com.denizenscript.denizen.paper.datacomponents;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ColorTag;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemRarity;

public class RarityColorAdapter extends DataComponentAdapter.Valued<ColorTag, ItemRarity> {

    // <--[property]
    // @object ItemTag
    // @name rarity_color
    // @input ColorTag
    // @description
    // Controls an item's rarity based on its associated ColorTag.
    // Returns the ColorTag associated with the item's current rarity.
    // @mechanism
    // Provide no input to reset the item to its default value.
    // -->

    public RarityColorAdapter() {
        super(ColorTag.class, DataComponentTypes.RARITY, "rarity_color");
    }

    @Override
    public ColorTag toDenizen(ItemRarity value) {
        if (value == null) {
            return new ColorTag(255, 255, 255);
        }
        return switch (value) {
            case COMMON -> new ColorTag(255, 255, 255);
            case UNCOMMON -> new ColorTag(255, 255, 85);
            case RARE -> new ColorTag(85, 255, 255);
            case EPIC -> new ColorTag(255, 85, 255);
        };
    }

    @Override
    public ItemRarity fromDenizen(ColorTag value, Mechanism mechanism) {
        if (value == null) return null;
        int r = value.red;
        int g = value.green;
        int b = value.blue;

        if (r == 255 && g == 255 && b == 255) return ItemRarity.COMMON;
        if (r == 255 && g == 255 && b == 85) return ItemRarity.UNCOMMON;
        if (r == 85 && g == 255 && b == 255) return ItemRarity.RARE;
        if (r == 255 && g == 85 && b == 255) return ItemRarity.EPIC;
        mechanism.echoError("Invalid rarity color provided. Must be an exact match for COMMON, UNCOMMON, RARE, or EPIC colors.");
        return null;
    }
}
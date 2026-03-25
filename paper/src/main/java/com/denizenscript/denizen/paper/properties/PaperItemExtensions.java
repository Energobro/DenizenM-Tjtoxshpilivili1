package com.denizenscript.denizen.paper.properties;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import java.awt.Color;

public class PaperItemExtensions {

    public static void register() {

        // <--[tag]
        // @attribute <ItemTag.rarity>
        // @returns ElementTag
        // @group paper
        // @Plugin Paper
        // @description
        // Returns the rarity of an item, as "common", "uncommon", "rare", or "epic".
        // -->
        ItemTag.tagProcessor.registerTag(ElementTag.class, "rarity", (attribute, item) -> {
            if (item == null) {
                return new ElementTag("COMMON");
            }

            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) {
                return new ElementTag("COMMON");
            }
            ItemRarity rarity = stack.getData(DataComponentTypes.RARITY);
            return new ElementTag(rarity != null ? rarity.name() : "COMMON");
        });

        ItemTag.tagProcessor.registerTag(ColorTag.class, "rarity_color", (attribute, item) -> {
            try {
                if (item == null) {
                    return new ColorTag(255, 255, 255); // COMMON = белый
                }
                ItemStack stack = item.getItemStack();
                if (stack == null || stack.isEmpty()) {
                    return new ColorTag(255, 255, 255);
                }
                ItemRarity rarity = stack.getData(DataComponentTypes.RARITY);

                if (rarity == null) {
                    rarity = ItemRarity.COMMON;
                }
                return switch (rarity) {
                    case COMMON -> new ColorTag(255, 255, 255);
                    case UNCOMMON -> new ColorTag(255, 255, 85);
                    case RARE -> new ColorTag(85, 255, 255);
                    case EPIC -> new ColorTag(255, 85, 255);
                };

            } catch (Exception e) {
                Debug.echoError(e);
                return new ColorTag(255, 255, 255);
            }
        });
    }

    private static java.awt.Color getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> Color.WHITE;
            case UNCOMMON -> new Color(255, 255, 85);
            case RARE -> new Color(85, 255, 255);
            case EPIC -> new Color(255, 85, 255);
        };
    }
}

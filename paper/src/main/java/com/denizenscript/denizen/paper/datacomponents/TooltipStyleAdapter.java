package com.denizenscript.denizen.paper.datacomponents;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;

public class TooltipStyleAdapter extends DataComponentAdapter.Valued<ElementTag, Key> {

    // <--[property]
    // @object ItemTag
    // @name tooltip_style
    // @input ElementTag
    // @description
    // Controls the namespaced key of the tooltip style (background and frame) used for the item's tooltip, see <@link language Item Components>.
    // This can be used to display custom tooltip styles from your own resource packs.
    // @mechanism
    // Provide no input to reset the item to its default value.
    // -->

    public TooltipStyleAdapter() {
        super(ElementTag.class, DataComponentTypes.TOOLTIP_STYLE, "tooltip_style");
    }

    @Override
    public ElementTag toDenizen(Key value) {
        return new ElementTag(value.asMinimalString(), true);
    }

    @Override
    public Key fromDenizen(ElementTag value, Mechanism mechanism) {
        return Utilities.parseNamespacedKey(value.asString());
    }
}

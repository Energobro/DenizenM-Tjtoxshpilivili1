package com.denizenscript.denizen.paper.datacomponents;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import io.papermc.paper.datacomponent.DataComponentTypes;

public class GlintOverrideAdapter extends DataComponentAdapter.Valued<ElementTag, Boolean> {

    // <--[property]
    // @object ItemTag
    // @name enchantment_glint_override
    // @input ElementTag(Boolean)
    // @description
    // Controls whether an item shows the enchantment glint, regardless of whether it is actually enchanted, see <@link language Item Components>.
    // @mechanism
    // Provide no input to reset the item to its default value.
    // -->

    public GlintOverrideAdapter() {
        super(ElementTag.class, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, "enchantment_glint_override");
    }

    @Override
    public ElementTag toDenizen(Boolean value) {
        return new ElementTag(value);
    }

    @Override
    public Boolean fromDenizen(ElementTag value, Mechanism mechanism) {
        return mechanism.requireBoolean() ? value.asBoolean() : null;
    }
}

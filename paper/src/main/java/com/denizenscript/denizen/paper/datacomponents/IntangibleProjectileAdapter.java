package com.denizenscript.denizen.paper.datacomponents;

import io.papermc.paper.datacomponent.DataComponentTypes;

public class IntangibleProjectileAdapter extends DataComponentAdapter.NonValued {

    // <--[property]
    // @object ItemTag
    // @name intangible_projectile
    // @input ElementTag(Boolean)
    // @description
    // Controls whether a projectile item, when fired, cannot be picked up by players, see <@link language Item Components>.
    // @mechanism
    // Provide no input to reset the item to its default value.
    // -->

    public IntangibleProjectileAdapter() {
        super(DataComponentTypes.INTANGIBLE_PROJECTILE, "intangible_projectile");
    }
}

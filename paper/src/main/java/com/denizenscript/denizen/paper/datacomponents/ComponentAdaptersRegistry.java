package com.denizenscript.denizen.paper.datacomponents;

public class ComponentAdaptersRegistry {

    public static void register() {
        DataComponentAdapter.register(new FoodAdapter());
        DataComponentAdapter.register(new GliderAdapter());
        DataComponentAdapter.register(new GlintOverrideAdapter());
        DataComponentAdapter.register(new IntangibleProjectileAdapter());
        DataComponentAdapter.register(new ItemModelAdapter());
        DataComponentAdapter.register(new ItemNameAdapter());
        DataComponentAdapter.register(new MaxDurabilityAdapter());
        DataComponentAdapter.register(new MaxStackSizeAdapter());
        DataComponentAdapter.register(new RarityAdapter());
        DataComponentAdapter.register(new RarityColorAdapter());
        DataComponentAdapter.register(new CustomModelDataAdapter());
        DataComponentAdapter.register(new TooltipStyleAdapter());
    }
}

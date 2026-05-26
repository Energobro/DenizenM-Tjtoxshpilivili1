package com.denizenscript.denizen.paper.datacomponents;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Color;

public class CustomModelDataAdapter extends DataComponentAdapter.Valued<MapTag, CustomModelData> {

    // <--[property]
    // @object ItemTag
    // @name custom_model_data
    // @input MapTag
    // @description
    // Controls the custom model data component of the item.
    // The map can include keys:
    // - "floats", ListTag of ElementTag(Decimal) - float values for range_dispatch model type.
    // - "strings", ListTag of ElementTag - string values for select model type.
    // - "flags", ListTag of ElementTag(Boolean) - boolean values for condition model type.
    // - "colors", ListTag of ColorTag - RGB color values for model tints.
    // Alternatively, provide a single number to set only a float value (legacy behavior).
    // @mechanism
    // Provide no input to reset the item to its default value.
    // -->

    public CustomModelDataAdapter() {
        super(MapTag.class, DataComponentTypes.CUSTOM_MODEL_DATA, "custom_model_data");
    }

    @Override
    public MapTag toDenizen(CustomModelData value) {
        MapTag map = new MapTag();
        if (!value.floats().isEmpty()) {
            ListTag floats = new ListTag();
            for (float f : value.floats()) {
                floats.addObject(new ElementTag(f));
            }
            map.putObject("floats", floats);
        }
        if (!value.strings().isEmpty()) {
            ListTag strings = new ListTag();
            for (String s : value.strings()) {
                strings.addObject(new ElementTag(s));
            }
            map.putObject("strings", strings);
        }
        if (!value.flags().isEmpty()) {
            ListTag flags = new ListTag();
            for (boolean b : value.flags()) {
                flags.addObject(new ElementTag(b));
            }
            map.putObject("flags", flags);
        }
        if (!value.colors().isEmpty()) {
            ListTag colors = new ListTag();
            for (Color c : value.colors()) {
                colors.addObject(ColorTag.fromRGB(c.asRGB()));
            }
            map.putObject("colors", colors);
        }
        return map;
    }

    @Override
    public CustomModelData fromDenizen(MapTag value, Mechanism mechanism) {
        CustomModelData.Builder builder = CustomModelData.customModelData();
        if (value.containsKey("floats")) {
            ListTag floats = value.getObject("floats").asType(ListTag.class, mechanism.context);
            if (floats != null) {
                for (String entry : floats) {
                    ElementTag el = new ElementTag(entry);
                    if (el.isFloat()) {
                        builder.addFloat(el.asFloat());
                    }
                    else {
                        mechanism.echoError("Invalid float in custom_model_data floats: " + entry);
                    }
                }
            }
        }

        if (value.containsKey("strings")) {
            ListTag strings = value.getObject("strings").asType(ListTag.class, mechanism.context);
            if (strings != null) {
                for (String entry : strings) {
                    builder.addString(entry);
                }
            }
        }

        if (value.containsKey("flags")) {
            ListTag flags = value.getObject("flags").asType(ListTag.class, mechanism.context);
            if (flags != null) {
                for (String entry : flags) {
                    ElementTag el = new ElementTag(entry);
                    if (el.isBoolean()) {
                        builder.addFlag(el.asBoolean());
                    }
                    else {
                        mechanism.echoError("Invalid boolean in custom_model_data flags: " + entry);
                    }
                }
            }
        }

        if (value.containsKey("colors")) {
            ListTag colors = value.getObject("colors").asType(ListTag.class, mechanism.context);
            if (colors != null) {
                for (String entry : colors) {
                    ColorTag color = ColorTag.valueOf(entry, mechanism.context);
                    if (color != null) {
                        builder.addColor(Color.fromRGB(color.asRGB()));
                    }
                    else {
                        mechanism.echoError("Invalid color in custom_model_data colors: " + entry);
                    }
                }
            }
        }
        return builder.build();
    }
}
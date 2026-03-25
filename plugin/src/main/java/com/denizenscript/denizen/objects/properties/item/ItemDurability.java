package com.denizenscript.denizen.objects.properties.item;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.tags.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemDurability implements Property {

    public static boolean describes(ObjectTag item) {
        return item instanceof ItemTag
                && ((ItemTag) item).isRepairable();
    }

    public static ItemDurability getFrom(ObjectTag _item) {
        if (!describes(_item)) {
            return null;
        }
        else {
            return new ItemDurability((ItemTag) _item);
        }
    }

    public static final String[] handledTags = new String[] {
            "durability", "max_durability"
    };

    public static final String[] handledMechs = new String[] {
            "durability", "max_durability"
    };

    public ItemDurability(ItemTag _item) {
        item = _item;
    }

    ItemTag item;

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {

        if (attribute == null) {
            return null;
        }

        // <--[tag]
        // @attribute <ItemTag.durability>
        // @returns ElementTag(Number)
        // @mechanism ItemTag.durability
        // @group properties
        // @description
        // Returns the current durability (number of uses) on the item.
        // -->
        if (attribute.startsWith("durability")) {
            return new ElementTag(((Damageable) item.getItemMeta()).getDamage())
                    .getObjectAttribute(attribute.fulfill(1));
        }

        // <--[tag]
        // @attribute <ItemTag.max_durability>
        // @returns ElementTag(Number)
        // @group properties
        // @description
        // Returns the maximum durability (number of uses) of this item.
        // For use with <@link tag ItemTag.durability> and <@link mechanism ItemTag.durability>.
        // -->
        if (attribute.startsWith("max_durability")) {
            int maxDurability;
            ItemStack bukkitItem = item.getItemStack();
            ItemMeta meta = bukkitItem.getItemMeta();
            if (meta instanceof Damageable damageable && damageable.hasMaxDamage()) {
                maxDurability = damageable.getMaxDamage();
            }
            else {
                maxDurability = bukkitItem.getType().getMaxDurability();
            }

            return new ElementTag(maxDurability)
                    .getObjectAttribute(attribute.fulfill(1));
        }

        return null;
    }

    @Override
    public String getPropertyString() {
        int durability = ((Damageable) item.getItemMeta()).getDamage();
        if (durability != 0) {
            return String.valueOf(durability);
        }
        else {
            return null;
        }
    }

    @Override
    public String getPropertyId() {
        return "durability";
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object ItemTag
        // @name durability
        // @input ElementTag(Number)
        // @description
        // Changes the durability of damageable items.
        // @tags
        // <ItemTag.durability>
        // <ItemTag.max_durability>
        // <ItemTag.repairable>
        // -->
        if (mechanism.matches("durability") && mechanism.requireInteger()) {
            item.setDurability((short) mechanism.getValue().asInt());
        }
        if (mechanism.matches("max_durability") && mechanism.requireInteger()) {
            int newMax = mechanism.getValue().asInt();
            ItemStack bitem = item.getItemStack();
            ItemMeta meta = bitem.getItemMeta();

            if (meta instanceof Damageable damageable) {
                damageable.setMaxDamage(newMax > 0 ? newMax : null);
                bitem.setItemMeta(damageable);
            }
        }
    }
}

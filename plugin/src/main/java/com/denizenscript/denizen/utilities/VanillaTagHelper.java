package com.denizenscript.denizen.utilities;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VanillaTagHelper {

    public static volatile HashMap<Material, HashSet<String>> tagsByMaterial = new HashMap<>();

    public static volatile HashMap<String, HashSet<Material>> materialTagsByKey = new HashMap<>();

    public static volatile HashMap<EntityType, HashSet<String>> tagsByEntity = new HashMap<>();

    public static volatile HashMap<String, HashSet<EntityType>> entityTagsByKey = new HashMap<>();

    private static final Object writeLock = new Object();

    static {
        loadTagsCache();
    }

    static <K, V> HashMap<K, HashSet<V>> deepCopy(HashMap<K, HashSet<V>> source) {
        HashMap<K, HashSet<V>> copy = new HashMap<>(source.size());
        for (Map.Entry<K, HashSet<V>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public static void addOrUpdateMaterialTag(Tag<Material> tag) {
        synchronized (writeLock) {
            HashMap<Material, HashSet<String>> byObject = deepCopy(tagsByMaterial);
            HashMap<String, HashSet<Material>> byKey = deepCopy(materialTagsByKey);
            if (byKey.containsKey(tag.getKey().getKey())) {
                update(tag, byObject, byKey);
            }
            else {
                add(tag, byObject, byKey);
            }
            tagsByMaterial = byObject;
            materialTagsByKey = byKey;
        }
    }

    public static void addOrUpdateEntityTag(Tag<EntityType> tag) {
        synchronized (writeLock) {
            HashMap<EntityType, HashSet<String>> byObject = deepCopy(tagsByEntity);
            HashMap<String, HashSet<EntityType>> byKey = deepCopy(entityTagsByKey);
            if (byKey.containsKey(tag.getKey().getKey())) {
                update(tag, byObject, byKey);
            }
            else {
                add(tag, byObject, byKey);
            }
            tagsByEntity = byObject;
            entityTagsByKey = byKey;
        }
    }

    // TODO: once 1.21 is the minimum supported version, remove this and related methods
    static <T extends Keyed> void update(Tag<T> tag, HashMap<T, HashSet<String>> tagByObj, HashMap<String, HashSet<T>> objByTag) {
        String tagName = Utilities.namespacedKeyToString(tag.getKey());
        Set<T> objs = objByTag.get(tagName);
        if (objs == null) {
            return;
        }
        for (T obj : objs) {
            Set<String> tags = tagByObj.get(obj);
            if (tags.size() == 1) {
                tagByObj.remove(obj);
            }
            else {
                tags.remove(tagName);
            }
        }
        Set<T> newObjs = tag.getValues();
        for (T obj : newObjs) {
            tagByObj.computeIfAbsent(obj, k -> new HashSet<>()).add(tagName);
        }
        objs.clear();
        objs.addAll(newObjs);
    }

    public static void updateMaterialTag(Tag<Material> tag) {
        synchronized (writeLock) {
            HashMap<Material, HashSet<String>> byObject = deepCopy(tagsByMaterial);
            HashMap<String, HashSet<Material>> byKey = deepCopy(materialTagsByKey);
            update(tag, byObject, byKey);
            tagsByMaterial = byObject;
            materialTagsByKey = byKey;
        }
    }

    public static void updateEntityTag(Tag<EntityType> tag) {
        synchronized (writeLock) {
            HashMap<EntityType, HashSet<String>> byObject = deepCopy(tagsByEntity);
            HashMap<String, HashSet<EntityType>> byKey = deepCopy(entityTagsByKey);
            update(tag, byObject, byKey);
            tagsByEntity = byObject;
            entityTagsByKey = byKey;
        }
    }

    static <T extends Keyed> void add(Tag<T> tag, HashMap<T, HashSet<String>> tagByObj, HashMap<String, HashSet<T>> objByTag) {
        String tagName = Utilities.namespacedKeyToString(tag.getKey());
        objByTag.computeIfAbsent(tagName, (k) -> new HashSet<>()).addAll(tag.getValues());
        for (T obj : tag.getValues()) {
            tagByObj.computeIfAbsent(obj, (k) -> new HashSet<>()).add(tagName);
        }
    }

    public static void loadTagsCache() {
        HashMap<Material, HashSet<String>> newTagsByMaterial = new HashMap<>();
        HashMap<String, HashSet<Material>> newMaterialTagsByKey = new HashMap<>();
        HashMap<EntityType, HashSet<String>> newTagsByEntity = new HashMap<>();
        HashMap<String, HashSet<EntityType>> newEntityTagsByKey = new HashMap<>();
        for (Tag<Material> tag : Bukkit.getTags("blocks", Material.class)) {
            add(tag, newTagsByMaterial, newMaterialTagsByKey);
        }
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_18)) { // Note: existed on prior versions, but was bugged
            for (Tag<EntityType> tag : Bukkit.getTags("entity_types", EntityType.class)) {
                add(tag, newTagsByEntity, newEntityTagsByKey);
            }
        }
        for (Tag<Material> tag : Bukkit.getTags("items", Material.class)) {
            add(tag, newTagsByMaterial, newMaterialTagsByKey);
        }
        synchronized (writeLock) {
            tagsByMaterial = newTagsByMaterial;
            materialTagsByKey = newMaterialTagsByKey;
            tagsByEntity = newTagsByEntity;
            entityTagsByKey = newEntityTagsByKey;
        }
    }
}

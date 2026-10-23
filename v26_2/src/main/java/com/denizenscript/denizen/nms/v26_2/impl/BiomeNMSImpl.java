package com.denizenscript.denizen.nms.v26_2.impl;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.abstracts.BiomeNMS;
import com.denizenscript.denizen.nms.v26_2.Handler;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.TriState;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.EntityType;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeNMSImpl extends BiomeNMS {

    public static final MethodHandle CHUNK_SET_NOISE_BIOME = Handler.reflectPaperRenamed(ChunkAccess.class, "setBiome", "setNoiseBiome", int.class, int.class, int.class, Holder.class);
    public static final MethodHandle BIOME_CLIMATESETTINGS_CONSTRUCTOR = ReflectionHelper.getConstructor(Biome.ClimateSettings.class, boolean.class, float.class, Biome.TemperatureModifier.class, float.class);
    public static final MethodHandle MAPPED_REGISTRY_REGISTRATION_INFOS = ReflectionHelper.getFields(MappedRegistry.class).getGetter("registrationInfos");
    public static final MethodHandle BIOME_ATTRIBUTES_SETTER = ReflectionHelper.getFields(Biome.class).getSetter("attributes");
    public static final Map<String, EnvironmentAttribute<?>> ATTRIBUTE_CACHE = new HashMap<>();
    public static final MethodHandle LEVEL_ATTRIBUTES_SETTER = ReflectionHelper.FieldCache.toHandleSetter(ReflectionHelper.getFields(ServerLevel.class).getFirstOfType(EnvironmentAttributeSystem.class));
    private static final Set<EnvironmentAttribute<?>> LAYERED_ATTRIBUTES = ConcurrentHashMap.newKeySet();

    static {
        try {
            for (Field field : EnvironmentAttributes.class.getFields()) {
                if (field.getType().equals(EnvironmentAttribute.class)) {
                    field.setAccessible(true);
                    EnvironmentAttribute<?> attribute = (EnvironmentAttribute<?>) field.get(null);
                    if (attribute != null) {
                        ATTRIBUTE_CACHE.put(field.getName().toUpperCase(), attribute);
                    }
                }
            }
        } catch (Exception e) {
            Debug.echoError(e);
        }
    }

    public Holder.Reference<Biome> biomeHolder;
    public ServerLevel world;

    public BiomeNMSImpl(ServerLevel world, NamespacedKey key) {
        super(world.getWorld(), key);
        this.world = world;
        this.biomeHolder = getBiomeRegistry().get(ResourceKey.create(Registries.BIOME, CraftNamespacedKey.toMinecraft(key))).orElse(null);
    }

    private MappedRegistry<Biome> getBiomeRegistry() {
        return (MappedRegistry<Biome>) world.registryAccess().lookupOrThrow(Registries.BIOME);
    }

    @Override
    public DownfallType getDownfallTypeAt(Location location) {
        Biome.Precipitation precipitation = biomeHolder.value().getPrecipitationAt(Handler.toBlockPos(location), world.getSeaLevel());
        return switch (precipitation) {
            case RAIN -> DownfallType.RAIN;
            case SNOW -> DownfallType.SNOW;
            case NONE -> DownfallType.NONE;
        };
    }

    @Override
    public float getHumidity() {
        return biomeHolder.value().climateSettings.downfall();
    }

    @Override
    public float getBaseTemperature() {
        return biomeHolder.value().getBaseTemperature();
    }

    @Override
    public float getTemperatureAt(Location location) {
        return biomeHolder.value().getTemperature(Handler.toBlockPos(location), world.getSeaLevel());
    }

    @Override
    public boolean hasDownfall() {
        return biomeHolder.value().hasPrecipitation();
    }

    @Override
    public List<EntityType> getAmbientEntities() {
        return getSpawnableEntities(MobCategory.AMBIENT);
    }

    @Override
    public List<EntityType> getCreatureEntities() {
        return getSpawnableEntities(MobCategory.CREATURE);
    }

    @Override
    public List<EntityType> getMonsterEntities() {
        return getSpawnableEntities(MobCategory.MONSTER);
    }

    @Override
    public List<EntityType> getWaterEntities() {
        return getSpawnableEntities(MobCategory.WATER_CREATURE);
    }

    @Override
    public int getFoliageColor() {
        // Check if the biome already has a default color
        if (biomeHolder.value().getFoliageColor() != 0) {
            return biomeHolder.value().getFoliageColor();
        }
        // Based on net.minecraft.world.level.biome.Biome#getFoliageColorFromTexture()
        float temperature = clampColor(getBaseTemperature());
        float humidity = clampColor(getHumidity());
        // Based on net.minecraft.world.level.FoliageColor#get()
        humidity *= temperature;
        int humidityValue = (int)((1.0f - humidity) * 255.0f);
        int temperatureValue = (int)((1.0f - temperature) * 255.0f);
        int index = temperatureValue << 8 | humidityValue;
        return index >= 65536 ? 4764952 : getColor(index / 256, index % 256).asRGB();
    }

    public void setClimate(boolean hasPrecipitation, float temperature, Biome.TemperatureModifier temperatureModifier, float downfall) {
        try {
            Object newClimate = BIOME_CLIMATESETTINGS_CONSTRUCTOR.invoke(hasPrecipitation, temperature, temperatureModifier, downfall);
            ReflectionHelper.setFieldValue(Biome.class, "climateSettings", biomeHolder.value(), newClimate);
            setNetworkedRegistrationInfo();
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    @Override
    public void setHumidity(float humidity) {
        setClimate(hasDownfall(), getBaseTemperature(), getTemperatureModifier(), humidity);
    }

    @Override
    public void setBaseTemperature(float baseTemperature) {
        setClimate(hasDownfall(), baseTemperature, getTemperatureModifier(), getHumidity());
    }

    @Override
    public void setHasDownfall(boolean hasDownfall) {
        setClimate(hasDownfall, getBaseTemperature(), getTemperatureModifier(), getHumidity());
    }

    @Override
    public void setFoliageColor(int color) {
        BiomeSpecialEffects nmsCurrEffects = biomeHolder.value().getSpecialEffects();
        BiomeSpecialEffects nmsNewEffects = new BiomeSpecialEffects(
                nmsCurrEffects.waterColor(), Optional.of(color), nmsCurrEffects.dryFoliageColorOverride(), nmsCurrEffects.grassColorOverride(), nmsCurrEffects.grassColorModifier()
        );
        ReflectionHelper.setFieldValue(Biome.class, "specialEffects", biomeHolder.value(), nmsNewEffects);
        setNetworkedRegistrationInfo();
    }

    @Override
    public void setAttribute(BiomeNMS biomeNMS, String name, ObjectTag value) {
        EnvironmentAttribute<?> attribute = ATTRIBUTE_CACHE.get(name.toUpperCase());
        if (attribute == null) {
            Debug.echoError("Environment attribute '" + name + "' does not exist.");
            return;
        }

        var defaultValue = attribute.defaultValue();
        String expectedTypeName = defaultValue.getClass().getSimpleName();

        final ElementTag elementTag = value.asElement();
        Object object = switch (defaultValue) {
            case Integer ignored -> {
                expectedTypeName = "ColorTag";
                var color = ColorTag.valueOf(elementTag.asString(), null);
                yield color != null ? color.asARGB() : null;
            }
            case Float ignored -> elementTag.isFloat() ? elementTag.asFloat() : null;
            case Boolean ignored -> elementTag.isBoolean() ? elementTag.asBoolean() : null;
            case AmbientSounds ignored -> {
                expectedTypeName = "MapTag of ambient sound settings";
                yield ambientSoundsFor(value);
            }
            case BackgroundMusic ignored -> {
                expectedTypeName = "MapTag of background music settings";
                yield backgroundMusicFor(value);
            }
            case BedRule ignored -> {
                expectedTypeName = "MapTag of bed rules";
                yield bedRuleFor(value);
            }
            case MoonPhase ignored -> {
                expectedTypeName = "MoonPhase name";
                yield elementTag.asEnum(MoonPhase.class);
            }
            case TriState ignored -> {
                expectedTypeName = "TRUE, FALSE, or DEFAULT";
                yield elementTag.asEnum(TriState.class);
            }
            case Activity ignored -> {
                expectedTypeName = "villager activity name";
                yield activityFor(elementTag);
            }
            case ParticleOptions ignored -> {
                expectedTypeName = "particle name";
                yield particleFor(value);
            }
            case List<?> ignored -> {
                expectedTypeName = "ListTag of ambient particles";
                yield ambientParticlesFor(value);
            }
            default -> null;
        };

        if (object == null) {
            Debug.echoError("Invalid value format for attribute '" + name + "'. Expected type: "
                    + expectedTypeName + ", but got: '" + value + "'");
            return;
        }

        object = ((EnvironmentAttribute) attribute).sanitizeValue(object);
        ((BiomeNMSImpl) biomeNMS).setEnvironmentAttribute((EnvironmentAttribute) attribute, object);
    }

    @Override
    public ObjectTag getAttribute(BiomeNMS biomeNMS, String name) {
        EnvironmentAttribute<?> attribute = ATTRIBUTE_CACHE.get(name.toUpperCase());
        if (attribute == null) {
            return null;
        }

        Object value = ((BiomeNMSImpl) biomeNMS).getEnvironmentAttribute(attribute);
        if (value == null) {
            return null;
        }

        return switch (value) {
            case Integer integer -> ColorTag.fromARGB(integer);
            case Float f -> new ElementTag(f);
            case Boolean b -> new ElementTag(b);
            case AmbientSounds sounds -> ambientSoundsToTag(sounds);
            case BackgroundMusic music -> backgroundMusicToTag(music);
            case BedRule rule -> bedRuleToTag(rule);
            case MoonPhase phase -> new ElementTag(phase);
            case TriState state -> new ElementTag(state);
            case Activity activity -> new ElementTag(CoreUtilities.toUpperCase(activity.getName()), true);
            case ParticleOptions particle -> particleToTag(particle);
            case List<?> list -> ambientParticlesToTag(list);
            default -> null;
        };
    }

    private static int intFor(MapTag map, String key, int defaultValue) {
        ObjectTag input = map.getObject(key);
        return input == null ? defaultValue : input.asElement().asInt();
    }

    private static double doubleFor(MapTag map, String key, double defaultValue) {
        ObjectTag input = map.getObject(key);
        return input == null ? defaultValue : input.asElement().asDouble();
    }

    private static float floatFor(MapTag map, String key, float defaultValue) {
        ObjectTag input = map.getObject(key);
        return input == null ? defaultValue : input.asElement().asFloat();
    }

    private static boolean boolFor(MapTag map, String key, boolean defaultValue) {
        ObjectTag input = map.getObject(key);
        return input == null ? defaultValue : input.asElement().asBoolean();
    }

    private static Holder<SoundEvent> soundFor(ObjectTag input) {
        if (input == null) {
            Debug.echoError("No sound specified for a biome environment attribute.");
            return null;
        }
        Sound sound = Utilities.elementToEnumlike(input.asElement(), Sound.class, false);
        if (sound == null) {
            Debug.echoError("Invalid sound '" + input + "' specified for a biome environment attribute.");
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.get(CraftNamespacedKey.toMinecraft(sound.getKey())).orElse(null);
    }

    private static ElementTag soundToTag(Holder<SoundEvent> holder) {
        Identifier identifier = holder.unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (identifier == null) {
            return new ElementTag(holder.getRegisteredName(), true);
        }
        Sound sound = Registry.SOUNDS.get(CraftNamespacedKey.fromMinecraft(identifier));
        return sound != null ? Utilities.enumLikeToLegacyElement(sound) : new ElementTag(identifier.toString(), true);
    }

    private static Activity activityFor(ElementTag input) {
        Identifier identifier = Identifier.tryParse(CoreUtilities.toLowerCase(input.asString()));
        Activity activity = identifier == null ? null : BuiltInRegistries.ACTIVITY.get(identifier).map(Holder::value).orElse(null);
        if (activity == null) {
            Debug.echoError("Invalid villager activity '" + input + "' specified for a biome environment attribute.");
        }
        return activity;
    }

    private static AmbientSounds ambientSoundsFor(ObjectTag value) {
        MapTag map = MapTag.getMapFor(value, CoreUtilities.noDebugContext);
        if (map == null) {
            Holder<SoundEvent> loop = soundFor(value);
            return loop == null ? null : new AmbientSounds(Optional.of(loop), Optional.empty(), List.of());
        }
        Optional<Holder<SoundEvent>> loop = Optional.empty();
        ObjectTag loopInput = map.getObject("loop");
        if (loopInput != null) {
            Holder<SoundEvent> holder = soundFor(loopInput);
            if (holder == null) {
                return null;
            }
            loop = Optional.of(holder);
        }
        Optional<AmbientMoodSettings> mood = Optional.empty();
        ObjectTag moodInput = map.getObject("mood");
        if (moodInput != null) {
            MapTag moodMap = MapTag.getMapFor(moodInput, CoreUtilities.noDebugContext);
            Holder<SoundEvent> holder = soundFor(moodMap == null ? moodInput : moodMap.getObject("sound"));
            if (holder == null) {
                return null;
            }
            mood = Optional.of(moodMap == null ? new AmbientMoodSettings(holder, 6000, 8, 2.0)
                    : new AmbientMoodSettings(holder, intFor(moodMap, "tick_delay", 6000), intFor(moodMap, "block_search_extent", 8), doubleFor(moodMap, "offset", 2.0)));
        }
        List<AmbientAdditionsSettings> additions = new ArrayList<>();
        ObjectTag additionsInput = map.getObject("additions");
        if (additionsInput != null && !additionsFor(additionsInput, additions)) {
            return null;
        }
        return new AmbientSounds(loop, mood, additions);
    }

    private static MapTag ambientSoundsToTag(AmbientSounds sounds) {
        MapTag result = new MapTag();
        sounds.loop().ifPresent(loop -> result.putObject("loop", soundToTag(loop)));
        sounds.mood().ifPresent(mood -> {
            MapTag moodMap = new MapTag();
            moodMap.putObject("sound", soundToTag(mood.soundEvent()));
            moodMap.putObject("tick_delay", new ElementTag(mood.tickDelay()));
            moodMap.putObject("block_search_extent", new ElementTag(mood.blockSearchExtent()));
            moodMap.putObject("offset", new ElementTag(mood.soundPositionOffset()));
            result.putObject("mood", moodMap);
        });
        if (!sounds.additions().isEmpty()) {
            MapTag additions = new MapTag();
            for (AmbientAdditionsSettings addition : sounds.additions()) {
                additions.putObject(soundToTag(addition.soundEvent()).asString(), new ElementTag(addition.tickChance()));
            }
            result.putObject("additions", additions);
        }
        return result;
    }

    private static boolean additionsFor(ObjectTag input, List<AmbientAdditionsSettings> into) {
        MapTag map = MapTag.getMapFor(input, CoreUtilities.noDebugContext);
        if (map != null && map.getObject("sound") == null) {
            for (var entry : map.entrySet()) {
                Holder<SoundEvent> holder = soundFor(new ElementTag(entry.getKey().str, true));
                if (holder == null) {
                    return false;
                }
                into.add(new AmbientAdditionsSettings(holder, entry.getValue().asElement().asDouble()));
            }
            return true;
        }
        for (ObjectTag entry : ListTag.getListFor(input, CoreUtilities.noDebugContext).objectForms) {
            MapTag entryMap = MapTag.getMapFor(entry, CoreUtilities.noDebugContext);
            Holder<SoundEvent> holder = soundFor(entryMap == null ? entry : entryMap.getObject("sound"));
            if (holder == null) {
                return false;
            }
            into.add(new AmbientAdditionsSettings(holder, entryMap == null ? 0.0111 : doubleFor(entryMap, "chance", 0.0111)));
        }
        return true;
    }

    private static Music musicFor(ObjectTag input) {
        MapTag map = MapTag.getMapFor(input, CoreUtilities.noDebugContext);
        if (map == null) {
            Holder<SoundEvent> sound = soundFor(input);
            return sound == null ? null : new Music(sound, 12000, 24000, false);
        }
        Holder<SoundEvent> sound = soundFor(map.getObject("sound"));
        if (sound == null) {
            return null;
        }
        return new Music(sound, intFor(map, "min_delay", 12000), intFor(map, "max_delay", 24000), boolFor(map, "replace_current", false));
    }

    private static MapTag musicToTag(Music music) {
        MapTag result = new MapTag();
        result.putObject("sound", soundToTag(music.sound()));
        result.putObject("min_delay", new ElementTag(music.minDelay()));
        result.putObject("max_delay", new ElementTag(music.maxDelay()));
        result.putObject("replace_current", new ElementTag(music.replaceCurrentMusic()));
        return result;
    }

    private static Optional<Music> musicSlotFor(MapTag map, String key) {
        ObjectTag input = map.getObject(key);
        if (input == null) {
            return Optional.empty();
        }
        Music music = musicFor(input);
        return music == null ? null : Optional.of(music);
    }

    private static BackgroundMusic backgroundMusicFor(ObjectTag value) {
        MapTag map = MapTag.getMapFor(value, CoreUtilities.noDebugContext);
        if (map == null || map.getObject("sound") != null) {
            Music music = musicFor(value);
            return music == null ? null : new BackgroundMusic(music);
        }
        Optional<Music> defaultMusic = musicSlotFor(map, "default");
        Optional<Music> creativeMusic = musicSlotFor(map, "creative");
        Optional<Music> underwaterMusic = musicSlotFor(map, "underwater");
        if (defaultMusic == null || creativeMusic == null || underwaterMusic == null) {
            return null;
        }
        return new BackgroundMusic(defaultMusic, creativeMusic, underwaterMusic);
    }

    private static MapTag backgroundMusicToTag(BackgroundMusic music) {
        MapTag result = new MapTag();
        music.defaultMusic().ifPresent(entry -> result.putObject("default", musicToTag(entry)));
        music.creativeMusic().ifPresent(entry -> result.putObject("creative", musicToTag(entry)));
        music.underwaterMusic().ifPresent(entry -> result.putObject("underwater", musicToTag(entry)));
        return result;
    }

    private static BedRule.Rule bedRuleFor(MapTag map, String key) {
        ObjectTag input = map.getObject(key);
        if (input == null) {
            return BedRule.Rule.ALWAYS;
        }
        BedRule.Rule rule = input.asElement().asEnum(BedRule.Rule.class);
        if (rule == null) {
            Debug.echoError("Invalid bed rule '" + input + "', must be ALWAYS, WHEN_DARK, or NEVER.");
        }
        return rule;
    }

    private static Optional<Component> bedRuleMessageFor(MapTag map) {
        ObjectTag message = map.getObject("error_message");
        return message == null ? Optional.empty() : Optional.ofNullable(Handler.parseNMSComponent(message.toString(), PaperAPITools.BaseColor.WHITE));
    }

    private static ParticleOptions particleFor(ObjectTag input) {
        if (input == null) {
            Debug.echoError("No particle specified for a biome environment attribute.");
            return null;
        }
        Particle particle = Utilities.elementToEnumlike(input.asElement(), Particle.class, false);
        if (particle == null) {
            Debug.echoError("Invalid particle '" + input + "' specified for a biome environment attribute.");
            return null;
        }
        try {
            return CraftParticle.createParticleParam(particle, null);
        }
        catch (Throwable ex) {
            Debug.echoError("Particle '" + input + "' needs extra data, which biome environment attributes cannot carry.");
            return null;
        }
    }

    private static ElementTag particleToTag(ParticleOptions particle) {
        Particle converted = CraftParticle.minecraftToBukkit(particle.getType());
        return converted == null ? null : Utilities.enumLikeToLegacyElement(converted);
    }

    private static List<AmbientParticle> ambientParticlesFor(ObjectTag value) {
        List<AmbientParticle> result = new ArrayList<>();
        MapTag byName = MapTag.getMapFor(value, CoreUtilities.noDebugContext);
        if (byName != null && byName.getObject("particle") == null) {
            for (var entry : byName.entrySet()) {
                ParticleOptions particle = particleFor(new ElementTag(entry.getKey().str, true));
                if (particle == null) {
                    return null;
                }
                result.add(new AmbientParticle(particle, entry.getValue().asElement().asFloat()));
            }
            return result;
        }
        for (ObjectTag entry : ListTag.getListFor(value, CoreUtilities.noDebugContext).objectForms) {
            MapTag map = MapTag.getMapFor(entry, CoreUtilities.noDebugContext);
            ParticleOptions particle = particleFor(map == null ? entry : map.getObject("particle"));
            if (particle == null) {
                return null;
            }
            result.add(new AmbientParticle(particle, map == null ? 0.05f : floatFor(map, "probability", 0.05f)));
        }
        return result;
    }

    private static MapTag ambientParticlesToTag(List<?> particles) {
        MapTag result = new MapTag();
        for (Object entry : particles) {
            if (!(entry instanceof AmbientParticle particle)) {
                return null;
            }
            ElementTag name = particleToTag(particle.particle());
            if (name != null) {
                result.putObject(name.asString(), new ElementTag(particle.probability()));
            }
        }
        return result;
    }

    private static BedRule bedRuleFor(ObjectTag value) {
        MapTag map = MapTag.getMapFor(value, CoreUtilities.noDebugContext);
        if (map == null) {
            Debug.echoError("Bed rules must be given as a MapTag.");
            return null;
        }
        BedRule.Rule canSleep = bedRuleFor(map, "can_sleep");
        BedRule.Rule canSetSpawn = bedRuleFor(map, "can_set_spawn");
        if (canSleep == null || canSetSpawn == null) {
            return null;
        }
        return new BedRule(canSleep, canSetSpawn, boolFor(map, "explodes", false), bedRuleMessageFor(map));
    }

    private static MapTag bedRuleToTag(BedRule rule) {
        MapTag result = new MapTag();
        result.putObject("can_sleep", new ElementTag(rule.canSleep()));
        result.putObject("can_set_spawn", new ElementTag(rule.canSetSpawn()));
        result.putObject("explodes", new ElementTag(rule.explodes()));
        rule.errorMessage().ifPresent(message -> result.putObject("error_message", new ElementTag(Handler.stringifyNMSComponent(message), true)));
        return result;
    }

    @Override
    public int getFogColor() {
        return getEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR);
    }

    @Override
    public void setFogColor(int color) {
        setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR, color);
    }

    @Override
    public int getWaterFogColor() {
        return getEnvironmentAttribute(EnvironmentAttributes.WATER_FOG_COLOR);
    }

    @Override
    public void setWaterFogColor(int color) {
        setEnvironmentAttribute(EnvironmentAttributes.WATER_FOG_COLOR, color);
    }

    public <T> T getEnvironmentAttribute(EnvironmentAttribute<T> attribute) {
        return biomeHolder.value().getAttributes().applyModifier(attribute, attribute.defaultValue());
    }

    public <T> void setEnvironmentAttribute(EnvironmentAttribute<T> attribute, T value) {
        Biome nmsBiome = biomeHolder.value();
        EnvironmentAttributeMap newAttributeMap = EnvironmentAttributeMap.builder().putAll(nmsBiome.getAttributes()).set(attribute, value).build();
        try {
            BIOME_ATTRIBUTES_SETTER.invokeExact(nmsBiome, newAttributeMap);
        }
        catch (Throwable e) {
            Debug.echoError(e);
        }
        setNetworkedRegistrationInfo();
        if (LAYERED_ATTRIBUTES.add(attribute)) {
            rebuildEnvironmentAttributes();
        }
    }

    private static void rebuildEnvironmentAttributes() {
        if (LEVEL_ATTRIBUTES_SETTER == null) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            ServerLevel level = ((CraftWorld) world).getHandle();
            try {
                LEVEL_ATTRIBUTES_SETTER.invoke(level, EnvironmentAttributeSystem.builder().addDefaultLayers(level).build());
            }
            catch (Throwable e) {
                Debug.echoError(e);
            }
        }
    }

    private List<EntityType> getSpawnableEntities(MobCategory creatureType) {
        MobSpawnSettings mobs = biomeHolder.value().getMobSettings();
        WeightedList<MobSpawnSettings.SpawnerData> typeSettingList = mobs.getMobs(creatureType);
        List<EntityType> entityTypes = new ArrayList<>();
        if (typeSettingList == null) {
            return entityTypes;
        }
        for (Weighted<MobSpawnSettings.SpawnerData> meta : typeSettingList.unwrap()) {
            entityTypes.add(CraftEntityType.minecraftToBukkit(meta.value().type()));
        }
        return entityTypes;
    }

    public static void setNoiseBiome(ChunkAccess chunk, int x, int y, int z, Holder<Biome> biome) {
        try {
            CHUNK_SET_NOISE_BIOME.invoke(chunk, x, y, z, biome);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setTo(Block block) {
        if (((CraftWorld) block.getWorld()).getHandle() != this.world) {
            NMSHandler.instance.getBiomeNMS(block.getWorld(), getKey()).setTo(block);
            return;
        }
        // Based on CraftWorld source
        BlockPos pos = new BlockPos(block.getX(), 0, block.getZ());
        if (world.hasChunkAt(pos)) {
            LevelChunk chunk = world.getChunkAt(pos);
            if (chunk != null) {
                setNoiseBiome(chunk, block.getX() >> 2, block.getY() >> 2, block.getZ() >> 2, biomeHolder);
                chunk.markUnsaved();
            }
        }
    }

    public Biome.TemperatureModifier getTemperatureModifier() {
        return biomeHolder.value().climateSettings.temperatureModifier();
    }

    private void setNetworkedRegistrationInfo() {
        try {
            Map<ResourceKey<Biome>, RegistrationInfo> registrationInfos = (Map<ResourceKey<Biome>, RegistrationInfo>) MAPPED_REGISTRY_REGISTRATION_INFOS.invokeExact(getBiomeRegistry());
            registrationInfos.put(biomeHolder.key(), RegistrationInfo.BUILT_IN);
        }
        catch (Throwable e) {
            Debug.echoError("Failed to set biome registration info, changes may not be synced correctly.");
            Debug.echoError(e);
        }
    }
}

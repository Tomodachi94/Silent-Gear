package net.silentchaos512.gear.api.parts;

import com.google.common.collect.Multimap;
import com.google.common.primitives.UnsignedInts;
import com.google.gson.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.event.GetStatModifierEvent;
import net.silentchaos512.gear.api.stats.CommonItemStats;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.stats.StatInstance.Operation;
import net.silentchaos512.gear.api.stats.StatModifierMap;
import net.silentchaos512.gear.config.Config;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.lib.util.Color;
import net.silentchaos512.lib.util.StackHelper;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// TODO: javadoc
@Getter(AccessLevel.PUBLIC)
public abstract class ItemPart extends IForgeRegistryEntry.Impl<ItemPart> {
    @Deprecated
    public static final String NBT_KEY = "Key";
    public static final String NBT_ID = "ID";

    protected static final ResourceLocation BLANK_TEXTURE = new ResourceLocation(SilentGear.MOD_ID, "items/blank");

    private static final Gson GSON = (new GsonBuilder()).create();
    private static final Pattern REGEX_TEXTURE_SUFFIX_REPLACE = Pattern.compile("[a-z]+_");

    @Getter(AccessLevel.NONE)
    protected Supplier<ItemStack> craftingStack = () -> ItemStack.EMPTY;
    protected String craftingOreDictName = "";
    @Getter(AccessLevel.NONE)
    protected Supplier<ItemStack> craftingStackSmall = () -> ItemStack.EMPTY;
    protected String craftingOreDictNameSmall = "";
    protected int tier = 0;
    protected boolean enabled = true;
    protected boolean hidden = false;
    protected String textureSuffix = "";
    protected int textureColor = Color.VALUE_WHITE;
    protected int brokenColor = Color.VALUE_WHITE;
    protected TextFormatting nameColor = TextFormatting.GRAY;
    protected String localizedNameOverride = "";
    private final PartOrigins origin;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private int id;

    @Getter(AccessLevel.NONE)
    protected Multimap<ItemStat, StatInstance> stats = new StatModifierMap();

    public ItemPart(PartOrigins origin) {
        this.origin = origin;
    }

    // ===========================
    // = Stats and Miscellaneous =
    // ===========================

    public abstract PartType getType();

    public ItemStack getCraftingStack() {
        return craftingStack.get();
    }

    public ItemStack getCraftingStackSmall() {
        return craftingStackSmall.get();
    }

    public Collection<StatInstance> getStatModifiers(ItemStat stat, ItemPartData part) {
        List<StatInstance> mods = new ArrayList<>(this.stats.get(stat));
        GetStatModifierEvent event = new GetStatModifierEvent(part, stat, mods);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getModifiers();
    }

    /**
     * Default operation to use if the resource file does not specify on operation for the given
     * stat
     */
    public Operation getDefaultStatOperation(ItemStat stat) {
        return stat == CommonItemStats.HARVEST_LEVEL ? Operation.MAX : Operation.ADD;
    }

    @Deprecated
    public int getRepairAmount(ItemStack gear, ItemPartData part) {
        return (int) this.getRepairAmount(gear, part, RepairContext.QUICK);
    }

    public float getRepairAmount(ItemStack gear, ItemPartData part, RepairContext context) {
        // Base value on material durability
        ItemPartData gearPrimary = GearData.getPrimaryPart(gear);
        if (gearPrimary != null && part.part.tier < gearPrimary.part.tier) return 0;
        Collection<StatInstance> mods = getStatModifiers(CommonItemStats.DURABILITY, part);
        float durability = CommonItemStats.DURABILITY.compute(0f, mods);

        switch (context) {
            case QUICK:
                return Config.quickRepairFactor * durability;
            case ANVIL:
                return Config.anvilRepairFactor * durability;
            default:
                throw new IllegalArgumentException("Unknown RepairContext: " + context);
        }
    }

    // ============
    // = Crafting =
    // ============

    public boolean matchesForCrafting(ItemStack partRep, boolean matchOreDict) {
        if (partRep.isEmpty())
            return false;
        if (partRep.isItemEqual(this.craftingStack.get()))
            return true;
        if (matchOreDict)
            return StackHelper.matchesOreDict(partRep, this.craftingOreDictName);
        return false;
    }

    /**
     * Gets the position the part occupies. Mainly used to prevent duplicate upgrades of one type
     * (tips, grips). Consider returning {@link PartPositions#ANY} if position is not relevant.
     *
     * @return The part position (never null)
     */
    public abstract IPartPosition getPartPosition();

    public boolean isBlacklisted() {
        return isBlacklisted(this.craftingStack.get());
    }

    public boolean isBlacklisted(ItemStack partRep) {
        return !this.enabled;
    }

    // ===================================
    // = Display (textures and tooltips) =
    // ===================================

    /**
     * Gets a texture to use based on the item class
     *
     * @param part           The part
     * @param gear           The equipment item (tool/weapon/armor)
     * @param gearClass      The gear class string (pickaxe/sword/etc.)
     * @param animationFrame Animation frame, usually 0
     */
    @Nullable
    public abstract ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame);

    @Nullable
    public abstract ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame);

    @Nullable
    public ResourceLocation getTexture(ItemPartData part, ItemStack equipment, String gearClass, IPartPosition position) {
        return getTexture(part, equipment, gearClass, position, 0);
    }

    /**
     * Gets a texture to use for a broken item based on the item class
     *
     * @param part      The part
     * @param gear      The equipment item (tool/weapon/armor)
     * @param gearClass The gear class string (pickaxe/sword/etc.)
     */
    @Nullable
    public ResourceLocation getBrokenTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position) {
        return getTexture(part, gear, gearClass, position, 0);
    }

    /**
     * Used for model caching. Be sure to include the animation frame if it matters!
     */
    public String getModelIndex(ItemPartData part, int animationFrame) {
        return this.id + (animationFrame == 3 ? "_3" : "");
    }

    @SuppressWarnings("unused") // unused parameters
    public int getColor(ItemPartData part, ItemStack gear, int animationFrame) {
        if (!gear.isEmpty() && GearHelper.isBroken(gear))
            return this.brokenColor;
        return this.textureColor;
    }

    /**
     * Adds information to the tooltip of an equipment item
     *
     * @param part    The part
     * @param gear    The equipment (tool/weapon/armor) stack
     * @param tooltip Current tooltip lines
     */
    public abstract void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced);

    /**
     * Gets a translation key for the part
     */
    public String getTranslationKey(@Nullable ItemPartData part) {
        ResourceLocation name = Objects.requireNonNull(this.getRegistryName());
        return String.format("material.%s.%s.name", name.getNamespace(), name.getPath());
    }

    /**
     * Gets a translated name for the part, suitable for display
     *
     * @param part The part
     * @param gear The equipment (tool/weapon/armor) stack
     */
    public String getTranslatedName(ItemPartData part, ItemStack gear) {
        if (!localizedNameOverride.isEmpty())
            return localizedNameOverride;
        return /* nameColor + */ SilentGear.i18n.translate(this.getTranslationKey(part));
    }

    /**
     * Gets a string that represents the type of part (main, rod, tip, etc.) Used for localization
     * of part type/class, not the individual part.
     */
    @Deprecated
    public abstract String getTypeName();

    public String getDebugSymbol() {
        return this.getType().getDebugSymbol();
    }

    @Override
    public String toString() {
        String str = this.getClass().getName() + "[" + this.getDebugSymbol() + this.id + "]{";
        str += this.origin + ", ";
        str += "Key: " + this.getRegistryName() + ", ";
        str += "CraftingStack: " + this.craftingStack.get() + ", ";
        str += "CraftingOreDictName: '" + this.craftingOreDictName + "', ";
        str += "Tier: " + this.tier;
        str += "}";
        return str;
    }

    // ====================================
    // = Resource file and NBT management =
    // ====================================

    /**
     * Get the location of the resource file that contains material information
     */
    private String getResourceFileLocation() {
        ResourceLocation name = Objects.requireNonNull(this.getRegistryName());
        return String.format("assets/%s/materials/%s.json", name.getNamespace(), name.getPath());
    }

    void loadJsonResources() {
        // Main resource file in JAR
        String path = getResourceFileLocation();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path);
        if (resourceAsStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"))) {
                readResourceFile(reader);
                if (SilentGear.log.getLogger().isDebugEnabled())
                    SilentGear.log.info("Successfully read {}", path);
            } catch (Exception e) {
                SilentGear.log.catching(e);
            }
        } else if (this.origin.isBuiltin()) {
            SilentGear.log.error("ItemPart {} is missing its data file!", this);
        }

        // Override in config folder
        ResourceLocation registryName = Objects.requireNonNull(this.getRegistryName());
        File file = new File(Config.INSTANCE.getDirectory().getPath(), "materials/" + registryName.getPath() + ".json");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            readResourceFile(reader);
            if (SilentGear.log.getLogger().isDebugEnabled())
                SilentGear.log.debug("Successfully read {}", file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (Exception e) {
            SilentGear.log.catching(e);
        }

        if (this.textureSuffix.isEmpty())
            this.textureSuffix = REGEX_TEXTURE_SUFFIX_REPLACE.matcher(registryName.getPath()).replaceFirst("");
    }

    /**
     * Loads material information from a JSON file. Does not handle file IO exceptions.
     */
    private void readResourceFile(BufferedReader reader) {
        JsonElement je = GSON.fromJson(reader, JsonElement.class);
        JsonObject json = je.getAsJsonObject();
        Loader.processJson(this, json);
    }

    public void writeToNBT(NBTTagCompound tags) {
        tags.setInteger(NBT_ID, this.id);
        tags.setString(NBT_KEY, Objects.requireNonNull(this.getRegistryName()).toString());
    }

    @Nullable
    public static ItemPart fromNBT(NBTTagCompound tags) {
        if (tags.hasKey(NBT_ID)) return PartRegistry.byId(tags.getInteger(NBT_ID));
        else if (tags.hasKey(NBT_KEY)) return PartRegistry.get(tags.getString(NBT_KEY));
        return null;
    }

    public void postInitChecks() {
        ResourceLocation name = Objects.requireNonNull(this.getRegistryName());

        if (this.origin == PartOrigins.BUILTIN_CORE && !SilentGear.MOD_ID.equals(name.getNamespace()))
            throw new RuntimeException(String.format("Part \"%s\" has origin %s, but should be %s",
                    name, PartOrigins.BUILTIN_CORE, PartOrigins.BUILTIN_ADDON));

        if (getCraftingStack().isEmpty())
            SilentGear.log.warn("Part \"{}\" ({}) has no crafting item.", name, this.origin);
    }

    /**
     * Used as a stand-in for parts that have been removed since world creation.
     */
    public static final class Dummy extends ItemPart {
        private Dummy() {
            super(PartOrigins.BUILTIN_CORE);
        }

        private Dummy(PartOrigins origin) {
            super(origin);
        }

        @Override
        public PartType getType() {
            return PartType.DUMMY;
        }

        static Dummy dummyFactory(ResourceLocation key) {
            Dummy dummy = new Dummy();
            dummy.setRegistryName(key);
            return dummy;
        }

        static Dummy missingFactory(ResourceLocation key, boolean isNetwork) {
            return dummyFactory(key);
        }

        static Dummy typeFactory(PartOrigins origin) {
            return new Dummy(origin);
        }

        @Override
        public IPartPosition getPartPosition() {
            return PartPositions.ANY;
        }

        @Nullable
        @Override
        public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame) {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame) {
            return null;
        }

        @Override
        public void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
        }

        @Override
        public String getTypeName() {
            return "dummy";
        }

        @Override
        public String getDebugSymbol() {
            return "d";
        }

        @Override
        void loadJsonResources() {
            // NO-OP
        }
    }

    /**
     * Handles most aspects of loading part properties from their JSON file
     */
    private static class Loader {
        private static void processJson(ItemPart part, JsonObject json) {
            readStats(part, json);
            readCraftingItems(part, json);
            readDisplayProperties(part, json);
            readAvailability(part, json);
        }

        private static void readStats(ItemPart part, JsonObject json) {
            JsonElement elementStats = json.get("stats");
            if (elementStats.isJsonArray()) {
                JsonArray array = elementStats.getAsJsonArray();
                Multimap<ItemStat, StatInstance> statMap = new StatModifierMap();
                int index = 0;
                for (JsonElement element : array) {
                    ++index;
                    JsonObject obj = element.getAsJsonObject();
                    String name = JsonUtils.getString(obj, "name", "");
                    ItemStat stat = ItemStat.ALL_STATS.get(name);

                    if (stat != null) {
                        float value = JsonUtils.getFloat(obj, "value", 0f);
                        Operation op = obj.has("op") ? Operation.byName(JsonUtils.getString(obj, "op"))
                                : part.getDefaultStatOperation(stat);
                        String id = String.format("mat_%s_%s%d", part.getRegistryName(), stat.getName(), index);
                        statMap.put(stat, new StatInstance(id, value, op));
                    }
                }

                // Move the newly loaded modifiers into the stat map, replacing existing ones
                for (ItemStat stat : statMap.keySet()) {
                    Collection<StatInstance> newList = statMap.get(stat);
                    if (!newList.isEmpty()) {
                        part.stats.removeAll(stat);
                        part.stats.putAll(stat, newList);
                    }
                }
            }
        }

        private static void readCraftingItems(ItemPart part, JsonObject json) {
            JsonElement elementCraftingItems = json.get("crafting_items");
            if (elementCraftingItems.isJsonObject()) {
                JsonObject objTop = elementCraftingItems.getAsJsonObject();
                // Normal item (ingot, gem)
                if (objTop.has("normal") && objTop.get("normal").isJsonObject()) {
                    JsonObject obj = objTop.get("normal").getAsJsonObject();
                    part.craftingStack = readItemData(obj);
                    part.craftingOreDictName = JsonUtils.getString(obj, "oredict", part.craftingOreDictName);
                }
                // Small item (nugget, shard)
                if (objTop.has("small") && objTop.get("small").isJsonObject()) {
                    JsonObject obj = objTop.get("small").getAsJsonObject();
                    part.craftingStackSmall = readItemData(obj);
                    part.craftingOreDictNameSmall = JsonUtils.getString(obj, "oredict", part.craftingOreDictNameSmall);
                }
            }
        }

        private static void readDisplayProperties(ItemPart part, JsonObject json) {
            JsonElement elementDisplay = json.get("display");
            if (elementDisplay.isJsonObject()) {
                JsonObject obj = elementDisplay.getAsJsonObject();
                part.hidden = JsonUtils.getBoolean(obj, "hidden", part.hidden);
                part.textureSuffix = JsonUtils.getString(obj, "texture_suffix", part.textureSuffix);
                if (obj.has("texture_color"))
                    part.textureColor = readColorCode(JsonUtils.getString(obj, "texture_color"));
                if (obj.has("broken_color"))
                    part.brokenColor = readColorCode(JsonUtils.getString(obj, "broken_color"));
                if (obj.has("name_color"))
                    part.nameColor = TextFormatting.getValueByName(JsonUtils.getString(obj, "name_color"));
                part.localizedNameOverride = JsonUtils.getString(obj, "override_localization", part.localizedNameOverride);
            }
        }

        private static void readAvailability(ItemPart part, JsonObject json) {
            JsonElement elementAvailability = json.get("availability");
            if (elementAvailability.isJsonObject()) {
                JsonObject obj = elementAvailability.getAsJsonObject();
                part.enabled = JsonUtils.getBoolean(obj, "enabled", part.enabled);
                part.tier = JsonUtils.getInt(obj, "tier", part.tier);
                // TODO: blacklist
            }
        }

        private static int readColorCode(String str) {
            try {
                return UnsignedInts.parseUnsignedInt(str, 16);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                return Color.VALUE_WHITE;
            }
        }

        /**
         * Parse ItemStack data from a JSON object
         */
        private static Supplier<ItemStack> readItemData(JsonObject json) {
            if (!json.has("item"))
                return () -> ItemStack.EMPTY;

            final String itemName = JsonUtils.getString(json, "item");
            final int meta = JsonUtils.getInt(json, "data", 0);
            // Item likely does not exist when the ItemPart is constructed, so we need to get it lazily
            return () -> {
                Item item = Item.getByNameOrId(itemName);
                return item == null ? ItemStack.EMPTY : new ItemStack(item, 1, meta);
            };
        }
    }
}

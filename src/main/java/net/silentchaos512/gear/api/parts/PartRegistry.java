package net.silentchaos512.gear.api.parts;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.silentchaos512.gear.SilentGear;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Used to register gear parts, and match parts to item stacks.
 *
 * @author SilentChaos512
 */
@Mod.EventBusSubscriber
public final class PartRegistry {
    static IForgeRegistry<ItemPart> registry;

    private static final ResourceLocation REGISTRY_NAME = new ResourceLocation(SilentGear.MOD_ID, "parts");

    private static final Map<Integer, ItemPart> PARTS_BY_ID = new HashMap<>();

    @Deprecated
    private static Map<String, ItemPart> map = new LinkedHashMap<>();
    private static List<PartMain> mains = null;
    private static List<PartRod> rods = null;
    private static List<PartMain> visibleMains = null;
    private static List<PartRod> visibleRods = null;
    private static Map<String, ItemPart> STACK_TO_PART = new HashMap<>();
    @Getter
    private static int highestMainPartTier = 0;

    private PartRegistry() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Gets the part with the given key, if it exists.
     *
     * @param key The part name/key
     * @return The {@link ItemPart} with the given key, or null if there is no match
     */
    @Deprecated
    @Nullable
    public static ItemPart get(String key) {
        return map.get(key);
    }

    /**
     * Gets the part with the given key, if it exists.
     *
     * @param key The part name/key
     * @return The {@link ItemPart} with the given key, or null if there is no match
     */
    @Deprecated
    @Nullable
    public static ItemPart get(ResourceLocation key) {
        return map.get(key.toString());
    }

    /**
     * Gets an {@link ItemPart} matching the stack, if one exists.
     *
     * @param stack {@link ItemStack} that may or may not be an {@link ItemPart}
     * @return The matching {@link ItemPart}, or null if there is none
     */
    @Deprecated
    @Nullable
    public static ItemPart get(ItemStack stack) {
        if (stack.isEmpty())
            return null;

        String key = stack.getItem().getTranslationKey() + "@" + stack.getItemDamage();
        if (STACK_TO_PART.containsKey(key))
            return STACK_TO_PART.get(key);

        for (ItemPart part : map.values()) {
            if (part.matchesForCrafting(stack, true)) {
                STACK_TO_PART.put(key, part);
                return part;
            }
        }
        return null;
    }

    /**
     * Registers a gear part (material). A part with the same key must not be registered.
     *
     * @param part The {@link ItemPart}
     */
    @Deprecated
    public static <T extends ItemPart> T putPart(@Nonnull T part) {
        String key = part.getRegistryName().toString();
        if (map.containsKey(key)) {
            //throw new IllegalArgumentException("Already have a part with key " + key);
        } else {
            map.put(key, part);
        }

        if (part instanceof PartMain && part.getTier() > highestMainPartTier)
            highestMainPartTier = part.getTier();

        return part;
    }

    @Nullable
    public static ItemPart byId(int id) {
        return PARTS_BY_ID.get(id);
    }

    public static Set<String> getKeySet() {
        return map.keySet();
    }

    public static Collection<ItemPart> getValues() {
        return map.values();
    }

    /**
     * Gets a list of registered ToolPartMains in the order they are registered (used for sub-item
     * display).
     */
    public static List<PartMain> getMains() {
        if (mains == null) {
            mains = registry.getValuesCollection().stream()
                    .filter(p -> p instanceof PartMain)
                    .map(PartMain.class::cast).collect(ImmutableList.toImmutableList());
        }
        return mains;
    }

    /**
     * Gets a list of registered ToolPartRods in the order they are registered.
     */
    public static List<PartRod> getRods() {
        if (rods == null) {
            rods = registry.getValuesCollection().stream()
                    .filter(p -> p instanceof PartRod)
                    .map(PartRod.class::cast).collect(ImmutableList.toImmutableList());
        }
        return rods;
    }

    /**
     * Gets a list of all mains that are not blacklisted or hidden
     */
    public static List<PartMain> getVisibleMains() {
        if (visibleMains == null) {
            visibleMains = registry.getValuesCollection().stream()
                    .filter(p -> p instanceof PartMain && !p.isBlacklisted() && !p.isHidden())
                    .map(PartMain.class::cast).collect(ImmutableList.toImmutableList());
        }
        return visibleMains;
    }

    /**
     * Gets a list of all rods that are not blacklisted or hidden
     */
    public static List<PartRod> getVisibleRods() {
        if (visibleRods == null) {
            visibleRods = registry.getValuesCollection().stream()
                    .filter(p -> p instanceof PartRod && !p.isBlacklisted() && !p.isHidden())
                    .map(PartRod.class::cast).collect(ImmutableList.toImmutableList());
        }
        return visibleRods;
    }

    public static void resetVisiblePartCaches() {
        visibleMains = null;
        visibleRods = null;
    }

    public static void getDebugLines(List<String> list) {
        list.add("PartRegistry.map=" + map.size());
        list.add("PartRegistry.STACK_TO_PART=" + STACK_TO_PART.size());
    }

    @SubscribeEvent
    public static void createRegistry(RegistryEvent.NewRegistry event) {
        SilentGear.log.info("Creating ItemPart registry");

        RegistryBuilder<ItemPart> builder = new RegistryBuilder<>();
        builder.setType(ItemPart.class);
        builder.add((IForgeRegistry.AddCallback<ItemPart>) (owner, stage, id, obj, oldObj) -> {
            SilentGear.log.debug("PartRegistry AddCallback: {} {} {} {} {}", owner, stage, id, obj, oldObj);
            obj.setId(id);
            putPart(obj);
            PARTS_BY_ID.put(id, obj);
        });
        builder.add((IForgeRegistry.ClearCallback<ItemPart>) (owner, stage) -> {
            SilentGear.log.debug("PartRegistry ClearCallback: {} {}", owner, stage);
            map.clear();
            PARTS_BY_ID.clear();
        });
        builder.set(ItemPart.Dummy::dummyFactory);
        builder.set(ItemPart.Dummy::missingFactory);
        builder.allowModification();
        builder.setName(REGISTRY_NAME);

        registry = builder.create();
    }

    public static void loadJsonResources() {
        registry.forEach(ItemPart::loadJsonResources);
    }
}

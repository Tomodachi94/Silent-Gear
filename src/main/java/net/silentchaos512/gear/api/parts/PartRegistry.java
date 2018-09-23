package net.silentchaos512.gear.api.parts;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import net.silentchaos512.gear.SilentGear;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to register gear parts, and match parts to item stacks.
 *
 * @author SilentChaos512
 */
@Mod.EventBusSubscriber
public final class PartRegistry {
    private static IForgeRegistry<ItemPart> registry;
    private static final ResourceLocation REGISTRY_NAME = new ResourceLocation(SilentGear.MOD_ID, "parts");
    private static final Map<Integer, ItemPart> PARTS_BY_ID = new HashMap<>();

    private static ImmutableList<PartMain> mains = null;
    private static ImmutableList<PartRod> rods = null;
    private static ImmutableList<PartMain> visibleMains = null;
    private static ImmutableList<PartRod> visibleRods = null;
    private static final Map<String, ItemPart> STACK_TO_PART = new HashMap<>();
    @Getter private static int highestMainPartTier = 0;

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
        return get(new ResourceLocation(key));
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
        return registry.getValue(key);
    }

    /**
     * Gets an {@link ItemPart} matching the stack, if one exists.
     *
     * @param stack {@link ItemStack} that may or may not be an {@link ItemPart}
     * @return The matching {@link ItemPart}, or null if there is none
     */
    @Nullable
    public static ItemPart get(ItemStack stack) {
        if (stack.isEmpty())
            return null;

        String key = stack.getItem().getRegistryName() + "@" + stack.getItemDamage();
        if (STACK_TO_PART.containsKey(key))
            return STACK_TO_PART.get(key);

        for (ItemPart part : registry) {
            if (part.matchesForCrafting(stack, true)) {
                STACK_TO_PART.put(key, part);
                return part;
            }
        }
        return null;
    }

    @Nullable
    static ItemPart byId(int id) {
        return PARTS_BY_ID.get(id);
    }

    public static Collection<ItemPart> getValues() {
        return registry.getValuesCollection();
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

    public static void getDebugLines(Collection<String> list) {
        list.add("PartRegistry.registry=" + registry.getValuesCollection().size());
        list.add("PartRegistry.STACK_TO_PART=" + STACK_TO_PART.size());
    }

    @SubscribeEvent
    public static void createRegistry(RegistryEvent.NewRegistry event) {
        SilentGear.log.info("Creating ItemPart registry");

        RegistryBuilder<ItemPart> builder = new RegistryBuilder<>();
        builder.setType(ItemPart.class);
        builder.add(PartRegistry::onAddCallback);
        builder.add((IForgeRegistry.ClearCallback<ItemPart>) PartRegistry::onClearCallback);
        builder.set(ItemPart.Dummy::dummyFactory);
        builder.set(ItemPart.Dummy::missingFactory);
        builder.allowModification();
        builder.setName(REGISTRY_NAME);

        registry = builder.create();
    }

    public static void loadJsonResources() {
        registry.forEach(ItemPart::loadJsonResources);
    }

    private static void onAddCallback(IForgeRegistryInternal<ItemPart> owner, RegistryManager stage, int id, ItemPart obj, ItemPart oldObj) {
//        SilentGear.log.debug("PartRegistry AddCallback: {} {} {} {} {}", owner, stage, id, obj, oldObj);
        obj.setId(id);
        PARTS_BY_ID.put(id, obj);

        if (obj instanceof PartMain && obj.getTier() > highestMainPartTier)
            highestMainPartTier = obj.getTier();
    }

    private static void onClearCallback(IForgeRegistryInternal<ItemPart> owner, RegistryManager stage) {
//        SilentGear.log.debug("PartRegistry ClearCallback: {} {}", owner, stage);
        PARTS_BY_ID.clear();

        highestMainPartTier = -1;
    }
}

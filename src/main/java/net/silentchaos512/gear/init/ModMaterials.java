package net.silentchaos512.gear.init;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.parts.*;
import net.silentchaos512.gear.config.Config;
import net.silentchaos512.gear.item.MiscUpgrades;
import net.silentchaos512.gear.item.TipUpgrades;
import net.silentchaos512.gear.item.ToolRods;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.lib.registry.IPhasedInitializer;
import net.silentchaos512.lib.registry.SRegistry;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
public class ModMaterials implements IPhasedInitializer {
    public static final ModMaterials INSTANCE = new ModMaterials();

    // Not bothering to store references to most parts. These are the only ones I need.
    @GameRegistry.ObjectHolder(SilentGear.RESOURCE_PREFIX + "main_wood")
    public static final PartMain mainWood = null;
    @GameRegistry.ObjectHolder(SilentGear.RESOURCE_PREFIX + "main_iron")
    public static final PartMain mainIron = null;
    @GameRegistry.ObjectHolder(SilentGear.RESOURCE_PREFIX + "bowstring_string")
    public static final PartBowstring bowstringString = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerParts(RegistryEvent.Register<ItemPart> event) {
        IForgeRegistry<ItemPart> reg = event.getRegistry();

        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_wood");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_stone");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_flint");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_terracotta");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_netherrack");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_iron");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_gold");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_emerald");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_diamond");
        register(reg, new PartMain(PartOrigins.BUILTIN_CORE), "main_obsidian");
//        if (SilentGear.instance.isDevBuild()) register(reg, new PartMain(), "main_test");

        for (ToolRods rod : ToolRods.values())
            register(reg, rod.getPart(), rod.getPartName());

        for (TipUpgrades tip : TipUpgrades.values())
            register(reg, tip.getPart(), tip.getPartName());

        for (EnumDyeColor color : EnumDyeColor.values())
            register(reg, new PartGrip(PartOrigins.BUILTIN_CORE), "grip_wool_" + color.name().toLowerCase(Locale.ROOT));
        register(reg, new PartGrip(PartOrigins.BUILTIN_CORE), "grip_leather");

        register(reg, new PartBowstring(PartOrigins.BUILTIN_CORE), "bowstring_string");
        register(reg, new PartBowstring(PartOrigins.BUILTIN_CORE), "bowstring_sinew");

        for (MiscUpgrades upgrade : MiscUpgrades.values())
            register(reg, upgrade.getPart(), upgrade.getPartName());

        UserDefined.loadUserParts(reg);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void loadJsonResources(RegistryEvent.Register<ItemPart> event) {
        PartRegistry.loadJsonResources();
    }

    private static void register(IForgeRegistry<ItemPart> registry, ItemPart part, String name) {
        ResourceLocation registryName = new ResourceLocation(SilentGear.MOD_ID, name);
        part.setRegistryName(registryName);
        registry.register(part);
    }

    @Override
    public void init(SRegistry registry, FMLInitializationEvent event) {
        // Update part caches
        // All mods should have added their parts during pre-init
        PartRegistry.resetVisiblePartCaches();
        GearHelper.resetSubItemsCache();
    }

    private static final class UserDefined {
        static void loadUserParts(IForgeRegistry<ItemPart> reg) {
            final File directory = new File(Config.INSTANCE.getDirectory(), "materials");
            final File[] files = directory.listFiles();
            if (!directory.isDirectory() || files == null) {
                SilentGear.log.warn("File \"{}\" is not a directory?", directory);
                return;
            }

            final Pattern typeRegex = Pattern.compile("^[a-z]+");
            for (File file : files) {
                loadFromFile(reg, typeRegex, file);
            }
        }

        private static void loadFromFile(IForgeRegistry<ItemPart> reg, Pattern typeRegex, File file) {
            SilentGear.log.info("Material file found: {}", file);
            String filename = file.getName().replace(".json", "");
            ResourceLocation name = new ResourceLocation(SilentGear.MOD_ID, filename);

            // Add to registered parts if it doesn't exist
            if (!reg.containsKey(name)) {
                Matcher match = typeRegex.matcher(filename);
                if (match.find()) {
                    String type = match.group();
                    SilentGear.log.info("Trying to add part {}, type {}", name, type);

                    PartType partType = PartType.get(type);
                    if (partType != null)
                        register(reg, partType.construct(PartOrigins.USER_DEFINED), filename);
                    else
                        SilentGear.log.error("Unknown part type \"{}\" for {}", type, filename);
                }
            } else {
                SilentGear.log.info("Part already registered. Must be an override.");
            }
        }
    }
}

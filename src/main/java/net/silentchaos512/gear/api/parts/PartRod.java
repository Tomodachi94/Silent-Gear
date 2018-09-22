package net.silentchaos512.gear.api.parts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.silentchaos512.gear.api.stats.CommonItemStats;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;

import java.util.List;
import java.util.Objects;

public final class PartRod extends ItemPart {
    public PartRod() {
        super(false);
    }

    public PartRod(boolean userDefined) {
        super(userDefined);
    }

    @Override
    public IPartPosition getPartPosition() {
        return PartPositions.ROD;
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame) {
        return new ResourceLocation(Objects.requireNonNull(this.getRegistryName()).getNamespace(),
                "items/" + gearClass + "/rod_" + this.textureSuffix);
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame) {
        return getTexture(part, gear, gearClass, PartPositions.ROD, animationFrame);
    }

    @Override
    public void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
        // Nothing
    }

    @Override
    public String getTypeName() {
        return "rod";
    }

    @Override
    public StatInstance.Operation getDefaultStatOperation(ItemStat stat) {
        if (stat == CommonItemStats.HARVEST_LEVEL)
            return StatInstance.Operation.MAX;
        if (stat == CommonItemStats.ATTACK_SPEED || stat == CommonItemStats.RARITY)
            return StatInstance.Operation.ADD;
        return StatInstance.Operation.MUL2;
    }
}

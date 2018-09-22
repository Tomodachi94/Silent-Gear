package net.silentchaos512.gear.api.parts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.silentchaos512.gear.api.stats.CommonItemStats;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;

import java.util.List;
import java.util.Objects;

public final class PartTip extends ItemPart implements IUpgradePart {
    public PartTip(PartOrigins origin) {
        super(origin);
    }

    @Override
    public PartType getType() {
        return PartType.TIP;
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame) {
        String frameStr = "bow".equals(gearClass) && animationFrame == 3 ? "_3" : "";
        return new ResourceLocation(Objects.requireNonNull(this.getRegistryName()).getNamespace(),
                "items/" + gearClass + "/tip_" + this.textureSuffix + frameStr);
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame) {
        return getTexture(part, gear, gearClass, PartPositions.TIP, animationFrame);
    }

    @Override
    public ResourceLocation getBrokenTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position) {
        return ItemPart.BLANK_TEXTURE;
    }

    @Override
    public void addInformation(ItemPartData data, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
        tooltip.add(1, getNameColor() + getTranslatedName(data, gear));
    }

    @Override
    public String getTypeName() {
        return "tip";
    }

    @Override
    public boolean replacesExisting() {
        return true;
    }

    @Override
    public IPartPosition getPartPosition() {
        return PartPositions.TIP;
    }

    @Override
    public StatInstance.Operation getDefaultStatOperation(ItemStat stat) {
        return stat == CommonItemStats.HARVEST_LEVEL ? StatInstance.Operation.MAX : StatInstance.Operation.ADD;
    }
}

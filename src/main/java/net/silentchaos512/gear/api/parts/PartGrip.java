package net.silentchaos512.gear.api.parts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.api.item.ICoreTool;

import java.util.List;
import java.util.Objects;

public final class PartGrip extends ItemPart implements IUpgradePart {
    public PartGrip(PartOrigins origin) {
        super(origin);
    }

    @Override
    public PartType getType() {
        return PartType.GRIP;
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame) {
        return new ResourceLocation(Objects.requireNonNull(this.getRegistryName()).getNamespace(),
                "items/" + gearClass + "/grip_" + this.textureSuffix);
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame) {
        return getTexture(part, gear, gearClass, PartPositions.GRIP, animationFrame);
    }

    @Override
    public void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
        // Nothing
    }

    @Override
    public String getTypeName() {
        return "grip";
    }

    @Override
    public boolean isValidFor(ICoreItem gearItem) {
        return gearItem instanceof ICoreTool;
    }

    @Override
    public boolean replacesExisting() {
        return true;
    }

    @Override
    public IPartPosition getPartPosition() {
        return PartPositions.GRIP;
    }
}

package net.silentchaos512.gear.api.parts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public final class PartBowstring extends ItemPart {
    public PartBowstring() {
        super(false);
    }

    public PartBowstring(boolean userDefined) {
        super(userDefined);
    }

    @Override
    public IPartPosition getPartPosition() {
        return PartPositions.BOWSTRING;
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack stack, String toolClass, IPartPosition position, int animationFrame) {
        if (!"bow".equals(toolClass)) return BLANK_TEXTURE;
        return new ResourceLocation(Objects.requireNonNull(this.getRegistryName()).getNamespace(),
                "items/" + toolClass + "/bowstring_" + this.textureSuffix + "_" + animationFrame);
    }

    @Override
    public ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame) {
        return getTexture(part, gear, gearClass, PartPositions.BOWSTRING, animationFrame);
    }

    @Override
    public String getModelIndex(ItemPartData part, int animationFrame) {
        return this.getId() + "_" + animationFrame;
    }

    @Override
    public void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
        // Nothing
    }

    @Override
    public String getTypeName() {
        return "bowstring";
    }
}

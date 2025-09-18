package org.cmyk.durability_overhaul.mixin;

import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DigDurabilityEnchantment.class)
public abstract class DurabilityEnchantmentMixin {
    @Overwrite
    public static boolean shouldIgnoreDurabilityDrop(net.minecraft.world.item.ItemStack stack, int level, net.minecraft.util.RandomSource random) {
        // 计算耐久减免概率：每级提供10%的减免
        float chance = level * 0.1F;
        // 如果随机数小于减免概率，则忽略本次耐久消耗
        return random.nextFloat() < chance;
    }
}
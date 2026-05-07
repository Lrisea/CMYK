package org.cmyk.mixin;

import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DigDurabilityEnchantment.class)
public abstract class DurabilityEnchantmentMixin {
    /**
     * 重写耐久附魔是否忽略耐久消耗的逻辑
     * 每级耐久附魔提供-10%的耐久消耗
     *
     * @param stack 物品堆栈
     * @param level 附魔等级
     * @param random 随机数生成器
     * @return 如果应该忽略耐久消耗则返回true
     */
    @Overwrite
    public static boolean shouldIgnoreDurabilityDrop(net.minecraft.world.item.ItemStack stack, int level, net.minecraft.util.RandomSource random) {
        // 计算耐久减免概率：每级提供10%的减免
        float chance = level * 0.1F;
        // 如果随机数小于减免概率，则忽略本次耐久消耗
        return random.nextFloat() < chance;
    }
}
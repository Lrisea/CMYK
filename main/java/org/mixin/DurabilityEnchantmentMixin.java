package org.mixin;

import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DigDurabilityEnchantment.class)
public abstract class DurabilityEnchantmentMixin {
    
    /**
     * @author Hikamsg
     * @reason 修改耐久附魔的效果，每级提供10%的耐久消耗减免，而不是默认的概率性减免
     * 此修改配合方块破坏时额外消耗9点耐久的功能，使耐久1的工具在挖掘时实际消耗10点耐久
     */
    @Overwrite
    public static boolean shouldIgnoreDurabilityDrop(net.minecraft.world.item.ItemStack stack, int level, net.minecraft.util.RandomSource random) {
        // 计算耐久减免概率：每级提供10%的减免
        float chance = level * 0.1F;
        // 如果随机数小于减免概率，则忽略本次耐久消耗
        return random.nextFloat() < chance;
    }
}